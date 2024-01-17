package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;

import java.util.List;

public interface DRepRegistrationStorage {
    void saveAll(List<DRepRegistration> dRepRegistrations);

    int deleteBySlotGreaterThan(long slot);
}
