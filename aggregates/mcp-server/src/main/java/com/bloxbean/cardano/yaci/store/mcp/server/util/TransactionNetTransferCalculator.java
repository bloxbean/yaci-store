package com.bloxbean.cardano.yaci.store.mcp.server.util;

import com.bloxbean.cardano.yaci.store.mcp.server.model.TransactionNetTransfer;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TransactionNetTransfer.AssetNetChange;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TransactionNetTransfer.NetTransferPerAddress;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TransactionNetTransfer.TransferSummary;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionDetails;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxUtxo;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class to calculate net transfers in a transaction.
 *
 * Solves the UTXO chain problem where tokens appear in both inputs and outputs:
 * - Aggregates all inputs per address
 * - Aggregates all outputs per address
 * - Calculates net difference (outputs - inputs)
 * - Identifies actual senders (negative net) and receivers (positive net)
 */
public class TransactionNetTransferCalculator {

    /**
     * Calculate net transfers for a transaction
     */
    public static TransactionNetTransfer calculate(TransactionDetails txDetails) {
        if (txDetails == null) {
            throw new IllegalArgumentException("Transaction details cannot be null");
        }

        // Track inputs per address
        Map<String, AddressAmounts> inputsByAddress = aggregateUtxosByAddress(txDetails.getInputs());

        // Track outputs per address
        Map<String, AddressAmounts> outputsByAddress = aggregateUtxosByAddress(txDetails.getOutputs());

        // Get all unique addresses involved
        Set<String> allAddresses = new HashSet<>();
        allAddresses.addAll(inputsByAddress.keySet());
        allAddresses.addAll(outputsByAddress.keySet());

        // Calculate net transfers per address
        Map<String, NetTransferPerAddress> netTransfers = new HashMap<>();
        List<String> senders = new ArrayList<>();
        List<String> receivers = new ArrayList<>();
        Set<String> allAssetUnits = new HashSet<>();

        for (String address : allAddresses) {
            AddressAmounts inputs = inputsByAddress.getOrDefault(address, new AddressAmounts());
            AddressAmounts outputs = outputsByAddress.getOrDefault(address, new AddressAmounts());

            // Calculate net lovelace
            BigInteger netLovelace = outputs.lovelace.subtract(inputs.lovelace);

            // Calculate net for each asset
            Set<String> assetUnits = new HashSet<>();
            assetUnits.addAll(inputs.assets.keySet());
            assetUnits.addAll(outputs.assets.keySet());
            allAssetUnits.addAll(assetUnits);

            Map<String, AssetNetChange> netAssets = new HashMap<>();
            for (String unit : assetUnits) {
                AssetAmount inputAsset = inputs.assets.getOrDefault(unit, new AssetAmount());
                AssetAmount outputAsset = outputs.assets.getOrDefault(unit, new AssetAmount());

                BigInteger netQuantity = outputAsset.quantity.subtract(inputAsset.quantity);

                // Only include if there's a net change
                if (netQuantity.compareTo(BigInteger.ZERO) != 0) {
                    netAssets.put(unit, AssetNetChange.builder()
                            .unit(unit)
                            .policyId(outputAsset.policyId != null ? outputAsset.policyId : inputAsset.policyId)
                            .assetName(outputAsset.assetName != null ? outputAsset.assetName : inputAsset.assetName)
                            .netQuantity(netQuantity)
                            .totalInputQuantity(inputAsset.quantity)
                            .totalOutputQuantity(outputAsset.quantity)
                            .build());
                }
            }

            // Determine if sender or receiver
            boolean isSender = netLovelace.compareTo(BigInteger.ZERO) < 0;
            boolean isReceiver = netLovelace.compareTo(BigInteger.ZERO) > 0;

            if (isSender) {
                senders.add(address);
            }
            if (isReceiver) {
                receivers.add(address);
            }

            // Only include addresses with actual net changes
            if (netLovelace.compareTo(BigInteger.ZERO) != 0 || !netAssets.isEmpty()) {
                netTransfers.put(address, NetTransferPerAddress.builder()
                        .address(address)
                        .stakeAddress(outputs.stakeAddress != null ? outputs.stakeAddress : inputs.stakeAddress)
                        .netLovelace(netLovelace)
                        .netAssets(netAssets)
                        .isSender(isSender)
                        .isReceiver(isReceiver)
                        .totalInputs(inputs.lovelace)
                        .totalOutputs(outputs.lovelace)
                        .build());
            }
        }

        // Calculate summary statistics
        BigInteger totalAdaMoved = netTransfers.values().stream()
                .map(NetTransferPerAddress::getNetLovelace)
                .map(BigInteger::abs)
                .reduce(BigInteger.ZERO, BigInteger::add)
                .divide(BigInteger.TWO); // Divide by 2 since we count both sides

        boolean isSimpleTransfer = senders.size() == 1 && receivers.size() == 1;

        // Check for script interaction (basic heuristic)
        boolean hasScriptInteraction = txDetails.getInputs() != null &&
                txDetails.getInputs().stream().anyMatch(utxo ->
                    utxo.getDataHash() != null || utxo.getInlineDatum() != null || utxo.getScriptRef() != null);

        TransferSummary summary = TransferSummary.builder()
                .totalAddresses(allAddresses.size())
                .senderCount(senders.size())
                .receiverCount(receivers.size())
                .assetTypesCount(allAssetUnits.size())
                .totalAdaMoved(totalAdaMoved)
                .isSimpleTransfer(isSimpleTransfer)
                .hasScriptInteraction(hasScriptInteraction)
                .build();

        return TransactionNetTransfer.builder()
                .txHash(txDetails.getHash())
                .blockHeight(txDetails.getBlockHeight())
                .slot(txDetails.getSlot())
                .fee(txDetails.getFees())
                .netTransfers(netTransfers)
                .senders(senders)
                .receivers(receivers)
                .summary(summary)
                .build();
    }

    /**
     * Aggregate UTXOs by address
     */
    private static Map<String, AddressAmounts> aggregateUtxosByAddress(List<TxUtxo> utxos) {
        if (utxos == null || utxos.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, AddressAmounts> result = new HashMap<>();

        for (TxUtxo utxo : utxos) {
            String address = utxo.getAddress();
            AddressAmounts amounts = result.computeIfAbsent(address, k -> new AddressAmounts());
            amounts.stakeAddress = utxo.getStakeAddress();

            // Process each amount in the UTXO
            if (utxo.getAmount() != null) {
                for (TxUtxo.Amount amount : utxo.getAmount()) {
                    if ("lovelace".equals(amount.getUnit()) || amount.getPolicyId() == null) {
                        // This is ADA
                        amounts.lovelace = amounts.lovelace.add(amount.getQuantity());
                    } else {
                        // This is a native asset
                        String unit = amount.getUnit();
                        AssetAmount assetAmount = amounts.assets.computeIfAbsent(unit, k -> new AssetAmount());
                        assetAmount.quantity = assetAmount.quantity.add(amount.getQuantity());
                        assetAmount.policyId = amount.getPolicyId();
                        assetAmount.assetName = amount.getAssetName();
                    }
                }
            }
        }

        return result;
    }

    /**
     * Helper class to track amounts per address
     */
    private static class AddressAmounts {
        BigInteger lovelace = BigInteger.ZERO;
        Map<String, AssetAmount> assets = new HashMap<>();
        String stakeAddress;
    }

    /**
     * Helper class to track asset amounts
     */
    private static class AssetAmount {
        BigInteger quantity = BigInteger.ZERO;
        String policyId;
        String assetName;
    }
}
