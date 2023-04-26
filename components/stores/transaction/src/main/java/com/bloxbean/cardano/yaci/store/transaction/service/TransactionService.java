package com.bloxbean.cardano.yaci.store.transaction.service;

import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.transaction.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.yaci.store.client.utxo.UtxoClient;
import com.bloxbean.cardano.yaci.store.common.domain.UtxoKey;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.transaction.domain.*;
import com.bloxbean.cardano.yaci.store.transaction.storage.api.TransactionStorage;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
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
    private final TransactionStorage transactionStorage;
    private final UtxoClient utxoClient;

    public Optional<TransactionDetails> getTransaction(String txHash) {
        Optional<Txn> txnOptional = transactionStorage.getTransactionByTxHash(txHash);
        if (txnOptional.isPresent()) {
            return txnOptional.map(txn -> {
                List<TxUtxo> inputUtxos = resolveInputs(txn.getInputs());
                List<TxUtxo> outputUtxos = resolveInputs(txn.getOutputs());
                List<TxUtxo> collateralInputs = resolveInputs(txn.getCollateralInputs());
                List<TxUtxo> referenceInputs = resolveInputs(txn.getReferenceInputs());

                BigInteger totalOutput = outputUtxos
                        .stream()
                        .filter(txUtxo -> txUtxo.getAmounts() != null)
                        .flatMap(txUtxo -> txUtxo.getAmounts().stream())
                        .filter(amount -> LOVELACE.equals(amount.getAssetName()) && !StringUtils.hasLength(amount.getPolicyId()))
                        .map(amount -> amount.getQuantity())
                        .reduce(BigInteger.ZERO, BigInteger::add);

                return TransactionDetails.builder()
                        .hash(txn.getTxHash())
                        .blockHeight(txn.getBlockNumber())
                        .slot(txn.getSlot())
                        .inputs(inputUtxos)
                        .outputs(outputUtxos)
                        .utxoCount(inputUtxos.size())
                        .totalOutput(totalOutput)
                        .fees(txn.getFee())
                        .ttl(txn.getTtl())
                        .auxiliaryDataHash(txn.getAuxiliaryDataHash())
                        .validityIntervalStart(txn.getValidityIntervalStart())
                        .scriptDataHash(txn.getScriptDataHash())
                        .collateralInputs(collateralInputs)
                        .requiredSigners(txn.getRequiredSigners())
                        .netowrkId(txn.getNetowrkId())
                        .collateralReturn(txn.getCollateralReturnJson())
                        .totalCollateral(txn.getTotalCollateral())
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
        List<Txn> txnPage = transactionStorage.getTransactions(page, count, Order.desc);
        //TODO -- Find total and totalPage in TransactionPage. Currently disabled as count query takes too long
//        long total = txnPage.getTotalElements();
//        int totalPage = txnPage.getTotalPages();

        List<TransactionSummary> transactionSummaries = txnPage.stream().map(txn -> {
            List<TxUtxo> outputUtxos = resolveInputs(txn.getOutputs());
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
                    .txHash(txn.getTxHash())
                    .blockNumber(txn.getBlockNumber())
                    .slot(txn.getSlot())
                    .outputAddresses(outputAddresses)
                    .totalOutput(totalOutput)
                    .fee(txn.getFee())
                    .build();

            return summary;
        }).collect(Collectors.toList());

        return TransactionPage
                .builder()
//                .total(total)
//                .totalPages(totalPage)
                .transactionSummaries(transactionSummaries)
                .build();

    }
}
