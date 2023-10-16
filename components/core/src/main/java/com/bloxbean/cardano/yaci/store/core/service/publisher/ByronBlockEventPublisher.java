package com.bloxbean.cardano.yaci.store.core.service.publisher;

import com.bloxbean.cardano.yaci.core.model.byron.ByronMainBlock;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.core.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.domain.Cursor;
import com.bloxbean.cardano.yaci.store.core.service.CursorService;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchByronBlock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.bloxbean.cardano.yaci.store.common.util.ListUtil.partition;

@Component
@Slf4j
public class ByronBlockEventPublisher implements BlockEventPublisher<ByronMainBlock> {
    private final ApplicationEventPublisher publisher;
    private final CursorService cursorService;
    private final ExecutorService blockExecutor;
    private final StoreProperties storeProperties;

    public ByronBlockEventPublisher(@Qualifier("blockExecutor") ExecutorService blockExecutor,
                                    ApplicationEventPublisher publisher,
                                    CursorService cursorService,
                                    StoreProperties storeProperties) {
        this.blockExecutor = blockExecutor;
        this.publisher = publisher;
        this.cursorService = cursorService;
        this.storeProperties = storeProperties;
    }

    private List<BatchByronBlock> byronBatchBlockList = new ArrayList<>();

    @Override
    public void publishBlockEvents(EventMetadata eventMetadata, ByronMainBlock byronBlock, List<Transaction> transactions) {
        ByronMainBlockEvent byronMainBlockEvent = new ByronMainBlockEvent(eventMetadata, byronBlock);

        publisher.publishEvent(byronMainBlockEvent);
        publisher.publishEvent(new CommitEvent<>(List.of(new BatchByronBlock(eventMetadata, byronBlock))));

        //Finally Set the cursor
        cursorService.setCursor(new Cursor(eventMetadata.getSlot(), eventMetadata.getBlockHash(),
                eventMetadata.getBlock(), eventMetadata.getPrevBlockHash(), eventMetadata.getEra()));
    }

    @Override
    public void publishBlockEventsInParallel(EventMetadata eventMetadata, ByronMainBlock byronBlock, List<Transaction> transactions) {
        byronBatchBlockList.add(new BatchByronBlock(eventMetadata, byronBlock));
        if (byronBatchBlockList.size() != storeProperties.getBlocksBatchSize())
            return;

        processByronMainBlocksInParallel();
    }

    public void processByronMainBlocksInParallel() {
        if (byronBatchBlockList.size() == 0)
            return;

        List<List<BatchByronBlock>> partitions = partition(byronBatchBlockList, storeProperties.getBlocksPartitionSize());

        List<CompletableFuture> futures = new ArrayList<>();
        for (List<BatchByronBlock> partition : partitions) {
            var future = CompletableFuture.supplyAsync(() -> {
                for (BatchByronBlock blockCache : partition) {
                    ByronMainBlockEvent byronMainBlockEvent = new ByronMainBlockEvent(blockCache.getEventMetadata(), blockCache.getBlock());
                    publisher.publishEvent(byronMainBlockEvent);
                }
                return true;
            }, blockExecutor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        publisher.publishEvent(new CommitEvent(byronBatchBlockList));

        BatchByronBlock lastBlockCache = byronBatchBlockList.getLast();
        cursorService.setCursor(new Cursor(lastBlockCache.getEventMetadata().getSlot(), lastBlockCache.getEventMetadata().getBlockHash(),
                lastBlockCache.getEventMetadata().getBlock(), lastBlockCache.getEventMetadata().getPrevBlockHash(),
                lastBlockCache.getEventMetadata().getEra()));

        byronBatchBlockList.clear();
    }
}
