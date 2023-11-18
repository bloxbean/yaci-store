package com.bloxbean.cardano.yaci.store.script.storage;

import com.bloxbean.cardano.yaci.store.script.domain.Script;

import java.util.Optional;

public interface ScriptStorageReader {
    Optional<Script> findByScriptHash(String scriptHash);
}
