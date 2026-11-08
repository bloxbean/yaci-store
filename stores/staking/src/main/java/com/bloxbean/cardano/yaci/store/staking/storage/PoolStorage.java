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
     * Find the latest registration of a pool that comes before the given certificate in chain order
     * (slot, then tx index, then cert index). Rows of the certificate itself and of later certificates are ignored,
     * so the answer is the same when a block is processed again after a restart.
     * <p>
     * Custom {@code PoolStorage} implementations must implement this method. It is used for every pool registration
     * certificate. The default only throws, because answering with {@link #findRecentPoolRegistration} would count
     * the certificate's own row and turn a replayed registration into an update.
     *
     * @param poolId pool id
     * @param maxEpoch max epoch
     * @param slot slot of the certificate
     * @param txIndex tx index of the certificate
     * @param certIndex cert index of the certificate
     * @return the latest earlier registration, if any
     * @throws UnsupportedOperationException if the implementation does not override this method
     */
    default Optional<Pool> findRecentPoolRegistrationBefore(String poolId, Integer maxEpoch, long slot, int txIndex, int certIndex) {
        throw new UnsupportedOperationException(getClass().getName() + " must implement "
                + "PoolStorage.findRecentPoolRegistrationBefore(poolId, maxEpoch, slot, txIndex, certIndex) "
                + "to process pool registrations");
    }

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
     * Find retired pools for a given epoch
     * @param epoch
     * @return List of pools
     */
    List<Pool> findRetiredPools(Integer epoch);

    /**
     * Find active pools for a given epoch
     * @param epoch
     * @return List of pools
     */
    List<Pool> findActivePools(Integer epoch);

    /**
     * Delete all records with slot greater than the given slot
     * @param slot
     * @return
     */
    int deleteBySlotGreaterThan(long slot);
}
