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
    public void saveAll(List<TxScript> txScripts) {
        List<TxScriptEntity> txScriptEntities = txScripts.stream().map(scriptMapper::toTxScriptEntity).collect(Collectors.toList());
        txScriptRepository.saveAll(txScriptEntities);
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return txScriptRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public List<TxScript> findByTxHash(String txHash) {
        List<Object[]> results = txScriptRepository.findByTxHash(txHash);
        return results.stream()
                .map(result -> {
                    TxScriptEntity txScriptEntity = (TxScriptEntity) result[0];
                    var datum = (String)result[1];
                    var redeemerData = (String)result[2];

                    var txScript = scriptMapper.toTxScript(txScriptEntity);

                    if (datum != null) {
                        txScript.setDatum(datum);
                    }

                    if (redeemerData != null) {
                        txScript.setRedeemerData(redeemerData);
                    }

                    return txScript;
                }).toList();
    }

}
