package com.bloxbean.cardano.yaci.store.core.service.publisher;

import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;

import java.util.List;

public interface BlockEventPublisher<T> {

    /**
     * Publish events in single thread
     * @param eventMetadata EventMetadata of the current block
     * @param block Current block
     * @param transactions List of transactions in the current block
     */
    void publishBlockEvents(EventMetadata eventMetadata, T block, List<Transaction> transactions);

    /**
     * Add the given blocks to block list and publish in parallel
     * @param eventMetadata EventMetadata of the current block
     * @param block Current block
     * @param transactions List of transactions in the current block
     */
    void publishBlockEventsInParallel(EventMetadata eventMetadata, T block, List<Transaction> transactions);

    /**
     * Process blocks in block list during parallel processing.
     */
    void processBlocksInParallel();
}
