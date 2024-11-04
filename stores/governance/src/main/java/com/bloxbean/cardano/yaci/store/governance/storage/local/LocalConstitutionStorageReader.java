package com.bloxbean.cardano.yaci.store.governance.storage.local;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalConstitution;

import java.util.Optional;

public interface LocalConstitutionStorageReader {
    Optional<LocalConstitution> findByMaxSlot();
}
