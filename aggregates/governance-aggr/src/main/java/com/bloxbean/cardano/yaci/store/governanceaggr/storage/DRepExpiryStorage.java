package com.bloxbean.cardano.yaci.store.governanceaggr.storage;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.DRepExpiry;

import java.util.List;

public interface DRepExpiryStorage {
    void save(List<DRepExpiry> dRepExpiryList);
    List<DRepExpiry> findByEpoch(Integer epoch);
}
