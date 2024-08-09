package com.bloxbean.cardano.yaci.store.staking.storage;

import com.bloxbean.cardano.yaci.store.staking.domain.Pool;

import java.util.List;
import java.util.Optional;

public interface PoolStorage {
    void save(List<Pool> pools);

    /**
     * Find recent pool registration
     * @param poolId
     * @param maxEpoch
     * @return Pool object if found
     */
    Optional<Pool> findRecentPoolRegistration(String poolId, Integer maxEpoch);

    /**
     * Find recent pool update
     * @param poolId pool id
     * @param maxEpoch max epoch
     * @return Pool object if found
     */
    Optional<Pool> findRecentPoolUpdate(String poolId, Integer maxEpoch);

    /**
     * Find recent pool retirement. It checks if there is no other retirement or update certificate after this.
     * @param poolId pool id
     * @param retirementEpoch retirement epoch
     * @return Pool object if found
     */
    Optional<Pool> findRecentPoolRetirement(String poolId, Integer retirementEpoch);

    /**
     * Find recent pool retired
     * @param poolId pool id
     * @param maxEpoch max epoch
     * @return Pool object if found
     */
    Optional<Pool> findRecentPoolRetired(String poolId, Integer maxEpoch);

    /**
     * Find retiring pools for a given epoch
     * @param epoch
     * @return List of pools
     */
    List<Pool> findRetiringPools(Integer epoch);

    /**
     * Get max epoch
     * @return
     */
    Integer getMaxEpoch();

    /**
     * Delete all records with slot greater than the given slot
     * @param slot
     * @return
     */
    int deleteBySlotGreaterThan(long slot);
}
