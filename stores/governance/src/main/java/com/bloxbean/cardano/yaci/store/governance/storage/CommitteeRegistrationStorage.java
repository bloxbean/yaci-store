package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;

import java.util.List;

public interface CommitteeRegistrationStorage {
    void saveAll(List<CommitteeRegistration> committeeRegistrations);

    int deleteBySlotGreaterThan(long slot);
}
