package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.mapper.ScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.ScriptEntity;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository.ScriptRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ScriptStorageImpl implements ScriptStorage {
    private final ScriptRepository scriptRepository;
    private final ScriptMapper scriptMapper;

    private List<Script> scriptCache = new ArrayList<>();

    @Override
    public List<Script> saveScripts(List<Script> scripts) {
        if (scripts != null && !scripts.isEmpty())
            scriptCache.addAll(scripts);

        return scripts;
    }

    @Override
    public Optional<Script> findByScriptHash(String scriptHash) {
        return scriptRepository.findById(scriptHash)
                .map(scriptMapper::toScript);
    }

    @EventListener
    @Transactional
    public void handleCommit(CommitEvent commitEvent) {
        try {
            List<ScriptEntity> scriptEntities = scriptCache.stream()
                    .filter(script -> !scriptRepository.existsById(script.getScriptHash()))
                    .map(scriptMapper::toScriptEntity)
                    .collect(Collectors.toList());

            scriptRepository.saveAll(scriptEntities);
        } finally {
            scriptCache.clear();
        }
    }
}
