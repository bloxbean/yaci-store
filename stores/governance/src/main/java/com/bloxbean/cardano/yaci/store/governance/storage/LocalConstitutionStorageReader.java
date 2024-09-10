package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.LocalConstitution;

import java.util.Optional;

public interface LocalConstitutionStorageReader {
    Optional<LocalConstitution> findByMaxSlot();
}
