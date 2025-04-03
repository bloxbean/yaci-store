package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.DRep;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepStatus;

import java.util.List;
import java.util.Optional;

public interface DRepStorage {
    void saveAll(List<DRep> dReps);
    Optional<DRep> findRecentDRepRegistration(String dRepId, Integer maxEpoch);
    List<DRep> findDRepsByStatusAndEpoch(DRepStatus status, Integer epoch);
    int deleteBySlotGreaterThan(long slot);
}
