package com.bloxbean.cardano.yaci.store.script.service;

import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.transaction.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.script.domain.*;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ScriptService {
    private final ScriptStorage scriptStorage;
    private final TxScriptStorage txScriptStorage;
    private final DatumStorage datumStorage;
    private final ObjectMapper objectMapper;

    public Optional<Script> getScriptByHash(String scriptHash) {
        return scriptStorage.findByScriptHash(scriptHash);
    }

    public List<TxContractDetails> getTransactionScripts(String txHash) {
        List<TxScript> txScripts = txScriptStorage.findByTxHash(txHash);

        if (txScripts == null || txScripts.size() == 0)
            return Collections.EMPTY_LIST;

        return txScripts.stream().map(txScript -> {
            String scriptHash = txScript.getScriptHash();
            String scriptContent = null;
            if (scriptHash != null && !scriptHash.isEmpty()) {
                scriptContent = scriptStorage.findByScriptHash(scriptHash)
                        .map(script -> script.getContent())
                        .orElse(null);
            }

            Redeemer redeemerDto = ScriptUtil.deserializeRedeemer(txScript.getRedeemer())
                    .map(redeemer -> Redeemer.builder()
                            .tag(redeemer.getTag())
                            .index(redeemer.getIndex())
                            .exUnits(redeemer.getExUnits())
                            .data(redeemer.getData() != null ? redeemer.getData().serializeToHex() : null)
                            .build())
                    .orElse(null);

            return TxContractDetails.builder()
                    .txHash(txHash)
                    .scriptHash(txScript.getScriptHash())
                    .scriptContent(scriptContent)
                    .type(txScript.getType())
                    .redeemer(redeemerDto)
                    .datum(txScript.getDatum())
                    .datumHash(txScript.getDatumHash())
                    .build();
        }).collect(Collectors.toList());
    }

    public Optional<Datum> getDatum(String datumHash) {
        return datumStorage.getDatum(datumHash)
                .filter(datum -> datum.getDatum() != null);
    }

    public Optional<JsonNode> getDatumAsJson(String datumHash) {
        return getDatum(datumHash)
                .map(datum -> {
                    try {
                        PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(datum.getDatum()));
                        return objectMapper.readTree(PlutusDataJsonConverter.toJson(plutusData));
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to parse plutus data : " + datum.getDatum());
                    }
                });
    }
}
