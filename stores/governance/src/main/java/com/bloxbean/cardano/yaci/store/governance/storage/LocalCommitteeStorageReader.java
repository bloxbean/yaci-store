package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalCommittee;

import java.util.Optional;

public interface LocalCommitteeStorageReader {
    Optional<LocalCommittee> findByMaxSlot();
}
