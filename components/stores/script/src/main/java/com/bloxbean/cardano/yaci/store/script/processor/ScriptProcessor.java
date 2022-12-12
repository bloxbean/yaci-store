package com.bloxbean.cardano.yaci.store.script.processor;


import com.bloxbean.cardano.client.util.Tuple;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.store.events.ScriptEvent;
import com.bloxbean.cardano.yaci.store.events.domain.TxScripts;
import com.bloxbean.cardano.yaci.store.script.model.Script;
import com.bloxbean.cardano.yaci.store.script.model.ScriptType;
import com.bloxbean.cardano.yaci.store.script.model.TxScript;
import com.bloxbean.cardano.yaci.store.script.repository.ScriptRepository;
import com.bloxbean.cardano.yaci.store.script.repository.TxScriptRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.getNativeScriptHash;
import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.getPlutusScriptHash;

@Component
public class ScriptProcessor {
    private TxScriptRepository txScriptRepository;
    private ScriptRepository scriptRepository;

    public ScriptProcessor(ScriptRepository scriptRepository, TxScriptRepository txScriptRepository) {
        this.scriptRepository = scriptRepository;
        this.txScriptRepository = txScriptRepository;
    }

   // @EventListener
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
        //TODO -- txScriptRepository.saveAll(txScriptEntities);
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
}
