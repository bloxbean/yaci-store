package com.bloxbean.cardano.yaci.store.governance.storage.local;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalCommittee;

import java.util.Optional;

public interface LocalCommitteeStorageReader {
    Optional<LocalCommittee> findByMaxSlot();
}
