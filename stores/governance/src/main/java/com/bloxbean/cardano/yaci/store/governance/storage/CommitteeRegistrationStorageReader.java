package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;

import java.util.List;

public interface CommitteeRegistrationStorageReader {
    List<CommitteeRegistration> findAll(int page, int count);
}
