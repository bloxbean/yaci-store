package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.carano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.script.helper.RedeemerDatumMatcher;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptContext;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil;
import com.bloxbean.cardano.yaci.store.script.helper.TxScriptFinder;
import com.bloxbean.cardano.yaci.store.script.model.ScriptEntity;
import com.bloxbean.cardano.yaci.store.script.model.TxScriptEntity;
import com.bloxbean.cardano.yaci.store.script.repository.ScriptRepository;
import com.bloxbean.cardano.yaci.store.script.repository.TxScriptRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.getDatumHash;
import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.getPlutusScriptHash;

@Component
@AllArgsConstructor
@Slf4j
public class ScriptRedeemerDatumProcessor {
    private TxScriptRepository txScriptRepository;
    private ScriptRepository scriptRepository;
    private RedeemerDatumMatcher redeemerMatcher;
    private TxScriptFinder txScriptFinder;

    @EventListener
    @Transactional
    public void handleScriptTransactionEvent(TransactionEvent transactionEvent) {
        for (Transaction transaction : transactionEvent.getTransactions()) {
            handleScriptTransaction(transactionEvent.getMetadata(), transaction);
        }
    }

    /**
     * Match redeemers with scripts
     * Check script in WitnessSet and reference inputs
     *
     * @param metadata
     * @param transaction
     */
    private void handleScriptTransaction(EventMetadata metadata, Transaction transaction) {
        if (transaction.isInvalid())
            return;
        if (transaction.getWitnesses().getRedeemers() == null || transaction.getWitnesses().getRedeemers().size() == 0)
            return;

        //Find all available scripts in a transaction
        Map<String, PlutusScript> scriptsMap = txScriptFinder.getScripts(transaction);

        //Get redeemer to script mapping
        List<ScriptContext> scriptContexts = redeemerMatcher.findScriptsForRedeemers(transaction, scriptsMap);

        Map<String, String> datumHashToDatumMap = findWitnessDatum(transaction);

        //Convert to TxScript objects
        List<TxScriptEntity> txScripts = scriptContexts.stream()
                .map(scriptContext -> {
                    //Try to set datum
                    if (StringUtil.isEmpty(scriptContext.getDatum())) {
                        String datumHash = scriptContext.getDatumHash();
                        if (datumHash != null && !datumHash.isEmpty())
                            scriptContext.setDatum(datumHashToDatumMap.get(datumHash));
                    }
                    //check if datum hash is not set
                    if (StringUtil.isEmpty(scriptContext.getDatumHash()) && !StringUtil.isEmpty(scriptContext.getDatum()))
                        scriptContext.setDatumHash(ScriptUtil.getDatumHash(scriptContext.getDatum()));

                    return TxScriptEntity.builder()
                            .txHash(transaction.getTxHash())
                            .slot(metadata.getSlot())
                            .block(metadata.getBlock())
                            .blockHash(metadata.getBlockHash())
                            .scriptHash(scriptContext.getScriptHash())
                            .type(scriptContext.getPlutusScriptType())
                            .redeemer(scriptContext.getRedeemer())
                            .datum(scriptContext.getDatum())
                            .datumHash(scriptContext.getDatumHash())
                            .build();
                }).collect(Collectors.toList());

        //Create TxScript entities to save
        List<ScriptEntity> plutusScripts = scriptsMap.values().stream()
                        .map(plutusScript -> ScriptEntity.builder()
                                .scriptHash(getPlutusScriptHash(plutusScript))
                                .plutusScript(plutusScript)
                                .build()).collect(Collectors.toList());
        if (plutusScripts != null && plutusScripts.size() > 0)
            scriptRepository.saveAll(plutusScripts);

        //Get all native scripts  and save
        if (transaction.getWitnesses().getNativeScripts() != null) {
            List<ScriptEntity> nativeScripts = transaction.getWitnesses().getNativeScripts().stream()
                    .map(nativeScript -> ScriptEntity.builder()
                            .scriptHash(ScriptUtil.getNativeScriptHash(nativeScript))
                            .nativeScript(nativeScript)
                            .build()).collect(Collectors.toList());
            if (nativeScripts != null && nativeScripts.size() > 0)
                scriptRepository.saveAll(nativeScripts);
        }

        if (txScripts.size() > 0)
         txScriptRepository.saveAll(txScripts);
    }

    private Map<String, String> findWitnessDatum(Transaction transaction) {
        if (transaction.getWitnesses().getDatums() == null)
            return Collections.EMPTY_MAP;

        Map<String, String> datumHashMap = new HashMap<>();
        transaction.getWitnesses().getDatums()
                .stream()
                .forEach(datum -> {
                    String datumHash = getDatumHash(datum);
                    if (datumHash != null)
                        datumHashMap.put(datumHash, datum.getCbor());
                });

        return datumHashMap;
    }
}
