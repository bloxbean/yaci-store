package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.yaci.core.model.byron.ByronTx;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.storage.api.TransactionStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ByronTransactionProcessor {

    private final TransactionStorage transactionStorage;

    @EventListener
    @Transactional
    public void handleByronTransactionEvent(ByronMainBlockEvent event) {
        List<ByronTx> byronTxList = event.getByronMainBlock().getBody().getTxPayload()
                .stream()
                .map(byronTxPayload -> byronTxPayload.getTransaction())
                .collect(Collectors.toList());

        List<Txn> txList = new ArrayList<>();
        for (ByronTx byronTx : byronTxList) {

            //find inputs
            List<UtxoKey> inputs = byronTx.getInputs().stream()
                    .map(txIn -> new UtxoKey(txIn.getTxId(), txIn.getIndex()))
                    .collect(Collectors.toList());

            //find outputs
            AtomicInteger index = new AtomicInteger(0);
            List<UtxoKey> outputs = byronTx.getOutputs().stream()
                    .map(txOut -> new UtxoKey(byronTx.getTxHash(), index.getAndIncrement()))
                    .collect(Collectors.toList());
            index.set(0);

            //build Tx
            Txn txn = Txn.builder()
                    .txHash(byronTx.getTxHash())
                    .blockHash(event.getEventMetadata().getBlockHash())
                    .blockNumber(event.getEventMetadata().getBlock())
                    .slot(event.getEventMetadata().getSlot())
                    .inputs(inputs)
                    .outputs(outputs)
//TODO                    .fee()
                    .build();

            txList.add(txn);
        }

        if (txList.size() > 0) {
            transactionStorage.saveAll(txList);
        }
    }

}
