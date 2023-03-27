package com.bloxbean.cardano.yaci.store.script.storage;

import com.bloxbean.cardano.yaci.store.script.domain.Script;

import java.util.List;
import java.util.Optional;

public interface ScriptStorage {
    List<Script> saveScripts(List<Script> scripts);
    Optional<Script> findByScriptHash(String scriptHash);
}
