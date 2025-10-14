package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.yaci.core.model.BootstrapWitness;
import com.bloxbean.cardano.yaci.core.model.VkeyWitness;
import com.bloxbean.cardano.yaci.core.model.byron.*;
import com.bloxbean.cardano.yaci.core.model.byron.payload.ByronTxPayload;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.aspect.EnableIf;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.events.ByronMainBlockEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreConfiguration;
import com.bloxbean.cardano.yaci.store.transaction.TransactionStoreProperties;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxWitnessType;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.domain.event.TxnEvent;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@EnableIf(TransactionStoreConfiguration.STORE_TRANSACTION_ENABLED)
@Slf4j
public class ByronTransactionProcessor {

    private final TransactionStorage transactionStorage;
    private final TransactionWitnessStorage transactionWitnessStorage;
    private final UtxoClient utxoClient;
    private final ApplicationEventPublisher publisher;
    private final TransactionStoreProperties transactionStoreProperties;

    //To keep invalid transactions in a batch if any to resolve fee
    private List<Tuple<Txn, ByronTx>> unresolvedFeeTxns = Collections.synchronizedList(new ArrayList<>());

    @EventListener
    @Transactional
    public void handleByronTransactionEvent(ByronMainBlockEvent event) {
        List<ByronTx> byronTxList = event.getByronMainBlock().getBody().getTxPayload()
                .stream()
                .map(byronTxPayload -> byronTxPayload.getTransaction())
                .collect(Collectors.toList());

        List<Txn> txList = new ArrayList<>();
        int txIndex = 0;
        for (ByronTx byronTx : byronTxList) {

            //find inputs
            List<UtxoKey> inputs = byronTx.getInputs().stream()
                    .map(txIn -> new UtxoKey(txIn.getTxId(), txIn.getIndex()))
                    .collect(Collectors.toList());

            //Calculate fee
            BigInteger fee = calculateFee(byronTx);

            //find outputs
            AtomicInteger index = new AtomicInteger(0);
            List<UtxoKey> outputs = byronTx.getOutputs().stream()
                    .map(txOut -> new UtxoKey(byronTx.getTxHash(), index.getAndIncrement()))
                    .collect(Collectors.toList());
            index.set(0);

            //build Tx
            Txn txn = Txn.builder()
                    .txHash(byronTx.getTxHash())
                    .blockHash(event.getMetadata().getBlockHash())
                    .blockNumber(event.getMetadata().getBlock())
                    .blockTime(event.getMetadata().getBlockTime())
                    .slot(event.getMetadata().getSlot())
                    .txIndex(txIndex++)
                    .epoch(event.getMetadata().getEpochNumber())
                    .inputs(inputs)
                    .outputs(outputs)
                    .fee(fee)
                    .build();

            if (fee == null) { //fee is unresolved due to parallel processing. Add to unresolved list
                unresolvedFeeTxns.add(new Tuple<>(txn, byronTx));
            } else {
                txList.add(txn);
            }
        }

        if (txList.size() > 0) {
            transactionStorage.saveAll(txList);

            //Publish txn event for valid transactions
            publisher.publishEvent(new TxnEvent(event.getMetadata(), txList));
        }
    }

    private BigInteger calculateFee(ByronTx byronTx) {
        BigInteger fee = null;

        List<UtxoKey> inputs = byronTx.getInputs().stream()
                .map(txIn -> new UtxoKey(txIn.getTxId(), txIn.getIndex()))
                .collect(Collectors.toList());

        List<AddressUtxo> inputUtxos = utxoClient.getUtxosByIds(inputs);
        if(inputUtxos.size() != inputs.size()) {
            if (log.isDebugEnabled())
                log.debug("Input utxos not found for transaction : " + byronTx.getTxHash());
        } else {
            var totalInputs = inputUtxos.stream()
                    .map(AddressUtxo::getLovelaceAmount)
                    .reduce(BigInteger::add).orElse(BigInteger.ZERO);

            var totalOutputs = byronTx.getOutputs().stream()
                    .map(ByronTxOut::getAmount)
                    .reduce(BigInteger::add).orElse(BigInteger.ZERO);
            fee = totalInputs.subtract(totalOutputs);
        }

        return fee;
    }

    //Resolve fee for unresolved fee transactions
    @EventListener
    @Transactional
    public void handleUnresolvedFee(PreCommitEvent preCommitEvent) {
        if (unresolvedFeeTxns.isEmpty())
            return;

        List<Txn> txnList = new ArrayList<>();
        try {
            //Handle collateral fee
            for (var unresolvedTxs : unresolvedFeeTxns) {
                var fee = calculateFee(unresolvedTxs._2);

                if (fee != null) {
                    unresolvedTxs._1.setFee(fee);
                } else {
                    unresolvedTxs._1.setFee(BigInteger.ZERO);
                    log.warn("Fee not resolved for transaction : " + unresolvedTxs._1.getTxHash());
                }

                txnList.add(unresolvedTxs._1);
            }

            if (txnList.size() > 0) {
                transactionStorage.saveAll(txnList);

                publisher.publishEvent(new TxnEvent(preCommitEvent.getMetadata(),
                        txnList));
            }
        } finally {
            unresolvedFeeTxns.clear();
        }
    }


    @EventListener
    @Transactional
    public void handleByronTransactionWitnesses(ByronMainBlockEvent event) {
        if (!transactionStoreProperties.isSaveWitness()) {
            return;
        }

        var byronTxPayloadsList = event.getByronMainBlock().getBody().getTxPayload()
                .stream()
                .toList();

        List<TxnWitness> txWitnesses = new ArrayList<>();
        for (ByronTxPayload byronTxPayload : byronTxPayloadsList) {
            var witnesses = byronTxPayload.getWitnesses();
            if (witnesses == null || witnesses.size() == 0)
                continue;

            int index = 0;
            for (var witness : witnesses) {
                if (witness == null)
                    continue;

                TxnWitness txnWitness = new TxnWitness();
                txnWitness.setTxHash(byronTxPayload.getTransaction().getTxHash());
                txnWitness.setIndex(index++);
                txnWitness.setSlot(event.getMetadata().getSlot());

                switch (witness) {
                    case VkeyWitness vkeyWitness -> {
                        txnWitness.setPubKey(vkeyWitness.getKey());
                        txnWitness.setSignature(vkeyWitness.getSignature());
                        txnWitness.setType(TxWitnessType.VKEY_WITNESS);
                    }
                    case BootstrapWitness bootstrapWitness -> {
                        txnWitness.setPubKey(bootstrapWitness.getPublicKey());
                        txnWitness.setSignature(bootstrapWitness.getSignature());
                        txnWitness.setType(TxWitnessType.BOOTSTRAP_WITNESS);
                    }
                    case ByronPkWitness byronPkWitness -> {
                        txnWitness.setPubKey(byronPkWitness.getPublicKey());
                        txnWitness.setSignature(byronPkWitness.getSignature());
                        txnWitness.setType(TxWitnessType.BYRON_PK_WITNESS);
                    }
                    case ByronRedeemWitness byronRedeemWitness -> {
                        txnWitness.setPubKey(byronRedeemWitness.getRedeemPublicKey());
                        txnWitness.setSignature(byronRedeemWitness.getRedeemSignature());
                        txnWitness.setType(TxWitnessType.BYRON_REDEEM_WITNESS);
                    }
                    case ByronScriptWitness scriptWitness -> {
                        log.warn("ByronScriptWitness found ----> Not sure how to handle this >>>>>>>>>>>>>>>");
                        txnWitness.setType(TxWitnessType.BYRON_SCRIPT_WITNESS);
                    }
                    case ByronUnknownWitness unknownWitness -> {
                        log.warn("ByronUnkownWitness found --> Not sure how to handle this >>>>>>>>>>>>>>>");
                        txnWitness.setType(TxWitnessType.BYRON_UNKNOWN_WITNESS);
                    }
                    default -> log.error("Invalid witness type : " + witness);
                }

                try {
                    if (txnWitness.getPubKey() != null && !StringUtil.isEmpty(txnWitness.getPubKey())) {
                        txnWitness.setPubKeyhash(KeyGenUtil.getKeyHash(HexUtil.decodeHexString(txnWitness.getPubKey())));
                    }
                } catch (Exception e) {
                    log.error("Error generating keyhash for key : " + txnWitness.getPubKey(), e);
                }

                txWitnesses.add(txnWitness);

            }
        }

        if (txWitnesses.size() > 0)
            transactionWitnessStorage.saveAll(txWitnesses);
    }

}
