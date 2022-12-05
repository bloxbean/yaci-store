package com.bloxbean.cardano.yaci.indexer.transaction.processor;

import com.bloxbean.carano.yaci.indexer.common.model.Amt;
import com.bloxbean.carano.yaci.indexer.common.model.TxOuput;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import com.bloxbean.cardano.yaci.helper.model.Transaction;
import com.bloxbean.cardano.yaci.indexer.events.TransactionEvent;
import com.bloxbean.cardano.yaci.indexer.transaction.entity.TxnEntity;
import com.bloxbean.cardano.yaci.indexer.transaction.repository.TxnEntityRepository;
import com.bloxbean.cardano.yaci.indexer.utxo.entity.UtxoId;
import com.bloxbean.cardano.yaci.indexer.utxo.repository.UtxoRepository;
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

    private final TxnEntityRepository txnEntityRepository;
    private final UtxoRepository utxoRepository;

    @EventListener
    @Order(3)
    @Transactional
    public void handleTransactionEvent(TransactionEvent event) {
        List<Transaction> transactions = event.getTransactions();
        List<TxnEntity> txnEntities = new ArrayList<>();

        transactions.forEach(transaction -> {
            List<UtxoId> inputs = transaction.getBody().getInputs().stream()
                    .map(transactionInput -> new UtxoId(transactionInput.getTransactionId(), transactionInput.getIndex()))
                    .collect(Collectors.toList());


            AtomicInteger index = new AtomicInteger(0);
            List<UtxoId> outputs = transaction.getBody().getOutputs().stream()
                    .map(transactionOutput -> new UtxoId(transaction.getTxHash(), index.getAndIncrement()))
                    .collect(Collectors.toList());
            //reset
            index.set(0);

            List<UtxoId> collateralInputs = null;
            if (transaction.getBody().getCollateralInputs() != null) {
                collateralInputs = transaction.getBody().getCollateralInputs().stream()
                        .map(transactionInput -> new UtxoId(transactionInput.getTransactionId(), transactionInput.getIndex()))
                        .collect(Collectors.toList());
            }

            List<UtxoId> referenceInputs = null;
            if (transaction.getBody().getReferenceInputs() != null) {
                referenceInputs = transaction.getBody().getReferenceInputs().stream()
                        .map(transactionInput -> new UtxoId(transactionInput.getTransactionId(), transactionInput.getIndex()))
                        .collect(Collectors.toList());
            }

            TxnEntity txnEntity = TxnEntity.builder()
                    .txHash(transaction.getTxHash())
                    .blockHash(event.getMetadata().getBlockHash())
                    .blockNumber(transaction.getBlockNumber())
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
                    .collateralReturn(new UtxoId(transaction.getTxHash(), outputs.size()))
                    .referenceInputs(referenceInputs)
                    .invalid(transaction.isInvalid())
                    .build();

            txnEntities.add(txnEntity);
        });

        txnEntityRepository.saveAll(txnEntities);
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
