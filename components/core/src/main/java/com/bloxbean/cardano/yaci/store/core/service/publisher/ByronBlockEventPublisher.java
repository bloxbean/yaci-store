package com.bloxbean.cardano.yaci.store.core.service.publisher;

import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import com.bloxbean.cardano.yaci.store.core.service.SyncSessionGuard;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchByronBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static com.bloxbean.cardano.yaci.store.common.util.ListUtil.partition;

@Component
@ReadOnly(false)
@Slf4j
public class ByronBlockEventPublisher implements BlockEventPublisher<ByronMainBlock> {
    private final ApplicationEventPublisher publisher;
    private final CursorService cursorService;
    private final ExecutorService blockExecutor;
    private final StoreProperties storeProperties;

    private List<BatchByronBlock> byronBatchBlockList = new ArrayList<>();
    //The sync session the blocks in byronBatchBlockList belong to. Guarded by byronBatchBlockList.
    private Long batchSession;

    public ByronBlockEventPublisher(@Qualifier("blockExecutor") ExecutorService blockExecutor,
                                    ApplicationEventPublisher publisher,
                                    CursorService cursorService,
                                    StoreProperties storeProperties) {
        this.blockExecutor = blockExecutor;
        this.publisher = publisher;
        this.cursorService = cursorService;
        this.storeProperties = storeProperties;
    }

    public void reset() {
        synchronized (byronBatchBlockList) {
            byronBatchBlockList.clear();
            batchSession = null;
        }
    }

    /**
     * Call with the byronBatchBlockList lock held. A thread of an old sync session must not add to, or take, the batch
     * of the session that replaced it, so the session is checked under the same lock. Blocks left by an older session
     * are dropped: the current session fetches them again from the cursor.
     */
    private void ownBatch() {
        sessionGuard.checkCaller();
        Long session = sessionGuard.callerSession();
        if (session != null && !session.equals(batchSession)) {
            if (!byronBatchBlockList.isEmpty())
                log.warn("Dropping {} batched Byron blocks of old sync session {}", byronBatchBlockList.size(), batchSession);
            byronBatchBlockList.clear();
            batchSession = session;
        }
    }

    //Shared with BlockFetchService. A default instance keeps hand-built instances (tests) working.
    private SyncSessionGuard sessionGuard = new SyncSessionGuard();

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setSessionGuard(SyncSessionGuard sessionGuard) {
        this.sessionGuard = sessionGuard;
    }

    @Transactional
    @Override
    public void publishBlockEvents(EventMetadata eventMetadata, ByronMainBlock byronBlock, List<Transaction> transactions) {
        ByronMainBlockEvent byronMainBlockEvent = new ByronMainBlockEvent(eventMetadata, byronBlock);

        publisher.publishEvent(byronMainBlockEvent);
        //A thread from an old sync session must not commit this block
        sessionGuard.checkCaller();
        publisher.publishEvent(new PreCommitEvent(eventMetadata));
        publisher.publishEvent(new CommitEvent<>(eventMetadata, List.of(new BatchByronBlock(eventMetadata, byronBlock))));

        //Finally Set the cursor. Checked again: a commit listener can hang while a restart starts a new session. In
        //this block's transaction, so a stale block is rolled back with its cursor.
        sessionGuard.writeIfCurrent(() -> cursorService.setCursor(new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(),
                eventMetadata.getBlock(), eventMetadata.getPrevBlockHash(), eventMetadata.getEra())));
    }

    @Override
    public void publishBlockEventsInParallel(EventMetadata eventMetadata, ByronMainBlock byronBlock, List<Transaction> transactions) {
        int size;
        synchronized (byronBatchBlockList) {
            ownBatch();
            byronBatchBlockList.add(new BatchByronBlock(eventMetadata, byronBlock));
            size = byronBatchBlockList.size();
        }
        if (size < storeProperties.getBlocksBatchSize())
            return;

        processBlocksInParallel();
    }

    public void processBlocksInParallel() {
        //Work on a copy: the list may be reset or refilled by another sync session while this batch is written
        List<BatchByronBlock> batch;
        synchronized (byronBatchBlockList) {
            ownBatch();
            if (byronBatchBlockList.isEmpty())
                return;
            batch = new ArrayList<>(byronBatchBlockList);
            byronBatchBlockList.clear();
        }

        Long session = sessionGuard.callerSession();
        List<List<BatchByronBlock>> partitions = partition(batch, storeProperties.getBlocksPartitionSize());

        List<CompletableFuture> futures = new ArrayList<>();
        for (List<BatchByronBlock> partition : partitions) {
            var future = CompletableFuture.supplyAsync(() -> {
                for (BatchByronBlock blockCache : partition) {
                    sessionGuard.check(session);
                    ByronMainBlockEvent byronMainBlockEvent = new ByronMainBlockEvent(blockCache.getMetadata(), blockCache.getBlock());
                    publisher.publishEvent(byronMainBlockEvent);
                }
                return true;
            }, sessionGuard.propagating(blockExecutor));
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(storeProperties.getProcessingThreadsTimeout(), TimeUnit.MINUTES)
                .join();

        //A batch of an old sync session is not committed and the cursor is not moved
        sessionGuard.checkCaller();

        BatchByronBlock lastBlockCache = batch.getLast();
        publisher.publishEvent(new PreCommitEvent(lastBlockCache.getMetadata()));
        publisher.publishEvent(new CommitEvent(lastBlockCache.getMetadata(), batch));

        //Checked again (a commit listener can hang while a restart starts a new session), and once more after the
        //write in the cursor's transaction
        sessionGuard.writeIfCurrent(() -> cursorService.setCursor(new Cursor(lastBlockCache.getMetadata().getSlot(),
                lastBlockCache.getMetadata().getBlockHash(), lastBlockCache.getMetadata().getBlock(),
                lastBlockCache.getMetadata().getPrevBlockHash(), lastBlockCache.getMetadata().getEra())));
    }

}
