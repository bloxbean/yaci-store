package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeDeRegistration;

import java.util.List;

public interface CommitteeDeRegistrationStorage {
    void saveAll(List<CommitteeDeRegistration> committeeDeRegistrations);

    int deleteBySlotGreaterThan(long slot);
}
