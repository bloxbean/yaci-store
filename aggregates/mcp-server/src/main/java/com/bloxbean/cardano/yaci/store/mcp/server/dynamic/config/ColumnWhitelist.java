package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Whitelist of allowed columns per table for dynamic queries.
 * Only columns explicitly listed here can be selected, filtered, or aggregated.
 */
@Component
public class ColumnWhitelist {

    private static final Map<String, Set<String>> ALLOWED_COLUMNS = Map.ofEntries(
        Map.entry("address_utxo", Set.of(
            "tx_hash", "output_index", "owner_addr", "owner_stake_addr",
            "owner_payment_credential", "owner_stake_credential", "owner_addr_full",
            "lovelace_amount", "data_hash", "inline_datum", "reference_script_hash",
            "script_ref", "epoch", "slot", "block", "block_hash", "block_time",
            "is_collateral_return", "amounts", "update_datetime"
        )),
        Map.entry("transaction", Set.of(
            "tx_hash", "epoch", "slot", "block", "block_hash", "block_index",
            "fee", "invalid", "total_output", "tx_size", "script_size"
        )),
        Map.entry("block", Set.of(
            "number", "hash", "epoch", "slot", "epoch_slot", "slot_leader",
            "body_size", "total_output", "total_fees", "no_of_txs",
            "block_time", "prev_hash", "era"
        )),
        Map.entry("tx_input", Set.of(
            "tx_hash", "output_index", "spent_tx_hash", "spent_at_block",
            "spent_at_slot", "spent_epoch"
        )),
        Map.entry("epoch", Set.of(
            "no", "start_time", "end_time", "block_count", "tx_count",
            "out_sum", "fees"
        )),
        Map.entry("pool_registration", Set.of(
            "pool_id", "vrf_key_hash", "pledge", "cost", "margin",
            "reward_addr", "epoch"
        )),
        Map.entry("pool_update", Set.of(
            "id", "pool_id", "epoch", "active_epoch"
        )),
        Map.entry("stake_registration", Set.of(
            "epoch", "tx_hash", "cert_index", "credential", "type"
        )),
        Map.entry("delegation", Set.of(
            "tx_hash", "cert_index", "tx_index", "credential", "cred_type",
            "pool_id", "address", "epoch", "slot", "block_hash", "block",
            "block_time", "update_datetime"
        )),
        Map.entry("epoch_stake", Set.of(
            "epoch", "address", "amount", "pool_id",
            "delegation_epoch", "active_epoch", "create_datetime"
        )),
        Map.entry("assets", Set.of(
            "id", "slot", "tx_hash", "policy", "asset_name", "unit",
            "fingerprint", "quantity", "mint_type", "block", "block_time",
            "update_datetime"
        )),
        Map.entry("pool", Set.of(
            "pool_id", "tx_hash", "cert_index", "tx_index", "status",
            "amount", "epoch", "active_epoch", "retire_epoch", "registration_slot",
            "slot", "block_hash", "block", "block_time", "update_datetime"
        )),
        Map.entry("reward", Set.of(
            "address", "earned_epoch", "type", "pool_id", "amount",
            "spendable_epoch", "slot", "update_datetime"
        )),
        Map.entry("reward_rest", Set.of(
            "id", "address", "type", "amount", "earned_epoch",
            "spendable_epoch", "slot", "create_datetime"
        )),
        Map.entry("transaction_metadata", Set.of(
            "id", "slot", "tx_hash", "label", "body", "cbor",
            "block", "block_time", "update_datetime"
        )),
        Map.entry("datum", Set.of(
            "hash", "datum", "created_at_tx", "create_datetime", "update_datetime"
        )),
        Map.entry("transaction_scripts", Set.of(
            "id", "slot", "block_hash", "tx_hash", "script_hash", "script_type",
            "datum_hash", "redeemer_cbor", "unit_mem", "unit_steps", "purpose",
            "redeemer_index", "redeemer_datahash", "block", "block_time", "update_datetime"
        )),
        Map.entry("script", Set.of(
            "script_hash", "script_type", "content", "create_datetime", "update_datetime"
        )),
        // Flattened UTXO views for enhanced LLM query support
        Map.entry("utxo_assets_flat", Set.of(
            "tx_hash", "output_index", "owner_addr", "owner_stake_addr",
            "owner_payment_credential", "owner_stake_credential", "lovelace_amount",
            "epoch", "slot", "block", "block_hash", "block_time", "update_datetime",
            "asset_unit", "policy_id", "asset_name", "quantity", "is_spent"
        )),
        Map.entry("utxo_assets_unspent", Set.of(
            "tx_hash", "output_index", "owner_addr", "owner_stake_addr",
            "owner_payment_credential", "owner_stake_credential", "lovelace_amount",
            "epoch", "slot", "block", "block_hash", "block_time", "update_datetime",
            "asset_unit", "policy_id", "asset_name", "quantity"
        )),
        Map.entry("token_holder_summary", Set.of(
            "policy_id", "asset_name", "asset_unit", "holder_count", "total_supply",
            "utxo_count", "min_quantity", "max_quantity", "avg_quantity", "median_quantity"
        )),
        Map.entry("address_lovelace_balance", Set.of(
            "owner_addr", "owner_stake_addr", "owner_payment_credential",
            "owner_stake_credential", "tx_hash", "output_index", "lovelace_amount",
            "epoch", "slot", "block", "block_time", "update_datetime", "is_spent"
        ))
    );

    /**
     * Checks if a column is allowed for a specific table.
     * Supports both simple column names ("tx_hash") and qualified names ("tx_input.spent_tx_hash").
     */
    public boolean isColumnAllowed(String table, String column) {
        // Handle qualified column names (e.g., "tx_input.spent_tx_hash")
        if (column.contains(".")) {
            String[] parts = column.split("\\.", 2);
            String qualifiedTable = parts[0];
            String qualifiedColumn = parts[1];
            Set<String> columns = ALLOWED_COLUMNS.get(qualifiedTable);
            return columns != null && columns.contains(qualifiedColumn);
        }

        // Handle simple column names
        Set<String> columns = ALLOWED_COLUMNS.get(table);
        return columns != null && columns.contains(column);
    }

    /**
     * Gets all allowed columns for a table.
     */
    public Set<String> getAllowedColumns(String table) {
        return ALLOWED_COLUMNS.getOrDefault(table, Set.of());
    }
}
