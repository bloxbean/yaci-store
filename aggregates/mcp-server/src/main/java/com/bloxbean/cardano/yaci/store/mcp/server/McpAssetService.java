package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.assets.domain.TxAsset;
import com.bloxbean.cardano.yaci.store.common.domain.Utxo;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.utxo.domain.AssetTransaction;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"store.assets.enabled", "store.mcp-server.tools.assets.enabled"},
    havingValue = "true",
    matchIfMissing = true
)
public class McpAssetService {
    @Qualifier("assetService")
    private final com.bloxbean.cardano.yaci.store.api.assets.service.AssetService assetService;

    @Qualifier("utxoAssetService")
    private final com.bloxbean.cardano.yaci.store.api.utxo.service.AssetService utxoAssetService;

    @Tool(name = "asset-minting-txs-by-fingerprint",
            description = "Get asset minting transactions by CIP-14 asset fingerprint (e.g., asset1...). Returns list of transactions where this asset was minted or burned. Page is 0-based.")
    public List<TxAsset> getAssetTxsByFingerprint(String fingerprint, int page, int count) {
        return assetService.getAssetTxsByFingerprint(fingerprint, page, count);
    }

    @Tool(name = "asset-minting-txs-by-policy",
            description = "Get all asset minting transactions for a specific policy ID. Returns list of transactions for all assets under this policy. Page is 0-based. Useful for tracking NFT collections.")
    public List<TxAsset> getAssetTxsByPolicyId(String policyId, int page, int count) {
        return assetService.getAssetTxsByPolicyId(policyId, page, count);
    }

    @Tool(name = "asset-minting-txs-by-unit",
            description = "Get asset minting transactions by asset unit (policyId + assetName in hex). Returns list of transactions where this specific asset was minted or burned. Page is 0-based.")
    public List<TxAsset> getAssetTxsByUnit(String unit, int page, int count) {
        return assetService.getAssetTxsByUnit(unit, page, count);
    }

    @Tool(name = "assets-by-transaction",
            description = "Get all assets minted or burned in a specific transaction by transaction hash. Returns list of assets with their quantities and metadata from the transaction.")
    public List<TxAsset> getAssetsByTx(String txHash) {
        return assetService.getAssetsByTx(txHash);
    }

    @Tool(name = "asset-supply-by-fingerprint",
            description = "Get the current total supply of an asset by its CIP-14 fingerprint (e.g., asset1...). Returns the circulating supply as a BigInteger. Negative values indicate net burning.")
    public BigInteger getSupplyByFingerprint(String fingerprint) {
        return assetService.getSupplyByFingerprint(fingerprint)
                .orElseThrow(() -> new RuntimeException("Supply not found for fingerprint: " + fingerprint));
    }

    @Tool(name = "asset-supply-by-unit",
            description = "Get the current total supply of an asset by its unit (policyId + assetName in hex). Returns the circulating supply as a BigInteger. Negative values indicate net burning.")
    public BigInteger getSupplyByUnit(String unit) {
        return assetService.getSupplyByUnit(unit)
                .orElseThrow(() -> new RuntimeException("Supply not found for unit: " + unit));
    }

    @Tool(name = "asset-supply-by-policy",
            description = "Get the total supply across all assets for a specific policy ID. Returns the aggregate supply for the entire policy. Useful for understanding policy-wide token economics.")
    public BigInteger getSupplyByPolicy(String policyId) {
        return assetService.getSupplyByPolicy(policyId)
                .orElseThrow(() -> new RuntimeException("Supply not found for policy: " + policyId));
    }

    @Tool(name = "utxos-by-asset",
            description = "Get all UTXOs containing a specific asset by asset unit (policyId + assetName in hex). Returns UTXOs with addresses holding this asset. Page is 0-based. Useful for finding asset holders.")
    public List<Utxo> getUtxosByAsset(String unit, int page, int count) {
        return utxoAssetService.getUtxosByAsset(unit, page, count, Order.desc);
    }

    @Tool(name = "asset-transactions",
            description = "Get all transactions involving a specific asset by asset unit (policyId + assetName in hex). Returns transactions where the asset was transferred, minted, or burned. Page is 0-based.")
    public List<AssetTransaction> getAssetTransactions(String unit, int page, int count) {
        return utxoAssetService.getAssetTransactionsByAsset(unit, page, count, Order.desc);
    }
}
