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
            description = "Get asset minting transactions by CIP-14 asset fingerprint (e.g., asset1...). " +
                         "Returns list of transactions where this asset was minted or burned. " +
                         "IMPORTANT: Each returned asset includes a 'unit' field (policyId + assetName in hex) which serves as the subject for Cardano Token Registry lookups. " +
                         "To provide human-readable token information, fetch verified metadata from: " +
                         "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<unit>.json " +
                         "Extract 'name', 'ticker', 'description', 'decimals' fields to present tokens as 'TokenName (TICKER)' instead of hex values. " +
                         "Handle 404 gracefully - not all tokens are registered in the official registry. " +
                         "Page is 0-based.")
    public List<TxAsset> getAssetTxsByFingerprint(String fingerprint, int page, int count) {
        return assetService.getAssetTxsByFingerprint(fingerprint, page, count);
    }

    @Tool(name = "asset-minting-txs-by-policy",
            description = "Get all asset minting transactions for a specific policy ID. " +
                         "Returns list of transactions for all assets under this policy. " +
                         "IMPORTANT: Each asset's 'unit' field can be used to fetch Cardano Token Registry metadata from: " +
                         "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<unit>.json " +
                         "For NFT collections, fetch metadata for each unique asset to display collection items with human-readable names. " +
                         "Extract 'name', 'ticker', 'description' to provide context about each token. " +
                         "Page is 0-based. Useful for tracking NFT collections.")
    public List<TxAsset> getAssetTxsByPolicyId(String policyId, int page, int count) {
        return assetService.getAssetTxsByPolicyId(policyId, page, count);
    }

    @Tool(name = "asset-minting-txs-by-unit",
            description = "Get asset minting transactions by asset unit (policyId + assetName in hex). " +
                         "Returns list of transactions where this specific asset was minted or burned. " +
                         "IMPORTANT: The 'unit' parameter/field is the exact subject needed for token registry lookup. " +
                         "Fetch verified token metadata from: " +
                         "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<unit>.json " +
                         "Display token as 'TokenName (TICKER)' with description for better user understanding. " +
                         "Page is 0-based.")
    public List<TxAsset> getAssetTxsByUnit(String unit, int page, int count) {
        return assetService.getAssetTxsByUnit(unit, page, count);
    }

    @Tool(name = "assets-by-transaction",
            description = "Get all assets minted or burned in a specific transaction by transaction hash. " +
                         "Returns list of assets with their quantities and metadata from the transaction. " +
                         "IMPORTANT: For each asset returned, use the 'unit' field to fetch human-readable token information from: " +
                         "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<unit>.json " +
                         "Present tokens with their verified names and tickers to help users understand what was minted/burned in the transaction. " +
                         "Example: Instead of showing hex values, display '1000 MIDAS tokens minted'.")
    public List<TxAsset> getAssetsByTx(String txHash) {
        return assetService.getAssetsByTx(txHash);
    }

    @Tool(name = "asset-supply-by-fingerprint",
            description = "Get the current total supply of an asset by its CIP-14 fingerprint (e.g., asset1...). " +
                         "Returns the circulating supply as a BigInteger. Negative values indicate net burning. " +
                         "IMPORTANT: After getting supply, fetch token registry metadata to provide context about what token this supply represents. " +
                         "Use the asset's unit field to construct registry URL and fetch name, ticker, decimals. " +
                         "Format output using decimals field: e.g., '1000000 supply with 6 decimals = 1.000000 TICKER'.")
    public BigInteger getSupplyByFingerprint(String fingerprint) {
        return assetService.getSupplyByFingerprint(fingerprint)
                .orElseThrow(() -> new RuntimeException("Supply not found for fingerprint: " + fingerprint));
    }

    @Tool(name = "asset-supply-by-unit",
            description = "Get the current total supply of an asset by its unit (policyId + assetName in hex). " +
                         "Returns the circulating supply as a BigInteger. Negative values indicate net burning. " +
                         "IMPORTANT: The 'unit' parameter is the subject for token registry lookup. " +
                         "Fetch metadata from: https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<unit>.json " +
                         "Use 'decimals' field to format supply correctly and present as 'TokenName (TICKER): X.XXX tokens in circulation'.")
    public BigInteger getSupplyByUnit(String unit) {
        return assetService.getSupplyByUnit(unit)
                .orElseThrow(() -> new RuntimeException("Supply not found for unit: " + unit));
    }

    @Tool(name = "asset-supply-by-policy",
            description = "Get the total supply across all assets for a specific policy ID. " +
                         "Returns the aggregate supply for the entire policy. " +
                         "IMPORTANT: For policies with multiple assets (NFT collections), each asset has a unique 'unit'. " +
                         "Fetch token registry metadata for representative assets to identify the collection/policy. " +
                         "Useful for understanding policy-wide token economics.")
    public BigInteger getSupplyByPolicy(String policyId) {
        return assetService.getSupplyByPolicy(policyId)
                .orElseThrow(() -> new RuntimeException("Supply not found for policy: " + policyId));
    }

    @Tool(name = "utxos-by-asset",
            description = "Get all UTXOs containing a specific asset by asset unit (policyId + assetName in hex). " +
                         "Returns UTXOs with addresses holding this asset. " +
                         "IMPORTANT: Use the 'unit' parameter to fetch token name from registry: " +
                         "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<unit>.json " +
                         "Present results as 'Holders of TokenName (TICKER)' instead of hex values for better readability. " +
                         "Page is 0-based. Useful for finding asset holders.")
    public List<Utxo> getUtxosByAsset(String unit, int page, int count) {
        return utxoAssetService.getUtxosByAsset(unit, page, count, Order.desc);
    }

    @Tool(name = "asset-transactions",
            description = "Get all transactions involving a specific asset by asset unit (policyId + assetName in hex). " +
                         "Returns transactions where the asset was transferred, minted, or burned. " +
                         "IMPORTANT: Fetch token registry metadata using the 'unit' parameter to provide context: " +
                         "https://raw.githubusercontent.com/cardano-foundation/cardano-token-registry/refs/heads/master/mappings/<unit>.json " +
                         "Present transaction history as 'TokenName (TICKER) transactions' with human-readable descriptions. " +
                         "Page is 0-based.")
    public List<AssetTransaction> getAssetTransactions(String unit, int page, int count) {
        return utxoAssetService.getAssetTransactionsByAsset(unit, page, count, Order.desc);
    }
}
