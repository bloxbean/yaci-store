package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Script;
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

import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.getPlutusScriptHash;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScriptRefProcessor {
    private final ScriptStorage scriptStorage;

    @EventListener
    @Transactional
    public void processScriptRefInUtxo(TransactionEvent transactionEvent) {
        try {
            List<Script> plutusScriptList = transactionEvent.getTransactions()
                    .stream()
                    .flatMap(transaction -> transaction.getBody().getOutputs().stream())
                    .map(transactionOutput -> transactionOutput.getScriptRef())
                    .filter(Objects::nonNull)
                    .map(scriptRef -> ScriptUtil.deserializeScriptRef(scriptRef))
                    .filter(Objects::nonNull)
                    .map(plutusScript -> Script.builder()
                            .scriptHash(getPlutusScriptHash(plutusScript))
                            .scriptType(ScriptUtil.toPlutusScriptType(plutusScript.getType()))
                            .content(JsonUtil.getJson(plutusScript))
                            .build())
                    .collect(Collectors.toList());

            //Save the scripts
            if (plutusScriptList.size() > 0) {
                scriptStorage.saveScripts(plutusScriptList);
            }
        } catch (Exception e) {
            log.error("Error saving script ref in utxo. Block: {} ", transactionEvent.getMetadata().getBlock(), e);
        }
    }
}
