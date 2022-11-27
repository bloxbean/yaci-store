package com.bloxbean.cardano.yaci.indexer.transaction.service;

import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.transaction.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.indexer.transaction.entity.TxnEntity;
import com.bloxbean.cardano.yaci.indexer.transaction.model.TransactionDetails;
import com.bloxbean.cardano.yaci.indexer.transaction.model.TxUtxo;
import com.bloxbean.cardano.yaci.indexer.transaction.repository.TxnEntityRepository;
import com.bloxbean.cardano.yaci.indexer.utxo.entity.UtxoId;
import com.bloxbean.cardano.yaci.indexer.utxo.repository.UtxoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Component
public class TransactionService {
    private TxnEntityRepository txnEntityRepository;
    private UtxoRepository utxoRepository;

    public TransactionService(TxnEntityRepository txnEntityRepository, UtxoRepository utxoRepository) {
        this.txnEntityRepository = txnEntityRepository;
        this.utxoRepository = utxoRepository;
    }

    public Optional<TransactionDetails> getTransaction(String txHash) {
        Optional<TxnEntity> txnEntityOptional = txnEntityRepository.findByTxHash(txHash);
        if (txnEntityOptional.isPresent()) {
            return txnEntityOptional.map(txnEntity -> {
                List<TxUtxo> inputUtxos = resolveInputs(txnEntity.getInputs());
                List<TxUtxo> outputUtxos = resolveInputs(txnEntity.getOutputs());
                List<TxUtxo> collateralInputs = resolveInputs(txnEntity.getCollateralInputs());
                List<TxUtxo> referenceInputs = resolveInputs(txnEntity.getReferenceInputs());

                return TransactionDetails.builder()
                        .txHash(txnEntity.getTxHash())
                        .blockNumber(txnEntity.getBlockNumber())
                        .slot(txnEntity.getSlot())
                        .inputs(inputUtxos)
                        .outputs(outputUtxos)
                        .fee(txnEntity.getFee())
                        .ttl(txnEntity.getTtl())
                        .auxiliaryDataHash(txnEntity.getAuxiliaryDataHash())
                        .validityIntervalStart(txnEntity.getValidityIntervalStart())
                        .scriptDataHash(txnEntity.getScriptDataHash())
                        .collateralInputs(collateralInputs)
                        .requiredSigners(txnEntity.getRequiredSigners())
                        .netowrkId(txnEntity.getNetowrkId())
                        .collateralReturn(txnEntity.getCollateralReturnJson())
                        .totalCollateral(txnEntity.getTotalCollateral())
                        .referenceInputs(referenceInputs)
                        .build();
            });

        } else {
            return Optional.empty();
        }
    }

    private TxUtxo resolveInput(UtxoId utxoId) {
        return utxoRepository.findById(utxoId)
                .map(addressUtxo ->
                        TxUtxo.builder()
                                .txHash(addressUtxo.getTxHash())
                                .outputIndex(addressUtxo.getOutputIndex())
                                .amounts(addressUtxo.getAmounts())
                                .dataHash(addressUtxo.getDataHash())
                                .inlineDatum(addressUtxo.getInlineDatum())
                                .referenceScriptHash(addressUtxo.getReferenceScriptHash())
                                .build())
                .orElse(TxUtxo.builder()
                        .txHash(utxoId.getTxHash())
                        .outputIndex(utxoId.getOutputIndex())
                        .build());
    }

    private List<TxUtxo> resolveInputs(List<UtxoId> utxoIds) {
        if (utxoIds == null || utxoIds.isEmpty())
            return Collections.EMPTY_LIST;

        Map<UtxoId, List<TxUtxo>> txUtxosMap = utxoRepository.findAllById(utxoIds)
                .stream()
                .map(addressUtxo -> TxUtxo.builder()
                        .txHash(addressUtxo.getTxHash())
                        .outputIndex(addressUtxo.getOutputIndex())
                        .ownerAddr(addressUtxo.getOwnerAddr())
                        .ownerStakeAddr(addressUtxo.getOwnerStakeAddr())
                        .amounts(addressUtxo.getAmounts())
                        .dataHash(addressUtxo.getDataHash())
                        .inlineDatum(addressUtxo.getInlineDatum())
                        .referenceScriptHash(addressUtxo.getReferenceScriptHash())
                        .inlineDatumJson(inlineDatumToJson(addressUtxo.getInlineDatum()))
                        .build())
                .collect(groupingBy(txUtxo -> new UtxoId(txUtxo.getTxHash(), txUtxo.getOutputIndex())));

        return utxoIds.stream()
                .map(utxoId -> txUtxosMap.containsKey(utxoId) ? txUtxosMap.get(utxoId).get(0)
                        : TxUtxo.builder()
                        .txHash(utxoId.getTxHash())
                        .outputIndex(utxoId.getOutputIndex())
                        .build())
                .collect(Collectors.toList());
    }

    private JsonNode inlineDatumToJson(String inlineDatum) {
        try {
            PlutusData plutusData = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));
            return JsonUtil.parseJson(PlutusDataJsonConverter.toJson(plutusData));
        } catch (Exception e) {
            return null;
        }
    }
}
