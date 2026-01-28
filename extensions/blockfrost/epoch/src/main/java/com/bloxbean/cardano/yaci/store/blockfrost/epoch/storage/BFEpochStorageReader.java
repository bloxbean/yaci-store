package com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface BFEpochStorageReader {
    List<Epoch> findNextEpochs(int epoch, int page, int count);

    List<Epoch> findPreviousEpochs(int epoch, int page, int count);

    List<String> findBlockHashesByEpoch(int epoch, int page, int count, Order order);

    List<String> findBlockHashesByEpochAndPool(int epoch, String poolId, int page, int count, Order order);

    Map<Integer, BigInteger> getActiveStakesByEpochs(List<Integer> epochs);
}
