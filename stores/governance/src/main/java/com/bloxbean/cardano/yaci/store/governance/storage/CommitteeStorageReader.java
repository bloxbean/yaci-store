package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.Committee;

import java.util.Optional;

public interface CommitteeStorageReader {
    Optional<Committee> findByMaxEpoch();
}
