package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.mapper.ScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.TxScriptEntity;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository.TxScriptRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TxScriptStorageImpl implements TxScriptStorage {
    private final TxScriptRepository txScriptRepository;
    private final ScriptMapper scriptMapper;

    @Override
    public List<TxScript> saveAll(List<TxScript> txScripts) {
        List<TxScriptEntity> txScriptEntities = txScripts.stream().map(scriptMapper::toTxScriptEntity).collect(Collectors.toList());
        List<TxScriptEntity> savedEntities = txScriptRepository.saveAll(txScriptEntities);

        return savedEntities.stream().map(scriptMapper::toTxScript).collect(Collectors.toList());
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txScriptRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public List<TxScript> findByTxHash(String txHash) {
        return txScriptRepository.findByTxHash(txHash)
                .stream().map(scriptMapper::toTxScript).collect(Collectors.toList());
    }
}
