package com.bloxbean.cardano.yaci.store.transaction.processor;

import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.yaci.core.model.BootstrapWitness;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.core.model.VkeyWitness;
import com.bloxbean.cardano.yaci.core.model.Witnesses;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.domain.TxOuput;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.TransactionEvent;
import com.bloxbean.cardano.yaci.store.transaction.domain.InvalidTransaction;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxWitnessType;
import com.bloxbean.cardano.yaci.store.transaction.domain.Txn;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.storage.InvalidTransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionStorage;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionWitnessStorage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @EventListener
    @Order(3)
    @Transactional
    public void handleTransactionEvent(TransactionEvent event) {
        List<Transaction> transactions = event.getTransactions();
        List<Txn> txList = new ArrayList<>();

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

            Txn txn = Txn.builder()
                    .txHash(transaction.getTxHash())
                    .blockHash(event.getMetadata().getBlockHash())
                    .blockNumber(transaction.getBlockNumber())
                    .blockTime(event.getMetadata().getBlockTime())
                    .slot(transaction.getSlot())
                    .inputs(inputs)
                    .outputs(outputs)
                    .fee(transaction.getBody().getFee())
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
                    .size(transaction.getTxSize())
                    .scriptSize(transaction.getTxScriptSize())
                    .build();

            txList.add(txn);

            if (transaction.isInvalid())
                saveInvalidTransaction(event.getMetadata(), transaction);
        });

        if (txList.size() > 0) {
            transactionStorage.saveAll(txList);
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
