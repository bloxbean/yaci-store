package com.bloxbean.cardano.yaci.store.transaction.storage;

import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;

import java.util.List;

public interface TransactionWitnessStorageReader {
    List<TxnWitness> getTransactionWitnesses(String txHash);
}
