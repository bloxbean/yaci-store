package com.bloxbean.cardano.yaci.store.script.storage.impl;

import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorageReader;
import com.bloxbean.cardano.yaci.store.script.storage.impl.mapper.ScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.model.TxScriptEntityJpa;
import com.bloxbean.cardano.yaci.store.script.storage.impl.repository.TxScriptRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class TxScriptStorageReaderImpl implements TxScriptStorageReader {
    private final TxScriptRepository txScriptRepository;
    private final ScriptMapper scriptMapper;

    @Override
    public List<TxScript> findByTxHash(String txHash) {
        List<Object[]> results = txScriptRepository.findByTxHash(txHash);
        return results.stream()
                .map(result -> {
                    TxScriptEntityJpa txScriptEntity = (TxScriptEntityJpa) result[0];
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
