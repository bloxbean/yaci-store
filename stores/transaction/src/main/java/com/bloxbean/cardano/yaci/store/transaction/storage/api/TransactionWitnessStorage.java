package com.bloxbean.cardano.yaci.store.transaction.storage.api;

import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;

import java.util.List;

public interface TransactionWitnessStorage {
    void saveAll(List<TxnWitness> txnWitnesses);
    List<TxnWitness> getTransactionWitnesses(String txHash);
    int deleteBySlotGreaterThan(long slot);
}
