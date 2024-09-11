package com.bloxbean.cardano.yaci.store.governance.storage.local;

import com.bloxbean.cardano.yaci.store.governance.domain.local.LocalDRepDistr;

import java.util.Optional;

public interface LocalDRepDistrStorageReader {
    Optional<LocalDRepDistr> findLatestLocalDRepDistrByDRepHash(String dRepHash);
}
