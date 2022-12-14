package com.bloxbean.cardano.yaci.store.script.service;

import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.script.domain.*;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil;
import com.bloxbean.cardano.yaci.store.script.model.ScriptEntity;
import com.bloxbean.cardano.yaci.store.script.model.TxScriptEntity;
import com.bloxbean.cardano.yaci.store.script.repository.ScriptRepository;
import com.bloxbean.cardano.yaci.store.script.repository.TxScriptRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class ScriptService {
    private ScriptRepository scriptRepository;
    private TxScriptRepository txScriptRepository;

    public Optional<Script> getScriptByHash(String scriptHash) {
        Optional<ScriptEntity> scriptOptional = scriptRepository.findById(scriptHash);
        if (scriptOptional.isPresent()) {
            ScriptEntity script = scriptOptional.get();
            if (script.getNativeScript() != null) {
                JsonNode jsonNode;
                try {
                    jsonNode = JsonUtil.parseJson(script.getNativeScript().getContent());
                } catch (Exception e) {
                    throw new IllegalStateException("NativeScript content cannot be parsed");
                }

                return Optional.of(NativeScript.builder()
                        .scriptHash(script.getScriptHash())
                        .content(jsonNode)
                        .build());
            } else if (script.getPlutusScript() != null) {
                return Optional.of(PlutusScript.builder()
                        .scriptHash(script.getScriptHash())
                        .content(script.getPlutusScript().getContent())
                        .type(script.getPlutusScript().getType())
                        .build()
                );
            } else
                throw new IllegalStateException("Invalid script type");
        } else {
            return Optional.empty();
        }
    }

    public List<TxContractDetails> getTransactionScripts(String txHash) {
        List<TxScriptEntity> txScripts = txScriptRepository.findByTxHash(txHash);

        if (txScripts == null || txScripts.size() == 0)
            return Collections.EMPTY_LIST;

        return txScripts.stream().map(txScript -> {
            String scriptHash = txScript.getScriptHash();
            com.bloxbean.cardano.yaci.core.model.PlutusScript plutusScript = null;

            if (scriptHash != null && !scriptHash.isEmpty()) {
                plutusScript = scriptRepository.findById(scriptHash)
                        .map(script -> script.getPlutusScript())
                        .orElse(null);
            }

            String content = plutusScript != null? plutusScript.getContent(): null;

            Redeemer redeemerDto = ScriptUtil.deserializeRedeemer(txScript.getRedeemer())
                    .map(redeemer -> Redeemer.builder()
                            .tag(redeemer.getTag())
                            .index(redeemer.getIndex())
                            .exUnits(redeemer.getExUnits())
                            .data(redeemer.getData() != null? redeemer.getData().serializeToHex(): null)
                            .build())
                    .orElse(null);

            return TxContractDetails.builder()
                    .txHash(txHash)
                    .scriptHash(txScript.getScriptHash())
                    .scriptContent(content)
                    .type(txScript.getType())
                    .redeemer(redeemerDto)
                    .datum(txScript.getDatum())
                    .datumHash(txScript.getDatumHash())
                    .build();
        }).collect(Collectors.toList());
    }
}
