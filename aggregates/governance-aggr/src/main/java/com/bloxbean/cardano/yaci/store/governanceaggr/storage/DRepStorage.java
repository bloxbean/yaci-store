package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRep;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.DRepStatus;

import java.util.List;
import java.util.Optional;

public interface DRepStorage {
    void saveAll(List<DRep> dReps);
    Optional<DRep> findRecentDRepRegistration(String dRepId, Integer maxEpoch);
    List<DRep> findDRepsByStatus(DRepStatus status, int page, int count, Order sort);
    int deleteBySlotGreaterThan(long slot);
}
