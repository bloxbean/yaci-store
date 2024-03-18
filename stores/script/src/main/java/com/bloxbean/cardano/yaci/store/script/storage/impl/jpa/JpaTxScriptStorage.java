package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.mapper.JpaScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.JpaTxScriptEntity;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository.JpaTxScriptRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class JpaTxScriptStorage implements TxScriptStorage {
    private final JpaTxScriptRepository jpaTxScriptRepository;
    private final JpaScriptMapper jpaScriptMapper;

    @Override
    public void saveAll(List<TxScript> txScripts) {
        List<JpaTxScriptEntity> txScriptEntities = txScripts.stream().map(jpaScriptMapper::toTxScriptEntity).collect(Collectors.toList());
        jpaTxScriptRepository.saveAll(txScriptEntities);
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return jpaTxScriptRepository.deleteBySlotGreaterThan(slot);
    }

}
