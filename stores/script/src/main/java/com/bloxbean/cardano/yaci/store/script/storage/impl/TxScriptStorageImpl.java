package com.bloxbean.cardano.yaci.store.script.storage.impl;

import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.mapper.ScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.model.TxScriptEntityJpa;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.TxScriptRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TxScriptStorageImpl implements TxScriptStorage {
    private final TxScriptRepository txScriptRepository;
    private final ScriptMapper scriptMapper;

    @Override
    public void saveAll(List<TxScript> txScripts) {
        List<TxScriptEntityJpa> txScriptEntities = txScripts.stream().map(scriptMapper::toTxScriptEntity).collect(Collectors.toList());
        txScriptRepository.saveAll(txScriptEntities);
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txScriptRepository.deleteBySlotGreaterThan(slot);
    }

}
