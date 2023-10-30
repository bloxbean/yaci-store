package com.bloxbean.cardano.yaci.store.core.service.publisher;

import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;

import java.util.List;

public interface BlockEventPublisher<T> {
    void publishBlockEvents(EventMetadata eventMetadata, T block, List<Transaction> transactions);
    void publishBlockEventsInParallel(EventMetadata eventMetadata, T block, List<Transaction> transactions);
}
