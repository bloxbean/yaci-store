package com.bloxbean.cardano.yaci.store.transaction.storage.api;

import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface TransactionStorage {
    void saveAll(List<Txn> txList);
    Optional<Txn> findByTxHash(String txHash);
    Page<Txn> findAll(Pageable sortedBySlot);
    int deleteBySlotGreaterThan(long slot);
}
