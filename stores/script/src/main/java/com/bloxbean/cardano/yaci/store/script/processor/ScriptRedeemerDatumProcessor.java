package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.yaci.core.model.PlutusScript;
import com.bloxbean.cardano.yaci.core.model.Redeemer;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.events.internal.BatchBlocksProcessedEvent;
import com.bloxbean.cardano.yaci.store.events.model.internal.BatchBlock;
import com.bloxbean.cardano.yaci.store.script.domain.*;
import com.bloxbean.cardano.yaci.store.script.helper.RedeemerDatumMatcher;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptContext;
import com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil;
import com.bloxbean.cardano.yaci.store.script.helper.TxScriptFinder;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import com.bloxbean.cardano.yaci.store.script.storage.ScriptStorage;
import com.bloxbean.cardano.yaci.store.script.storage.TxScriptStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.common.util.ListUtil.partition;
import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.getDatumHash;
import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.getPlutusScriptHash;

@Component
@Slf4j
public class ScriptRedeemerDatumProcessor {
    private final TxScriptStorage txScriptStorage;
    private final ScriptStorage scriptStorage;
    private final DatumStorage datumStorage;
    private final RedeemerDatumMatcher redeemerMatcher;
    private final TxScriptFinder txScriptFinder;
    private final ApplicationEventPublisher publisher;

    @Value("${store.executor.blocks-partition-size:10}")
    private int blockBatchPartitionSize;

    private Executor executor;

    public ScriptRedeemerDatumProcessor(TxScriptStorage txScriptStorage, ScriptStorage scriptStorage, DatumStorage datumStorage,
                                        RedeemerDatumMatcher redeemerMatcher, TxScriptFinder txScriptFinder, ApplicationEventPublisher publisher) {
        this.txScriptStorage = txScriptStorage;
        this.scriptStorage = scriptStorage;
        this.datumStorage = datumStorage;
        this.redeemerMatcher = redeemerMatcher;
        this.txScriptFinder = txScriptFinder;
        this.publisher = publisher;

        init();
    }

    public void init() {
        executor = Executors.newVirtualThreadPerTaskExecutor();
    }

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
    public void handleScriptTransactionEventForBlockCacheList(BatchBlocksProcessedEvent blockCacheProcessedEvent) {
        var blockCacheList = blockCacheProcessedEvent.getBlockCaches();
        List<List<BatchBlock>> partitions = partition(blockCacheList, blockBatchPartitionSize);
        List<CompletableFuture> futures = new ArrayList<>();
        for (List<BatchBlock> partition : partitions) {
            var future = CompletableFuture.supplyAsync(() -> {
                processBlockPartition(partition);
                return true;
            }, executor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();
    }

    private void processBlockPartition(List<BatchBlock> blockCacheList) {
        blockCacheList.stream().parallel().forEach(blockCache -> {
            for (Transaction transaction : blockCache.getTransactions()) {
                handleScriptTransaction(blockCache.getMetadata(), transaction);
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

                    Redeemer redeemer;
                    if (scriptContext.getRedeemer() != null)
                        redeemer = scriptContext.getRedeemer();
                    else
                        redeemer = new Redeemer();

                    return TxScript.builder()
                            .txHash(transaction.getTxHash())
                            .slot(metadata.getSlot())
                            .blockNumber(metadata.getBlock())
                            .blockHash(metadata.getBlockHash())
                            .blockTime(metadata.getBlockTime())
                            .scriptHash(scriptContext.getScriptHash())
                            .type(scriptContext.getPlutusScriptType())
                            .datum(scriptContext.getDatum())
                            .datumHash(scriptContext.getDatumHash())
                            .redeemerCbor(redeemer.getCbor())
                            .unitMem(redeemer.getExUnits() != null? redeemer.getExUnits().getMem(): null)
                            .unitSteps(redeemer.getExUnits() != null? redeemer.getExUnits().getSteps(): null)
                            .purpose(redeemer.getTag())
                            .redeemerIndex(redeemer.getIndex())
                            .redeemerData(redeemer.getData() != null? redeemer.getData().getCbor(): null)
                            .redeemerDatahash(redeemer.getData() != null? redeemer.getData().getHash(): null)
                            .build();
                }).collect(Collectors.toList());

        //Save redeemer data by data hash in Datum storage
        if (scriptContexts != null && scriptContexts.size() > 0) {
            List<Datum> redeemerDataList = scriptContexts.stream()
                    .map(scriptContext -> scriptContext.getRedeemer())
                    .filter(redeemer -> redeemer != null)
                    .map(redeemer -> new Datum(redeemer.getData().getHash(), redeemer.getData().getCbor(), transaction.getTxHash()))
                    .toList();

            if (redeemerDataList.size() > 0) {
                datumStorage.saveAll(redeemerDataList);
            }
        }

        //Create TxScript entities to save
        List<Script> plutusScripts = scriptsMap.values().stream()
                        .map(plutusScript -> {
                            String scriptHash;
                            try {
                                scriptHash = ScriptUtil.getPlutusScriptHash(plutusScript);
                            } catch (Exception e) {
                                log.error("Error getting native script hash. Block hash: " + metadata.getBlockHash(), e);
                                return null;
                            }
                            return Script.builder()
                                    .scriptHash(scriptHash)
                                    .scriptType(ScriptUtil.toPlutusScriptType(plutusScript.getType()))
                                    .content(JsonUtil.getJson(plutusScript))
                                    .build();
                            }
                        )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (plutusScripts != null && plutusScripts.size() > 0)
            scriptStorage.saveScripts(plutusScripts);

        //Get all native scripts  and save
        if (transaction.getWitnesses().getNativeScripts() != null) {
            List<Script> nativeScripts = transaction.getWitnesses().getNativeScripts().stream()
                    .map(nativeScript -> {
                        String scriptHash;
                        try {
                            scriptHash = ScriptUtil.getNativeScriptHash(nativeScript);
                        } catch (Exception e) {
                            log.error("Error getting native script hash. Block hash: " + metadata.getBlockHash(), e);
                            return null;
                        }
                        return Script.builder()
                                .scriptHash(scriptHash)
                                .scriptType(ScriptType.NATIVE_SCRIPT)
                                .content(JsonUtil.getJson(nativeScript))
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
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
