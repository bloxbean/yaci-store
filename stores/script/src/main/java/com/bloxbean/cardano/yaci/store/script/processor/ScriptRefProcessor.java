package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.NativeScript;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.common.util.ScriptReferenceUtil;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Script;
import com.bloxbean.cardano.yaci.store.script.domain.ScriptType;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.script.ScriptStoreConfiguration.STORE_SCRIPT_ENABLED;
import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.toPlutusScript;
import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.toScriptType;

@Component
@RequiredArgsConstructor
@EnableIf(STORE_SCRIPT_ENABLED)
@Slf4j
public class ScriptRefProcessor {
    private final ScriptStorage scriptStorage;

    @EventListener
    @Transactional
    public void processScriptRefInUtxo(TransactionEvent transactionEvent) {
        try {
            List<Script> scriptList = transactionEvent.getTransactions()
                    .stream()
                    .filter(transaction -> !transaction.isInvalid())
                    .flatMap(transaction -> transaction.getBody().getOutputs().stream())
                    .map(transactionOutput -> transactionOutput.getScriptRef())
                    .filter(Objects::nonNull)
                    .map(scriptRef -> {
                        com.bloxbean.cardano.client.spec.Script script;
                        try {
                            script = ScriptReferenceUtil.deserializeScriptRef(HexUtil.decodeHexString(scriptRef));
                        } catch (Exception e) {
                            log.error("Script deserialization failed. Block hash: " + transactionEvent.getMetadata().getBlockHash(), e);
                            return null;
                        }

                        ScriptType scriptType = toScriptType(script);
                        String scriptHash;
                        String content;

                        if (scriptType == ScriptType.NATIVE_SCRIPT) {
                            NativeScript nativeScript = NativeScript.builder()
                                    .type(script.getScriptType())
                                    .content(JsonUtil.getJson(script))
                                    .build();

                            content = JsonUtil.getJson(nativeScript);
                            try {
                                scriptHash = ScriptUtil.getNativeScriptHash(nativeScript);
                            } catch (Exception e) {
                                log.error("Error getting native script hash, Block hash: " + transactionEvent.getMetadata().getBlockHash(), e);
                                return null;
                            }
                        } else {
                            PlutusScript plutusScript = toPlutusScript(script);

                            content = JsonUtil.getJson(plutusScript);

                            try {
                                scriptHash = ScriptUtil.getPlutusScriptHash(plutusScript);
                            } catch (Exception e) {
                                log.error("Error getting native script hash, Block hash: " + transactionEvent.getMetadata().getBlockHash(), e);
                                return null;
                            }
                        }

                        return Script.builder()
                                .scriptHash(scriptHash)
                                .scriptType(scriptType)
                                .content(content)
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            //Save the scripts
            if (scriptList.size() > 0) {
                scriptStorage.saveScripts(scriptList);
            }
        } catch (Exception e) {
            log.error("Error saving script ref in utxo. Block: {} ", transactionEvent.getMetadata().getBlock(), e);
        }
    }

    @EventListener
    public void handleCommit(CommitEvent commitEvent) {
        scriptStorage.handleCommit(commitEvent);
    }
}
