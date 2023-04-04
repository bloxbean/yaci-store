package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.mapper.ScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.ScriptEntity;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository.ScriptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScriptStorageImpl implements ScriptStorage {
    private final ScriptRepository scriptRepository;
    private final ScriptMapper scriptMapper;

    @Override
    public List<Script> saveScripts(List<Script> scripts) {
        List<ScriptEntity> scriptEntities = scripts.stream().map(scriptMapper::toScriptEntity).collect(Collectors.toList());
        List<ScriptEntity> savedEntities = scriptRepository.saveAll(scriptEntities);

        return savedEntities.stream().map(scriptMapper::toScript).collect(Collectors.toList());
    }

    @Override
    public Optional<Script> findByScriptHash(String scriptHash) {
        return scriptRepository.findById(scriptHash)
                .map(scriptMapper::toScript);
    }
}
