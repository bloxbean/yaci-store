package com.bloxbean.cardano.yaci.store.script.storage.impl;

import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorageReader;
import com.bloxbean.cardano.yaci.store.script.storage.impl.mapper.ScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.ScriptRepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class ScriptStorageReaderImpl implements ScriptStorageReader {
    private final ScriptRepository scriptRepository;
    private final ScriptMapper scriptMapper;

    @Override
    public Optional<Script> findByScriptHash(String scriptHash) {
        return scriptRepository.findById(scriptHash)
                .map(scriptMapper::toScript);
    }
}
