package com.bloxbean.cardano.yaci.store.transaction.storage;

import com.bloxbean.cardano.yaci.store.transaction.domain.TxnCbor;

import java.util.List;

public interface TransactionCborStorage {
    void save(List<TxnCbor> txnCborList);

    int deleteBySlotGreaterThan(long slot);

    int deleteBySlotLessThan(long slot);
}
