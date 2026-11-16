package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.DRep;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepStatus;

import java.util.List;
import java.util.Optional;

public interface DRepStorage {
    void saveAll(List<DRep> dReps);
    Optional<DRep> findRecentDRepRegistration(String dRepId, Integer maxEpoch);

    /**
     * Find the latest stored row of a DRep that comes before the given certificate in chain order
     * (slot, then tx index, then cert index). Rows of the certificate itself and of later certificates are ignored,
     * so the answer is the same when a block is processed again after a restart.
     * <p>
     * It is used for every DRep update and retirement certificate. Do not answer with
     * {@link #findRecentDRepRegistration}: it could return a later row when a block is processed again.
     *
     * @param dRepId drep id
     * @param maxEpoch max epoch
     * @param slot slot of the certificate
     * @param txIndex tx index of the certificate
     * @param certIndex cert index of the certificate
     * @return the latest earlier row, if any
     */
    Optional<DRep> findRecentDRepRegistrationBefore(String dRepId, Integer maxEpoch, long slot, int txIndex, int certIndex);
    List<DRep> findDRepsByStatusAndEpoch(DRepStatus status, Integer epoch);
    int deleteBySlotGreaterThan(long slot);
}
