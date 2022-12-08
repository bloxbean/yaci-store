package com.bloxbean.cardano.yaci.indexer.script.processor;


import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.util.Tuple;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.indexer.events.ScriptEvent;
import com.bloxbean.cardano.yaci.indexer.events.domain.TxScripts;
import com.bloxbean.cardano.yaci.indexer.script.model.Script;
import com.bloxbean.cardano.yaci.indexer.script.model.ScriptType;
import com.bloxbean.cardano.yaci.indexer.script.model.TxScript;
import com.bloxbean.cardano.yaci.indexer.script.repository.ScriptRepository;
import com.bloxbean.cardano.yaci.indexer.script.repository.TxScriptRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScriptProcessor {
    private TxScriptRepository txScriptRepository;
    private ScriptRepository scriptRepository;

    public ScriptProcessor(ScriptRepository scriptRepository, TxScriptRepository txScriptRepository) {
        this.scriptRepository = scriptRepository;
        this.txScriptRepository = txScriptRepository;
    }

    @EventListener
    public void handleScriptEvent(ScriptEvent scriptEvent) {
        List<TxScripts> txScriptList = scriptEvent.getTxScriptsList();

        List<Script> scriptEntities = new ArrayList<>();
        List<TxScript> txScriptEntities = new ArrayList<>();
        txScriptList.stream()
                .forEach(txScripts -> {
                    if (txScripts.getPlutusV1Scripts() != null) {
                        List<Tuple<Script, TxScript>> plutusV1Scripts = txScripts.getPlutusV1Scripts().stream()
                                .map(plutusScript -> {
                                    return getPlutusScriptEntities(txScripts, plutusScript, ScriptType.PLUTUS_V1);
                                }).collect(Collectors.toList());

                        if (plutusV1Scripts.size() > 0) {
                            scriptEntities.addAll(plutusV1Scripts.stream().map(tuple -> tuple._1).collect(Collectors.toList()));
                            txScriptEntities.addAll(plutusV1Scripts.stream().map(tuple -> tuple._2).collect(Collectors.toList()));
                        }
                    }

                    if (txScripts.getPlutusV2Scripts() != null) {
                        List<Tuple<Script, TxScript>> plutusV2Scripts = txScripts.getPlutusV2Scripts().stream()
                                .map(plutusScript -> {
                                    return getPlutusScriptEntities(txScripts, plutusScript, ScriptType.PLUTUS_V2);
                                }).collect(Collectors.toList());

                        if (plutusV2Scripts != null) {
                            scriptEntities.addAll(plutusV2Scripts.stream().map(tuple -> tuple._1).collect(Collectors.toList()));
                            txScriptEntities.addAll(plutusV2Scripts.stream().map(tuple -> tuple._2).collect(Collectors.toList()));
                        }
                    }

                    if (txScripts.getNativeScripts() != null) {
                        List<Tuple<Script, TxScript>> nativeScripts = txScripts.getNativeScripts().stream()
                                .map(nativeScript -> {
                                    return getNativeScriptEntities(txScripts, nativeScript, ScriptType.NATIVE_SCRIPT);
                                }).collect(Collectors.toList());

                        if (nativeScripts != null) {
                            scriptEntities.addAll(nativeScripts.stream().map(tuple -> tuple._1).collect(Collectors.toList()));
                            txScriptEntities.addAll(nativeScripts.stream().map(tuple -> tuple._2).collect(Collectors.toList()));
                        }
                    }

                });

        scriptRepository.saveAll(scriptEntities);
        txScriptRepository.saveAll(txScriptEntities);
    }

    private Tuple<Script, TxScript> getPlutusScriptEntities(TxScripts txScripts, PlutusScript plutusScript,
                                                            ScriptType type) {
        String scriptHash = getPlutusScriptHash(plutusScript);
        Script script = Script.builder()
                .scriptHash(scriptHash)
                .plutusScript(plutusScript)
                .build();
        TxScript txScript = TxScript.builder()
                .txHash(txScripts.getTxHash())
                .type(type)
                .scriptHash(scriptHash)
                .build();
        return new Tuple(script, txScript);
    }

    private Tuple<Script, TxScript> getNativeScriptEntities(TxScripts txScripts, NativeScript nativeScript,
                                                            ScriptType type) {
        String scriptHash = getNativeScriptHash(nativeScript);
        Script script = Script.builder()
                .scriptHash(scriptHash)
                .nativeScript(nativeScript)
                .build();
        TxScript txScript = TxScript.builder()
                .txHash(txScripts.getTxHash())
                .type(type)
                .scriptHash(scriptHash)
                .build();
        return new Tuple(script, txScript);
    }

    private String getNativeScriptHash(NativeScript nativeScript) {
        try {
            com.bloxbean.cardano.client.transaction.spec.script.NativeScript nativeScript1
                    = com.bloxbean.cardano.client.transaction.spec.script.NativeScript.deserializeJson(nativeScript.getContent());
            return HexUtil.encodeHexString(nativeScript1.getScriptHash());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String getPlutusScriptHash(PlutusScript plutusScript) {
        byte[] bytes = HexUtil.decodeHexString(plutusScript.getContent());
        return HexUtil.encodeHexString(Blake2bUtil.blake2bHash224(bytes));
    }
}
