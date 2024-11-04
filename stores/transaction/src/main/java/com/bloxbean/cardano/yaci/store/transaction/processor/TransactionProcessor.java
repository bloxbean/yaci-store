package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.yaci.core.model.BootstrapWitness;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.core.model.VkeyWitness;
import com.bloxbean.cardano.yaci.core.model.Witnesses;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.core.util.Tuple;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.TxOuput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreCommitEvent;
import com.bloxbean.cardano.yaci.store.transaction.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxWitnessType;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.domain.event.TxnEvent;
import com.bloxbean.cardano.yaci.store.transaction.storage.InvalidTransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
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
@Slf4j
public class TransactionProcessor {
    public static final String CHAINCODE = "chaincode";
    public static final String ATTRIBUTES = "attributes";

    private final TransactionStorage transactionStorage;

    private final TransactionWitnessStorage transactionWitnessStorage;
    private final InvalidTransactionStorage invalidTransactionStorage;
    private final ObjectMapper objectMapper;
    private final FeeResolver feeResolver;
    private final ApplicationEventPublisher publisher;

    //To keep invalid transactions in a batch if any to resolve fee
    private List<Tuple<Txn, Transaction>> invalidUnresolvedFeeTxns = Collections.synchronizedList(new ArrayList<>());

    @EventListener
    @Order(3)
    @Transactional
    public void handleTransactionEvent(TransactionEvent event) {
        List<Transaction> transactions = event.getTransactions();
        List<Txn> txList = new ArrayList<>();

        var txIndex = new AtomicInteger(0);
        transactions.forEach(transaction -> {
            List<UtxoKey> inputs = transaction.getBody().getInputs().stream()
                    .map(transactionInput -> new UtxoKey(transactionInput.getTransactionId(), transactionInput.getIndex()))
                    .collect(Collectors.toList());


            AtomicInteger index = new AtomicInteger(0);
            List<UtxoKey> outputs = transaction.getBody().getOutputs().stream()
                    .map(transactionOutput -> new UtxoKey(transaction.getTxHash(), index.getAndIncrement()))
                    .collect(Collectors.toList());
            //reset
            index.set(0);

            List<UtxoKey> collateralInputs = null;
            if (transaction.getBody().getCollateralInputs() != null) {
                collateralInputs = transaction.getBody().getCollateralInputs().stream()
                        .map(transactionInput -> new UtxoKey(transactionInput.getTransactionId(), transactionInput.getIndex()))
                        .collect(Collectors.toList());
            }

            List<UtxoKey> referenceInputs = null;
            if (transaction.getBody().getReferenceInputs() != null) {
                referenceInputs = transaction.getBody().getReferenceInputs().stream()
                        .map(transactionInput -> new UtxoKey(transactionInput.getTransactionId(), transactionInput.getIndex()))
                        .collect(Collectors.toList());
            }

            BigInteger fee = feeResolver.resolveFee(transaction);

            Txn txn = Txn.builder()
                    .txHash(transaction.getTxHash())
                    .blockHash(event.getMetadata().getBlockHash())
                    .blockNumber(transaction.getBlockNumber())
                    .blockTime(event.getMetadata().getBlockTime())
                    .slot(transaction.getSlot())
                    .txIndex(txIndex.getAndIncrement())
                    .epoch(event.getMetadata().getEpochNumber())
                    .inputs(inputs)
                    .outputs(outputs)
                    .fee(fee)
                    .ttl(transaction.getBody().getTtl())
                    .auxiliaryDataHash(transaction.getBody().getAuxiliaryDataHash())
                    .validityIntervalStart(transaction.getBody().getValidityIntervalStart())
                    .scriptDataHash(transaction.getBody().getScriptDataHash())
                    .collateralInputs(collateralInputs)
                    .collateralReturnJson(convertOutput(transaction.getBody().getCollateralReturn()))
                    .netowrkId(transaction.getBody().getNetowrkId())
                    .totalCollateral(transaction.getBody().getTotalCollateral())
                    .collateralReturn(new UtxoKey(transaction.getTxHash(), outputs.size()))
                    .referenceInputs(referenceInputs)
                    .invalid(transaction.isInvalid())
                    .build();

            if (fee == null && transaction.isInvalid()) { //will be resolved in pre-commit event as it can't be resolved now due to parallel processing.
                invalidUnresolvedFeeTxns.add(new Tuple<>(txn, transaction));
            } else {
                txList.add(txn);
            }

            if (transaction.isInvalid())
                saveInvalidTransaction(event.getMetadata(), transaction);
        });

        if (txList.size() > 0) {
            transactionStorage.saveAll(txList);

            //Publish txn event for valid transactions
            publisher.publishEvent(new TxnEvent(event.getMetadata(), txList));
        }

    }

    //Resolve collateral fee for invalid transactions -- Required during parallel processing
    @EventListener
    @Transactional
    public void handleCollateralFee(PreCommitEvent preCommitEvent) {
        if (invalidUnresolvedFeeTxns.isEmpty())
            return;

        try {
            //Handle collateral fee
            for (var invalidTx : invalidUnresolvedFeeTxns) {
                var fee = feeResolver.resolveFee(invalidTx._2);
                if (fee == null) {
                    log.error("Fee not found for transaction. Something is wrong : {}", invalidTx._2.getBody().getTxHash());
                }

                invalidTx._1.setFee(fee);
                transactionStorage.saveAll(List.of(invalidTx._1));
            }

            if (invalidUnresolvedFeeTxns.size() > 0) {
                //Publish txn event for invalid transactions
                publisher.publishEvent(new TxnEvent(preCommitEvent.getMetadata(),
                        invalidUnresolvedFeeTxns.stream().map(t -> t._1).toList()));
            }
        } finally {
            invalidUnresolvedFeeTxns.clear();
        }
    }

    private void saveInvalidTransaction(EventMetadata metadata, Transaction transaction) {
        //insert invalid transactions
        InvalidTransaction invalidTransaction = InvalidTransaction.builder()
                .txHash(transaction.getTxHash())
                .slot(metadata.getSlot())
                .blockHash(metadata.getBlockHash())
                .transaction(transaction)
                .build();
        invalidTransactionStorage.save(invalidTransaction);
    }


    @EventListener
    @Transactional
    public void handleTransactionWitnesses(TransactionEvent event) {
        List<Transaction> transactions = event.getTransactions();
        List<TxnWitness> txnWitnesses = new ArrayList<>();

        int index = 0;
        for(Transaction transaction: transactions) {
            Witnesses witnesses = transaction.getWitnesses();
            if (witnesses == null)
                return;

            var vkeyWitnessList = witnesses.getVkeyWitnesses();
            var bootstrapWitnessList = witnesses.getBootstrapWitnesses();

            if (vkeyWitnessList != null && vkeyWitnessList.size() > 0) {
                for (VkeyWitness vkeyWitness: vkeyWitnessList) {
                    TxnWitness txnWitness = TxnWitness.builder()
                            .txHash(transaction.getTxHash())
                            .index(index++)
                            .pubKey(vkeyWitness.getKey())
                            .signature(vkeyWitness.getSignature())
                            .type(TxWitnessType.VKEY_WITNESS)
                            .slot(event.getMetadata().getSlot())
                            .build();
                    txnWitness.setPubKeyhash(getKeyHash(txnWitness.getPubKey()));
                    txnWitnesses.add(txnWitness);
                }
            }

            if (bootstrapWitnessList != null && bootstrapWitnessList.size() > 0) {
                for (BootstrapWitness bootstrapWitness : bootstrapWitnessList) {
                    TxnWitness txnWitness = TxnWitness.builder()
                            .txHash(transaction.getTxHash())
                            .index(index++)
                            .pubKey(bootstrapWitness.getPublicKey())
                            .signature(bootstrapWitness.getSignature())
                            .type(TxWitnessType.BOOTSTRAP_WITNESS)
                            .slot(event.getMetadata().getSlot())
                            .build();
                    txnWitness.setPubKeyhash(getKeyHash(txnWitness.getPubKey()));

                    ObjectNode objectNode = objectMapper.createObjectNode();
                    objectNode.put(CHAINCODE, bootstrapWitness.getChainCode());
                    objectNode.put(ATTRIBUTES, bootstrapWitness.getAttributes());
                    txnWitness.setAdditionalData(objectNode);

                    txnWitnesses.add(txnWitness);
                }
            }

            if (txnWitnesses.size() > 0)
                transactionWitnessStorage.saveAll(txnWitnesses);
        }
    }

    private String getKeyHash(String pubKey) {
        try {
            return KeyGenUtil.getKeyHash(HexUtil.decodeHexString(pubKey));
        } catch (Exception e) {
            log.error("Error generating keyhash for key : " + pubKey, e);
        }
        return null;
    }


    /**
    private TxResolvedInput resolveInput(String txHash, int outputIndex) {
        return utxoRepository.findById(new UtxoId(txHash, outputIndex))
                .map(addressUtxo ->
                        TxResolvedInput.builder()
                                .txHash(addressUtxo.getTxHash())
                                .outputIndex(addressUtxo.getOutputIndex())
                                .amounts(addressUtxo.getAmounts())
                                .dataHash(addressUtxo.getDataHash())
                                .inlineDatum(addressUtxo.getInlineDatum())
                                .referenceScriptHash(addressUtxo.getReferenceScriptHash())
                                .build())
                .orElse(TxResolvedInput.builder()
                        .txHash(txHash)
                        .outputIndex(outputIndex)
                        .build());
    }

     **/
    private TxOuput convertOutput(TransactionOutput output) {
        if (output == null)
            return null;
        List<Amt> amounts = output.getAmounts().stream().map(amount ->
                        Amt.builder()
                                .unit(amount.getUnit())
                                .policyId(amount.getPolicyId())
                                .assetName(amount.getAssetName().replace('\u0000', ' '))
                                .quantity(amount.getQuantity())
                                .build())
                .collect(Collectors.toList());

        TxOuput txOuput = TxOuput.builder()
                .address(output.getAddress())
                .dataHash(output.getDatumHash())
                .inlineDatum(output.getInlineDatum())
                .referenceScriptHash(output.getScriptRef())
                .amounts(amounts)
                .build();

        return txOuput;
    }

}
