package com.bloxbean.cardano.yaci.store.script.storage.impl;

import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.mapper.ScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.model.ScriptEntity;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.ScriptRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ScriptStorageImpl implements ScriptStorage {
    private final ScriptRepository scriptRepository;
    private final ScriptMapper scriptMapper;

    private List<Script> scriptCache = Collections.synchronizedList(new ArrayList<>());

    @Override
    public List<Script> saveScripts(List<Script> scripts) {
        if (scripts != null && !scripts.isEmpty())
            scriptCache.addAll(scripts);

        return scripts;
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
