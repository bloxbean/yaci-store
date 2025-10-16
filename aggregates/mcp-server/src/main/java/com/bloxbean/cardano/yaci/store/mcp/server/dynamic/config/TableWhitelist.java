package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.config;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Whitelist of allowed tables for dynamic queries.
 * Only tables explicitly listed here can be queried.
 */
@Component
public class TableWhitelist {

    // Allowed tables with their primary key columns
    private static final Map<String, List<String>> ALLOWED_TABLES = Map.ofEntries(
        Map.entry("address_utxo", List.of("tx_hash", "output_index")),
        Map.entry("transaction", List.of("tx_hash")),
        Map.entry("block", List.of("number")),
        Map.entry("tx_input", List.of("tx_hash", "output_index")),
        Map.entry("epoch", List.of("no")),
        Map.entry("pool_registration", List.of("pool_id")),
        Map.entry("pool_update", List.of("id")),
        Map.entry("stake_registration", List.of("tx_hash", "cert_index")),
        Map.entry("delegation", List.of("tx_hash", "cert_index")),
        Map.entry("epoch_stake", List.of("epoch", "address")),
        Map.entry("assets", List.of("id")),
        Map.entry("pool", List.of("pool_id", "tx_hash", "cert_index")),
        Map.entry("reward", List.of("address", "earned_epoch", "type", "pool_id")),
        Map.entry("reward_rest", List.of("id")),
        Map.entry("transaction_metadata", List.of("id")),
        Map.entry("datum", List.of("hash")),
        Map.entry("transaction_scripts", List.of("id")),
        Map.entry("script", List.of("script_hash")),
        // Flattened UTXO views for enhanced LLM query support
        Map.entry("utxo_assets_flat", List.of("tx_hash", "output_index", "asset_unit")),
        Map.entry("utxo_assets_unspent", List.of("tx_hash", "output_index", "asset_unit")),
        Map.entry("token_holder_summary", List.of("policy_id", "asset_name", "asset_unit")),
        Map.entry("address_lovelace_balance", List.of("owner_addr", "tx_hash", "output_index"))
    );

    /**
     * Checks if a table is allowed for dynamic queries.
     */
    public boolean isAllowed(String table) {
        return ALLOWED_TABLES.containsKey(table);
    }

    /**
     * Gets the primary key columns for a table.
     */
    public List<String> getPrimaryKeys(String table) {
        return ALLOWED_TABLES.getOrDefault(table, List.of());
    }

    /**
     * Gets all allowed table names.
     */
    public Set<String> getAllowedTables() {
        return ALLOWED_TABLES.keySet();
    }
}
