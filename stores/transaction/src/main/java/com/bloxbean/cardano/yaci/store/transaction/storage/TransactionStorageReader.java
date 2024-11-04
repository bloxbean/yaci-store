package com.bloxbean.cardano.yaci.store.transaction.storage;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

public interface TransactionStorageReader {
    Optional<Txn> getTransactionByTxHash(String txHash);
    List<Txn> getTransactions(int page, int count, Order order);
    List<Txn> getTransactionsByBlockHash(String blockHash);
    List<Txn> getTransactionsByBlockNumber(long blockNumber);

    BigInteger getTotalFee(int epoch);
}
