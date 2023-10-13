package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.events.internal.BatchBlocksProcessedEvent;
import com.bloxbean.cardano.yaci.store.script.domain.*;
import com.bloxbean.cardano.yaci.store.script.helper.RedeemerDatumMatcher;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptContext;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil;
import com.bloxbean.cardano.yaci.store.script.helper.TxScriptFinder;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
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
@RequiredArgsConstructor
@Slf4j
public class ScriptRedeemerDatumProcessor {
    private final TxScriptStorage txScriptStorage;
    private final ScriptStorage scriptStorage;
    private final DatumStorage datumStorage;
    private final RedeemerDatumMatcher redeemerMatcher;
    private final TxScriptFinder txScriptFinder;
    private final ApplicationEventPublisher publisher;

    /**
     * This method is called when parallel mode is disabled.
     * @param transactionEvent
     */
    @EventListener
    @Transactional
    public void handleScriptTransactionEvent(TransactionEvent transactionEvent) {
        if (transactionEvent.getMetadata().isParallelMode()) //Skip when parallel mode as it will be handled by BlockCacheProcessedEvent
            return;

        for (Transaction transaction : transactionEvent.getTransactions()) {
            handleScriptTransaction(transactionEvent.getMetadata(), transaction);
        }
    }

    /**
     * This method is called when parallel mode is enabled.
     * @param blockCacheProcessedEvent
     */
    @EventListener
    @Transactional
    @Timed(value = "store.script.batch.process", percentiles = {0.5, 0.95, 0.99}, histogram = true)
    public void handleScriptTransactionEventForBlockCacheList(BatchBlocksProcessedEvent blockCacheProcessedEvent) {
        var blockCacheList = blockCacheProcessedEvent.getBlockCaches();

        blockCacheList.stream().parallel().forEach(blockCache -> {
            for (Transaction transaction : blockCache.getTransactions()) {
                handleScriptTransaction(blockCache.getEventMetadata(), transaction);
            }
        });
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
        List<TxScript> txScripts = scriptContexts.stream()
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

                    return TxScript.builder()
                            .txHash(transaction.getTxHash())
                            .slot(metadata.getSlot())
                            .blockNumber(metadata.getBlock())
                            .blockHash(metadata.getBlockHash())
                            .blockTime(metadata.getBlockTime())
                            .scriptHash(scriptContext.getScriptHash())
                            .type(scriptContext.getPlutusScriptType())
                            .redeemer(scriptContext.getRedeemer())
                            .datum(scriptContext.getDatum())
                            .datumHash(scriptContext.getDatumHash())
                            .build();
                }).collect(Collectors.toList());

        //Save redeemer data by data hash in Datum storage
        if (scriptContexts != null && scriptContexts.size() > 0) {
            List<Datum> redeemerDataList = scriptContexts.stream()
                    .filter(scriptContext -> !StringUtil.isEmpty(scriptContext.getRedeemerDataHash()))
                    .map(scriptContext -> new Datum(scriptContext.getRedeemerDataHash(), scriptContext.getRedeemerData(), transaction.getTxHash()))
                    .toList();

            if (redeemerDataList.size() > 0) {
                datumStorage.saveAll(redeemerDataList);
            }
        }

        //Create TxScript entities to save
        List<Script> plutusScripts = scriptsMap.values().stream()
                        .map(plutusScript -> Script.builder()
                                .scriptHash(getPlutusScriptHash(plutusScript))
                                .scriptType(ScriptUtil.toPlutusScriptType(plutusScript.getType()))
                                .content(JsonUtil.getJson(plutusScript))
                                .build()).collect(Collectors.toList());
        if (plutusScripts != null && plutusScripts.size() > 0)
            scriptStorage.saveScripts(plutusScripts);

        //Get all native scripts  and save
        if (transaction.getWitnesses().getNativeScripts() != null) {
            List<Script> nativeScripts = transaction.getWitnesses().getNativeScripts().stream()
                    .map(nativeScript -> Script.builder()
                            .scriptHash(ScriptUtil.getNativeScriptHash(nativeScript))
                            .scriptType(ScriptType.NATIVE_SCRIPT)
                            .content(JsonUtil.getJson(nativeScript))
                            .build()).collect(Collectors.toList());
            if (nativeScripts != null && nativeScripts.size() > 0)
                scriptStorage.saveScripts(nativeScripts);
        }

        if (txScripts.size() > 0) {
            txScriptStorage.saveAll(txScripts);

            //biz event
            publisher.publishEvent(new TxScriptEvent(metadata, txScripts));
        }
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
