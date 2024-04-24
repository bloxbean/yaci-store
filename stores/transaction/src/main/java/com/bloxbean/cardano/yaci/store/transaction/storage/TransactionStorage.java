package com.bloxbean.cardano.yaci.store.transaction.storage;

import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;

import java.util.List;

public interface TransactionStorage {
    void saveAll(List<Txn> txList);
    int deleteBySlotGreaterThan(long slot);
    int deleteBySlotLessThan(long slot);
}
