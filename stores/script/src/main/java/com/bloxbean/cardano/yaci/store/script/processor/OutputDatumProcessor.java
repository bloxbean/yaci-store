package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.events.internal.CommitEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Datum;
import com.bloxbean.cardano.yaci.store.script.domain.DatumEvent;
import com.bloxbean.cardano.yaci.store.script.domain.OutputDatumContext;
import com.bloxbean.cardano.yaci.store.script.domain.WitnessDatumContext;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.getDatumHash;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutputDatumProcessor {
    private final DatumStorage datumStorage;
    private final ApplicationEventPublisher publisher;

    @EventListener
    @Transactional
    public void handleOutputDatumInTransaction(TransactionEvent transactionEvent) {
        for (Transaction transaction : transactionEvent.getTransactions()) {
            handleOutputDatums(transactionEvent.getMetadata(), transaction);
        }
    }

    private void handleOutputDatums(EventMetadata metadata, Transaction transaction) {
        if (transaction.isInvalid())
            return;

        //Get both inline datum / datum hash from outputs
        var outputs = transaction.getBody().getOutputs();
        List<OutputDatumContext> outputDatumContexts = IntStream.range(0, outputs.size())
                .mapToObj(i -> {
                    var output = outputs.get(i);
                    return output.getDatumHash() != null || output.getInlineDatum() != null
                            ? getDatumFromTxOut(output, transaction.getTxHash(), i)
                            : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        //Final map to keep unique datums
        Map<String, Datum> datumHashToDatumMap = new HashMap<>();

        outputDatumContexts.forEach(datumContext -> datumHashToDatumMap.put(datumContext.getDatum().getHash(), datumContext.getDatum()));

        //If datum value exists in witnessset, create a map for such values
        Map<String, WitnessDatumContext> witnessDatumMap = findWitnessDatum(transaction);
        if (witnessDatumMap.size() > 0) {
            witnessDatumMap.forEach((hash, datumContext) -> {
                datumHashToDatumMap.put(hash, datumContext.getDatum());
            });
        }

        Collection<Datum> datumList = datumHashToDatumMap.values();
        //Filter out datum with datum value
        datumList = datumList.stream()
                .filter(datum -> !StringUtil.isEmpty(datum.getDatum()))
                .toList();

        if (datumList.size() > 0) {
            if (log.isTraceEnabled())
                log.trace("Found datumList >> " + datumList.size());

            datumStorage.saveAll(datumList);
        }

        if ( outputDatumContexts.size() > 0 || witnessDatumMap.size() > 0) {
            //biz event
            publisher.publishEvent(new DatumEvent(metadata, outputDatumContexts, witnessDatumMap.values().stream().toList()));
        }
    }

    private static OutputDatumContext getDatumFromTxOut(TransactionOutput txOuput, String txHash, Integer outputIndex) {
        try {
            if (txOuput.getDatumHash() == null && txOuput.getInlineDatum() != null) {
                String datumHash = getDatumHash(txOuput.getInlineDatum());
                var datum = new Datum(datumHash, txOuput.getInlineDatum(), txHash);
                return OutputDatumContext.builder()
                        .txHash(txHash)
                        .outputIndex(outputIndex)
                        .outputAddress(txOuput.getAddress())
                        .datum(datum)
                        .build();
            } else {
                var datum = new Datum(txOuput.getDatumHash(), txOuput.getInlineDatum(), txHash);
                return OutputDatumContext.builder()
                        .txHash(txHash)
                        .outputIndex(outputIndex)
                        .outputAddress(txOuput.getAddress())
                        .datum(datum)
                        .build();
            }
        } catch (Exception e) {
            throw new RuntimeException("Datum parsing error", e);
        }
    }

    private Map<String, WitnessDatumContext> findWitnessDatum(Transaction transaction) {
        if (transaction.getWitnesses().getDatums() == null)
            return Collections.EMPTY_MAP;

        Map<String, WitnessDatumContext> datumHashMap = new HashMap<>();
        transaction.getWitnesses().getDatums()
                .stream()
                .forEach(datum -> {
                    String datumHash = datum.getHash();
                    if (datumHash != null) {
                        var witnessDatumContext = WitnessDatumContext.builder()
                                .txHash(transaction.getTxHash())
                                .datum(new Datum(datumHash, datum.getCbor(), transaction.getTxHash()))
                                .build();

                        datumHashMap.put(datumHash, witnessDatumContext);
                    }
                });

        return datumHashMap;
    }

    @EventListener
    public void handleEvent(CommitEvent commitEvent) {
        datumStorage.handleCommit(commitEvent);
    }
}
