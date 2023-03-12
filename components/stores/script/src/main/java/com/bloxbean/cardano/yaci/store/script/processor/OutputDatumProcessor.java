package com.bloxbean.cardano.yaci.store.script.processor;

import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.script.domain.Datum;
import com.bloxbean.cardano.yaci.store.script.storage.DatumStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.script.helper.ScriptUtil.getDatumHash;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutputDatumProcessor {
    private final DatumStorage datumStorage;

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

        //Extract datum from txOuput
        Set<Datum> datumSet = transaction.getBody()
                .getOutputs().stream()
                .filter(txOutput -> txOutput.getDatumHash() != null || txOutput.getInlineDatum() != null)
                .map(txOutput -> getDatumFromTxOut(txOutput, transaction.getTxHash()))
                .collect(Collectors.toSet());

        Map<String, Datum> datumHashToDatumMap = new HashMap<>();
        datumSet.forEach(datum -> datumHashToDatumMap.put(datum.getHash(), datum));

        //If datum value exists in witnessset, create a map for such values
        Map<String, Datum> witnessDatumMap = findWitnessDatum(transaction);
        if (witnessDatumMap.size() > 0) {
            witnessDatumMap.forEach((hash, datum) -> {
                datumHashToDatumMap.put(hash, datum);
            });
        }

        Collection<Datum> datumList = datumHashToDatumMap.values();
        if (datumList.size() > 0) {
            if (log.isTraceEnabled())
                log.trace("Found datumList >> " + datumList.size());

            datumStorage.saveAll(datumList);
        }
    }

    private static Datum getDatumFromTxOut(TransactionOutput txOuput, String txHash) {
        try {
            if (txOuput.getDatumHash() == null && txOuput.getInlineDatum() != null) {
                String datumHash = getDatumHash(txOuput.getInlineDatum());
                return new Datum(datumHash, txOuput.getInlineDatum(), txHash);
            } else
                return new Datum(txOuput.getDatumHash(), txOuput.getInlineDatum(), txHash);
        } catch (Exception e) {
            throw new RuntimeException("Datum parsing error", e);
        }
    }

    private Map<String, Datum> findWitnessDatum(Transaction transaction) {
        if (transaction.getWitnesses().getDatums() == null)
            return Collections.EMPTY_MAP;

        Map<String, Datum> datumHashMap = new HashMap<>();
        transaction.getWitnesses().getDatums()
                .stream()
                .forEach(datum -> {
                    String datumHash = getDatumHash(datum);
                    if (datumHash != null)
                        datumHashMap.put(datumHash, new Datum(datumHash, datum.getCbor(), transaction.getTxHash()));
                });

        return datumHashMap;
    }
}
