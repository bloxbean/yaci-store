package com.bloxbean.cardano.yaci.store.core.service.publisher;

import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
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
        byronBatchBlockList.clear();
    }

    @Transactional
    @Override
    public void publishBlockEvents(EventMetadata eventMetadata, ByronMainBlock byronBlock, List<Transaction> transactions) {
        ByronMainBlockEvent byronMainBlockEvent = new ByronMainBlockEvent(eventMetadata, byronBlock);

        publisher.publishEvent(byronMainBlockEvent);
        publisher.publishEvent(new PreCommitEvent(eventMetadata));
        publisher.publishEvent(new CommitEvent<>(eventMetadata, List.of(new BatchByronBlock(eventMetadata, byronBlock))));

        //Finally Set the cursor
        cursorService.setCursor(new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(),
                eventMetadata.getBlock(), eventMetadata.getPrevBlockHash(), eventMetadata.getEra()));
    }

    @Override
    public void publishBlockEventsInParallel(EventMetadata eventMetadata, ByronMainBlock byronBlock, List<Transaction> transactions) {
        byronBatchBlockList.add(new BatchByronBlock(eventMetadata, byronBlock));
        if (byronBatchBlockList.size() != storeProperties.getBlocksBatchSize())
            return;

        processBlocksInParallel();
    }

    public void processBlocksInParallel() {
        if (byronBatchBlockList.size() == 0)
            return;

        List<List<BatchByronBlock>> partitions = partition(byronBatchBlockList, storeProperties.getBlocksPartitionSize());

        List<CompletableFuture> futures = new ArrayList<>();
        for (List<BatchByronBlock> partition : partitions) {
            var future = CompletableFuture.supplyAsync(() -> {
                for (BatchByronBlock blockCache : partition) {
                    ByronMainBlockEvent byronMainBlockEvent = new ByronMainBlockEvent(blockCache.getMetadata(), blockCache.getBlock());
                    publisher.publishEvent(byronMainBlockEvent);
                }
                return true;
            }, blockExecutor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .orTimeout(storeProperties.getProcessingThreadsTimeout(), TimeUnit.MINUTES)
                .join();

        BatchByronBlock lastBlockCache = byronBatchBlockList.getLast();
        publisher.publishEvent(new PreCommitEvent(lastBlockCache.getMetadata()));
        publisher.publishEvent(new CommitEvent(lastBlockCache.getMetadata(), byronBatchBlockList));

        cursorService.setCursor(new Cursor(lastBlockCache.getMetadata().getSlot(), lastBlockCache.getMetadata().getBlockHash(),
                lastBlockCache.getMetadata().getBlock(), lastBlockCache.getMetadata().getPrevBlockHash(),
                lastBlockCache.getMetadata().getEra()));

        byronBatchBlockList.clear();
    }

}
