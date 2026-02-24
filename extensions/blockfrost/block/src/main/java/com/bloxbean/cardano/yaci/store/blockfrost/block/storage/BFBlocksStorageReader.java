package com.bloxbean.cardano.yaci.store.blockfrost.block.storage;

import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockAddressTxRow;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockRow;
import com.bloxbean.cardano.yaci.store.blockfrost.block.storage.impl.model.BFBlockTxCborRow;
import com.bloxbean.cardano.yaci.store.common.model.Order;

import java.util.List;
import java.util.Optional;

public interface BFBlocksStorageReader {
    Optional<BFBlockRow> findLatestBlock();

    Optional<BFBlockRow> findBlockByHash(String hash);

    Optional<BFBlockRow> findBlockByNumber(long number);

    Optional<BFBlockRow> findBlockBySlot(long slot);

    Optional<BFBlockRow> findBlockByEpochAndSlot(int epoch, long slot);

    List<BFBlockRow> findNextBlocks(long blockNumber, int page, int count);

    List<BFBlockRow> findPreviousBlocks(long blockNumber, int page, int count);

    List<String> findBlockTxHashes(long blockNumber, int page, int count, Order order);

    List<BFBlockTxCborRow> findBlockTxCbor(long blockNumber, int page, int count, Order order);

    List<BFBlockAddressTxRow> findBlockAddressTransactions(long blockNumber, int page, int count);
}
