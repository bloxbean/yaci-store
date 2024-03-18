package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorageReader;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.mapper.JpaScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository.JpaScriptRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class JpaScriptStorageReader implements ScriptStorageReader {
    private final JpaScriptRepository jpaScriptRepository;
    private final JpaScriptMapper jpaScriptMapper;

    @Override
    public Optional<Script> findByScriptHash(String scriptHash) {
        return jpaScriptRepository.findById(scriptHash)
                .map(jpaScriptMapper::toScript);
    }
}
