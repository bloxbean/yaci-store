package com.bloxbean.cardano.yaci.store.transaction.service;

import com.bloxbean.carano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.transaction.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionDetails;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionPage;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionSummary;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxUtxo;
import com.bloxbean.cardano.yaci.store.transaction.model.TxnEntity;
import com.bloxbean.cardano.yaci.store.transaction.repository.TxnEntityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;
import static java.util.stream.Collectors.groupingBy;

@Component
@RequiredArgsConstructor
public class TransactionService {
    private TxnEntityRepository txnEntityRepository;
    private UtxoClient utxoClient;

    public Optional<TransactionDetails> getTransaction(String txHash) {
        Optional<TxnEntity> txnEntityOptional = txnEntityRepository.findByTxHash(txHash);
        if (txnEntityOptional.isPresent()) {
            return txnEntityOptional.map(txnEntity -> {
                List<TxUtxo> inputUtxos = resolveInputs(txnEntity.getInputs());
                List<TxUtxo> outputUtxos = resolveInputs(txnEntity.getOutputs());
                List<TxUtxo> collateralInputs = resolveInputs(txnEntity.getCollateralInputs());
                List<TxUtxo> referenceInputs = resolveInputs(txnEntity.getReferenceInputs());

                BigInteger totalOutput = outputUtxos
                        .stream()
                        .filter(txUtxo -> txUtxo.getAmounts() != null)
                        .flatMap(txUtxo -> txUtxo.getAmounts().stream())
                        .filter(amount -> LOVELACE.equals(amount.getAssetName()) && !StringUtils.hasLength(amount.getPolicyId()))
                        .map(amount -> amount.getQuantity())
                        .reduce(BigInteger.ZERO, BigInteger::add);

                return TransactionDetails.builder()
                        .hash(txnEntity.getTxHash())
                        .blockHeight(txnEntity.getBlockNumber())
                        .slot(txnEntity.getSlot())
                        .inputs(inputUtxos)
                        .outputs(outputUtxos)
                        .utxoCount(inputUtxos.size())
                        .totalOutput(totalOutput)
                        .fees(txnEntity.getFee())
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

    private TxUtxo resolveInput(UtxoKey utxoId) {
        return utxoClient.getUtxoById(utxoId)
                .map(addressUtxo ->
                        TxUtxo.builder()
                                .txHash(addressUtxo.getTxHash())
                                .outputIndex(addressUtxo.getOutputIndex())
                                .amounts(addressUtxo.getAmounts())
                                .dataHash(addressUtxo.getDataHash())
                                .inlineDatum(addressUtxo.getInlineDatum())
                                .scriptRef(addressUtxo.getScriptRef())
                                .build())
                .orElse(TxUtxo.builder()
                        .txHash(utxoId.getTxHash())
                        .outputIndex(utxoId.getOutputIndex())
                        .build());
    }

    private List<TxUtxo> resolveInputs(List<UtxoKey> utxoIds) {
        if (utxoIds == null || utxoIds.isEmpty())
            return Collections.EMPTY_LIST;

        Map<UtxoKey, List<TxUtxo>> txUtxosMap = utxoClient.getUtxosByIds(utxoIds)
                .stream()
                .map(addressUtxo -> TxUtxo.builder()
                        .txHash(addressUtxo.getTxHash())
                        .outputIndex(addressUtxo.getOutputIndex())
                        .ownerAddr(addressUtxo.getOwnerAddr())
                        .ownerStakeAddr(addressUtxo.getOwnerStakeAddr())
                        .amounts(addressUtxo.getAmounts())
                        .dataHash(addressUtxo.getDataHash())
                        .inlineDatum(addressUtxo.getInlineDatum())
                        .scriptRef(addressUtxo.getScriptRef())
                        .inlineDatumJson(inlineDatumToJson(addressUtxo.getInlineDatum()))
                        .build())
                .collect(groupingBy(txUtxo -> new UtxoKey(txUtxo.getTxHash(), txUtxo.getOutputIndex())));

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

    public TransactionPage getTransactions(int page, int count) {
        Pageable sortedBySlot =
                PageRequest.of(page, count, Sort.by("slot").descending());

        Page<TxnEntity> txnEntityPage = txnEntityRepository.findAll(sortedBySlot);
        long total = txnEntityPage.getTotalElements();
        int totalPage = txnEntityPage.getTotalPages();

        List<TransactionSummary> transactionSummaries = txnEntityPage.stream().map(txnEntity -> {
            List<TxUtxo> outputUtxos = resolveInputs(txnEntity.getOutputs());
            List<String> outputAddresses = outputUtxos.stream()
                    .map(txUtxo -> txUtxo.getOwnerAddr())
                    .collect(Collectors.toList());

            BigInteger totalOutput = outputUtxos.stream()
                    .flatMap(txUtxo -> txUtxo.getAmounts().stream())
                    .filter(amt -> amt.getUnit().equals(LOVELACE))
                    .map(amt -> amt.getQuantity())
                    .reduce((qty1, qty2) -> qty1.add(qty2))
                    .orElse(BigInteger.ZERO);

            TransactionSummary summary = TransactionSummary
                    .builder()
                    .txHash(txnEntity.getTxHash())
                    .blockNumber(txnEntity.getBlockNumber())
                    .slot(txnEntity.getSlot())
                    .outputAddresses(outputAddresses)
                    .totalOutput(totalOutput)
                    .fee(txnEntity.getFee())
                    .build();

            return summary;
        }).collect(Collectors.toList());

        return TransactionPage
                .builder()
                .total(total)
                .totalPages(totalPage)
                .transactionSummaries(transactionSummaries)
                .build();

    }
}
