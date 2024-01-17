package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.DrepRegistration;

import java.util.List;

public interface DrepRegistrationStorage {
    void saveAll(List<DrepRegistration> drepRegistrations);

    int deleteBySlotGreaterThan(long slot);
}
