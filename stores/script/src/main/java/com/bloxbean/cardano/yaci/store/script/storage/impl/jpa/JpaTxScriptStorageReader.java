package com.bloxbean.cardano.yaci.store.script.storage.impl.jpa;

import com.bloxbean.cardano.yaci.store.script.domain.TxScript;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorageReader;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.mapper.JpaScriptMapper;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.model.JpaTxScriptEntity;
import com.bloxbean.cardano.yaci.store.script.storage.impl.jpa.repository.JpaTxScriptRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class JpaTxScriptStorageReader implements TxScriptStorageReader {

    private final JpaTxScriptRepository jpaTxScriptRepository;
    private final JpaScriptMapper jpaScriptMapper;

    @Override
    public List<TxScript> findByTxHash(String txHash) {
        List<Object[]> results = jpaTxScriptRepository.findByTxHash(txHash);
        return results.stream()
                .map(result -> {
                    JpaTxScriptEntity txScriptEntity = (JpaTxScriptEntity) result[0];
                    var datum = (String)result[1];
                    var redeemerData = (String)result[2];

                    var txScript = jpaScriptMapper.toTxScript(txScriptEntity);

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
