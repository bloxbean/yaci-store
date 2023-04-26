package com.bloxbean.cardano.yaci.store.transaction.storage.api;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;

import java.util.List;
import java.util.Optional;

public interface TransactionStorage {
    void saveAll(List<Txn> txList);
    Optional<Txn> getTransactionByTxHash(String txHash);
    List<Txn> getTransactions(int page, int count, Order order);
    int deleteBySlotGreaterThan(long slot);
}
