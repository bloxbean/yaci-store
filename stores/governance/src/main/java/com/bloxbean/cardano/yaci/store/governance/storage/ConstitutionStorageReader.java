package com.bloxbean.cardano.yaci.store.governance.storage;

import com.bloxbean.cardano.yaci.store.governance.domain.Constitution;

import java.util.Optional;

public interface ConstitutionStorageReader {
    Optional<Constitution> findCurrentConstitution();
}
