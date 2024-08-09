package com.bloxbean.cardano.yaci.store.staking.storage;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolDetails;

import java.util.List;

public interface PoolStorageReader {

    /**
     * Find latest pool details for a given epoch
     * @param poolIds
     * @param epoch
     * @return List of PoolDetails
     */
    List<PoolDetails> getPoolDetails(List<String> poolIds, Integer epoch);

    List<PoolDetails> getLatestPoolUpdateDetails(List<String> poolIds, Integer txSubmissionEpoch);

}
