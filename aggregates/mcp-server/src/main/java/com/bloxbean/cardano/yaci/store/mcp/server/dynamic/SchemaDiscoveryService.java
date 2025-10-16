package com.bloxbean.cardano.yaci.store.mcp.server.dynamic;

import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.config.ColumnWhitelist;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.config.TableWhitelist;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * MCP service providing schema discovery for dynamic queries.
 * Helps AI understand available tables, columns, and query patterns.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "store.mcp-server.aggregation.dynamic-query.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class SchemaDiscoveryService {

    private final TableWhitelist tableWhitelist;
    private final ColumnWhitelist columnWhitelist;

    @Tool(name = "get-query-schema",
          description = "Get database schema information for building dynamic queries. " +
                        "Returns available tables, columns, data types, and common query patterns. " +
                        "IMPORTANT: Only use this when you've determined that dynamic-aggregation-query is necessary. " +
                        "Remember: dynamic-aggregation-query is a LAST RESORT - try Tier 1 tools first! " +
                        "If you must use dynamic queries, ALWAYS call this tool FIRST to understand " +
                        "available tables, columns, and query patterns.")
    public SchemaDiscoveryResponse getSchema() {
        log.info("Schema discovery requested");

        List<TableSchema> tables = new ArrayList<>();

        // Add all table schemas
        tables.add(createAddressUtxoSchema());
        tables.add(createTransactionSchema());
        tables.add(createBlockSchema());
        tables.add(createTxInputSchema());
        tables.add(createEpochSchema());
        tables.add(createPoolRegistrationSchema());
        tables.add(createPoolUpdateSchema());
        tables.add(createStakeRegistrationSchema());
        tables.add(createDelegationSchema());
        tables.add(createAssetsSchema());

        // Flattened UTXO views for simplified asset queries
        tables.add(createUtxoAssetsFlatSchema());
        tables.add(createUtxoAssetsUnspentSchema());
        tables.add(createTokenHolderSummarySchema());
        tables.add(createAddressLovelaceBalanceSchema());

        String documentation = """
            # Dynamic Query Schema

            ## ⚠️ TOOL SELECTION PRIORITY
            This information is ONLY for dynamic-aggregation-query, which should be your LAST RESORT.

            **ALWAYS follow this priority:**
            1. **FIRST**: Try specific Tier 1 tools (utxos-by-address, blocks-list, transactions-list, assets-by-transaction, etc.)
            2. **SECOND**: Try predefined aggregation tools if available (they are optimized)
            3. **LAST RESORT**: Use dynamic-aggregation-query only when:
               - Existing tools don't provide the required data
               - Multiple Tier 1 calls would be too slow/complex
               - Custom cross-table aggregations are needed

            ## Important Notes
            - Use exact table names (e.g., 'address_utxo' not 'utxo')
            - All queries are validated against whitelists
            - Maximum 10,000 rows per query
            - Query timeout: 30 seconds

            ## 🚀 Flattened Views (RECOMMENDED for Asset Queries)

            For easier asset queries without JSONB complexity, use these pre-built views:

            ### utxo_assets_flat
            **RECOMMENDED**: Use this instead of querying address_utxo.amounts JSONB directly!
            - Flattens JSONB amounts array into standard SQL columns
            - Pre-computed is_spent status (no JOIN needed)
            - One row per asset per UTXO

            Example - Find all unspent UTXOs for a token:
            ```
            table: utxo_assets_flat
            filters: [
              {column: "policy_id", operator: "EQ", value: "abc123..."},
              {column: "is_spent", operator: "EQ", value: false}
            ]
            ```

            ### utxo_assets_unspent
            **MOST COMMON**: Pre-filtered to unspent UTXOs only (95% of use cases).

            Example - Get asset holdings for an address:
            ```
            table: utxo_assets_unspent
            selectFields: [
              {column: "policy_id"},
              {column: "asset_name"},
              {column: "quantity", function: "SUM", alias: "total"}
            ]
            filters: [{column: "owner_addr", operator: "EQ", value: "addr1..."}]
            groupBy: ["policy_id", "asset_name"]
            ```

            ### token_holder_summary
            **FASTEST**: Pre-aggregated token statistics. Use for token discovery!

            Example - Find tokens with 100+ holders:
            ```
            table: token_holder_summary
            filters: [{column: "holder_count", operator: "GTE", value: 100}]
            orderBy: [{column: "holder_count", direction: "DESC"}]
            limit: 20
            ```

            ### address_lovelace_balance
            **ADA ONLY**: Fast ADA balance queries without JSONB overhead.

            Example - Current ADA balance for address:
            ```
            table: address_lovelace_balance
            selectFields: [
              {column: "owner_addr"},
              {column: "lovelace_amount", function: "SUM", alias: "total_ada"}
            ]
            filters: [
              {column: "owner_addr", operator: "EQ", value: "addr1..."},
              {column: "is_spent", operator: "EQ", value: false}
            ]
            groupBy: ["owner_addr"]
            ```

            ## 🔍 TOKEN ANALYSIS DECISION GUIDE

            **CRITICAL: Choose the correct table based on what you want to analyze!**

            ### Question: "What tokens were minted/burned recently?"
            ✅ USE: `assets` table
            - Tracks mint and burn events ONLY
            - Filter by slot range for time period
            - quantity > 0 = minting, quantity < 0 = burning
            - Example: "Find tokens minted in last hour"
            ```
            table: assets
            filters: [
              {column: "slot", operator: "GT", value: <slot_1hr_ago>},
              {column: "quantity", operator: "GT", value: 0}
            ]
            groupBy: ["policy", "asset_name", "unit"]
            ```

            ### Question: "What tokens does this address currently hold?"
            ✅ USE: `utxo_assets_unspent` view
            - Pre-filtered to unspent UTXOs (current holdings)
            - No JOIN needed
            - Example: "Show my portfolio"
            ```
            table: utxo_assets_unspent
            selectFields: [
              {column: "policy_id"},
              {column: "asset_name"},
              {column: "quantity", function: "SUM", alias: "total"}
            ]
            filters: [{column: "owner_addr", operator: "EQ", value: "addr1..."}]
            groupBy: ["policy_id", "asset_name"]
            ```

            ### Question: "What tokens with 100+ holders exist?"
            ✅ USE: `token_holder_summary` view
            - Pre-aggregated statistics (FASTEST!)
            - No aggregation needed
            - Example: "Find popular tokens"
            ```
            table: token_holder_summary
            filters: [{column: "holder_count", operator: "GTE", value: 100}]
            orderBy: [{column: "holder_count", direction: "DESC"}]
            ```

            ### Question: "How many times was this token transferred in last week?"
            ✅ USE: `utxo_assets_flat` view
            - Shows all UTXOs (spent and unspent) with asset info
            - Filter by time period
            - COUNT spent UTXOs = transfer count
            - Example: "Token transfer activity"
            ```
            table: utxo_assets_flat
            selectFields: [{column: "tx_hash", function: "COUNT", alias: "transfers"}]
            filters: [
              {column: "policy_id", operator: "EQ", value: "abc123..."},
              {column: "is_spent", operator: "EQ", value: true},
              {column: "slot", operator: "GT", value: <slot_1week_ago>}
            ]
            ```

            ### Question: "What's the minting history of a specific token?"
            ✅ USE: `assets` table
            - All mint/burn events for the token
            - Shows when and how much was minted/burned
            - Example: "Token supply timeline"
            ```
            table: assets
            selectFields: [
              {column: "slot"},
              {column: "quantity"},
              {column: "tx_hash"}
            ]
            filters: [{column: "unit", operator: "EQ", value: "<policy+name_hex>"}]
            orderBy: [{column: "slot", direction: "ASC"}]
            ```

            ### Quick Reference Table:

            | Query Type | Table to Use | Why |
            |------------|--------------|-----|
            | Mint/burn events | `assets` | Only table with mint/burn records |
            | Current holdings | `utxo_assets_unspent` | Pre-filtered to unspent |
            | Transfer activity | `utxo_assets_flat` | Shows all UTXOs with spent status |
            | Token popularity | `token_holder_summary` | Pre-aggregated stats |
            | ADA balance only | `address_lovelace_balance` | No JSONB overhead |
            | Complex UTXO queries | `address_utxo` | Full UTXO data with JSONB |

            ⚠️ COMMON MISTAKES TO AVOID:
            1. ❌ Using `assets` table for "transactions with token X" → Use `utxo_assets_flat`
            2. ❌ Using `address_utxo` for "tokens with N+ holders" → Use `token_holder_summary`
            3. ❌ Aggregating `utxo_assets_flat` when `token_holder_summary` already has it
            4. ❌ Using table name "asset" (singular) → Correct name is "assets" (plural)

            ## Common Patterns

            ### Finding Unspent UTXOs (IMPORTANT!)
            ⚠️ CRITICAL: tx_input requires a COMPOSITE JOIN on BOTH tx_hash AND output_index!
            Use LEFT JOIN with tx_input and check if spent_tx_hash IS NULL:
            ```
            table: address_utxo
            joins: [{
              joinTable: "tx_input",
              joinType: "LEFT",
              conditions: [
                {leftColumn: "address_utxo.tx_hash", rightColumn: "tx_input.tx_hash"},
                {leftColumn: "address_utxo.output_index", rightColumn: "tx_input.output_index"}
              ]
            }]
            filters: [{column: "tx_input.spent_tx_hash", operator: "IS_NULL"}]
            ```

            ### Finding Unspent UTXOs at Specific Epoch/Block
            Check spent_epoch or spent_at_block:
            ```
            # Current unspent:
            filters: [{column: "tx_input.spent_tx_hash", operator: "IS_NULL"}]

            # Unspent at epoch 500:
            filters: [
              {column: "epoch", operator: "LTE", value: 500},
              {column: "tx_input.spent_epoch", operator: "GT", value: 500, logicalOp: "OR"},
              {column: "tx_input.spent_epoch", operator: "IS_NULL"}
            ]
            ```

            ### Historical Balance Queries
            Filter by epoch/slot and check spent_epoch:
            ```
            table: address_utxo
            filters: [
              {column: "epoch", operator: "LTE", value: 85},
              {column: "owner_addr", operator: "EQ", value: "addr1..."}
            ]
            ```

            ### JSONB Queries (amounts column)
            The 'amounts' column is JSONB array containing asset information.
            Example structure: [{"unit":"lovelace","quantity":2000000,"policy_id":null,"asset_name":"lovelace"},
                                {"unit":"5c303...","quantity":1,"policy_id":"5c303...","asset_name":"NodeFeed"}]

            Find UTXOs with native assets (more than just lovelace):
            ```
            table: address_utxo
            filters: [{column: "amounts", operator: "JSONB_ARRAY_LENGTH_GT", value: 1}]
            ```

            Count assets per UTXO:
            ```
            table: address_utxo
            selectFields: [
              {column: "tx_hash"},
              {column: "amounts", function: "JSONB_ARRAY_LENGTH", alias: "num_assets"}
            ]
            ```

            Available JSONB operators:
            - JSONB_ARRAY_LENGTH_EQ: array length equals
            - JSONB_ARRAY_LENGTH_GT: array length greater than
            - JSONB_ARRAY_LENGTH_GTE: array length greater than or equal
            - JSONB_ARRAY_LENGTH_LT: array length less than
            - JSONB_ARRAY_LENGTH_LTE: array length less than or equal
            - JSONB_CONTAINS: check if JSONB contains another JSONB (PostgreSQL @>)
            - JSONB_PATH_MATCH: match JSONPath expression (PostgreSQL @@)

            ### Searching by Asset Name or Policy ID
            ⚠️ ALWAYS USE JSONB_CONTAINS (NOT JSONB_PATH_MATCH) for asset searches!

            Find UTXOs containing a specific asset by name:
            ```
            table: address_utxo
            filters: [{column: "amounts", operator: "JSONB_CONTAINS",
                      value: "[{\"asset_name\":\"tTEURO\"}]"}]
            ```

            Find UTXOs containing a specific asset by policy_id:
            ```
            table: address_utxo
            filters: [{column: "amounts", operator: "JSONB_CONTAINS",
                      value: "[{\"policy_id\":\"e68f1cea19752d1292b4be71b7f5d2b3219a15859c028f7454f66cdf\"}]"}]
            ```

            Find UTXOs with both policy_id AND asset_name:
            ```
            table: address_utxo
            filters: [{column: "amounts", operator: "JSONB_CONTAINS",
                      value: "[{\"policy_id\":\"e68f1cea19752d1292b4be71b7f5d2b3219a15859c028f7454f66cdf\",\"asset_name\":\"tTEURO\"}]"}]
            ```

            CRITICAL:
            - JSONB_CONTAINS uses PostgreSQL's @> operator for partial object matching
            - Value must be valid JSON array with object(s) containing the fields to match
            - DO NOT use JSONB_PATH_MATCH for asset searches - it has different syntax requirements

            ### Complete Example: Unspent UTXOs with Specific Asset
            Combining JSONB search + unspent check (most common use case):
            ```
            table: address_utxo
            selectFields: [
              {column: "address_utxo.tx_hash"},
              {column: "address_utxo.lovelace_amount"},
              {column: "address_utxo.amounts"}
            ]
            joins: [{
              joinTable: "tx_input",
              joinType: "LEFT",
              conditions: [
                {leftColumn: "address_utxo.tx_hash", rightColumn: "tx_input.tx_hash"},
                {leftColumn: "address_utxo.output_index", rightColumn: "tx_input.output_index"}
              ]
            }]
            filters: [
              {column: "amounts", operator: "JSONB_CONTAINS",
               value: "[{\"policy_id\":\"e68f1cea19752d1292b4be71b7f5d2b3219a15859c028f7454f66cdf\"}]",
               logicalOp: "AND"},
              {column: "tx_input.spent_tx_hash", operator: "IS_NULL", logicalOp: "AND"}
            ]
            ```

            This finds ALL unspent UTXOs containing assets with the specified policy_id.
            """;

        return new SchemaDiscoveryResponse(tables, documentation);
    }

    private TableSchema createAddressUtxoSchema() {
        return new TableSchema(
            "address_utxo",
            "UTXO outputs indexed by address. Use this to query wallet balances, UTXO sets, and asset holdings.",
            tableWhitelist.getPrimaryKeys("address_utxo"),
            List.of(
                new ColumnSchema("tx_hash", "string", "Transaction hash that created this UTXO"),
                new ColumnSchema("output_index", "integer", "Output index within the transaction"),
                new ColumnSchema("owner_addr", "string", "Cardano address (addr1... or stake1...)"),
                new ColumnSchema("owner_stake_addr", "string", "Associated stake address"),
                new ColumnSchema("owner_payment_credential", "string", "Payment credential hash"),
                new ColumnSchema("owner_stake_credential", "string", "Stake credential hash"),
                new ColumnSchema("owner_addr_full", "string", "Full address with all details"),
                new ColumnSchema("lovelace_amount", "bigint", "ADA amount in lovelace"),
                new ColumnSchema("amounts", "jsonb", "All asset amounts (lovelace + native tokens)"),
                new ColumnSchema("epoch", "integer", "Epoch when UTXO was created"),
                new ColumnSchema("slot", "bigint", "Slot when UTXO was created"),
                new ColumnSchema("block", "bigint", "Block number"),
                new ColumnSchema("block_hash", "string", "Block hash"),
                new ColumnSchema("block_time", "bigint", "Block timestamp"),
                new ColumnSchema("data_hash", "string", "Datum hash (if present)"),
                new ColumnSchema("inline_datum", "string", "Inline datum (if present)"),
                new ColumnSchema("reference_script_hash", "string", "Reference script hash"),
                new ColumnSchema("script_ref", "string", "Reference script content"),
                new ColumnSchema("is_collateral_return", "boolean", "Whether this is a collateral return output"),
                new ColumnSchema("update_datetime", "timestamp", "Last update time")
            ),
            List.of(
                new QueryPattern(
                    "Find unspent UTXOs for an address",
                    "LEFT JOIN tx_input ON address_utxo.tx_hash = tx_input.tx_hash AND address_utxo.output_index = tx_input.output_index",
                    "CRITICAL: Use composite JOIN with conditions array, then filter tx_input.spent_tx_hash IS NULL"
                ),
                new QueryPattern(
                    "Get current balance",
                    "SUM(lovelace_amount) for unspent UTXOs",
                    "Aggregate lovelace_amount for UTXOs not in tx_input"
                ),
                new QueryPattern(
                    "Find all assets for address",
                    "Query amounts column (JSONB)",
                    "The amounts column contains all assets including lovelace and native tokens"
                ),
                new QueryPattern(
                    "Find UTXOs with native assets",
                    "Filter amounts using JSONB_ARRAY_LENGTH_GT operator with value 1",
                    "UTXOs with native assets have amounts array length > 1 (lovelace + tokens)"
                ),
                new QueryPattern(
                    "Count number of assets per UTXO",
                    "Use JSONB_ARRAY_LENGTH function on amounts column",
                    "Returns integer count of elements in the amounts JSONB array"
                ),
                new QueryPattern(
                    "Find UTXOs by asset name",
                    "Use JSONB_CONTAINS operator: amounts @> '[{\"asset_name\":\"TokenName\"}]'",
                    "Search for specific token name within the amounts JSONB array"
                ),
                new QueryPattern(
                    "Find UTXOs by policy ID",
                    "Use JSONB_CONTAINS operator: amounts @> '[{\"policy_id\":\"abc123...\"}]'",
                    "Search for specific policy ID within the amounts JSONB array"
                )
            )
        );
    }

    private TableSchema createTransactionSchema() {
        return new TableSchema(
            "transaction",
            "All transactions on the blockchain. Use for transaction-level analytics, fee trends, and network usage patterns.",
            tableWhitelist.getPrimaryKeys("transaction"),
            List.of(
                new ColumnSchema("tx_hash", "string", "Unique transaction hash"),
                new ColumnSchema("epoch", "integer", "Epoch number"),
                new ColumnSchema("slot", "bigint", "Slot number"),
                new ColumnSchema("block", "bigint", "Block number"),
                new ColumnSchema("block_hash", "string", "Block hash"),
                new ColumnSchema("block_index", "integer", "Index within block"),
                new ColumnSchema("fee", "bigint", "Transaction fee in lovelace"),
                new ColumnSchema("invalid", "boolean", "Whether transaction is invalid"),
                new ColumnSchema("total_output", "bigint", "Total output in lovelace"),
                new ColumnSchema("tx_size", "integer", "Transaction size in bytes"),
                new ColumnSchema("script_size", "integer", "Script size in bytes")
            ),
            List.of(
                new QueryPattern(
                    "Network fee trends by epoch",
                    "GROUP BY epoch, aggregate COUNT, SUM(fee), AVG(fee)",
                    "Example: {table: 'transaction', selectFields: [{column: 'epoch'}, {column: 'tx_hash', function: 'COUNT', alias: 'tx_count'}, {column: 'fee', function: 'SUM', alias: 'total_fees'}, {column: 'fee', function: 'AVG', alias: 'avg_fee'}], groupBy: [{column: 'epoch'}], orderBy: [{column: 'epoch', direction: 'DESC'}], limit: 10}"
                ),
                new QueryPattern(
                    "High-value transactions",
                    "Filter total_output > threshold, ORDER BY total_output DESC",
                    "Example: {table: 'transaction', selectFields: [{column: 'tx_hash'}, {column: 'total_output'}, {column: 'fee'}, {column: 'slot'}], filters: [{column: 'total_output', operator: 'GT', value: 1000000000000}], orderBy: [{column: 'total_output', direction: 'DESC'}], limit: 20}"
                ),
                new QueryPattern(
                    "Failed transactions in time range",
                    "Filter invalid = true, slot BETWEEN start AND end",
                    "Example: {table: 'transaction', selectFields: [{column: 'tx_hash'}, {column: 'slot'}, {column: 'fee'}], filters: [{column: 'invalid', operator: 'EQ', value: true, logicalOp: 'AND'}, {column: 'slot', operator: 'BETWEEN', value: [<start_slot>, <end_slot>], logicalOp: 'AND'}], orderBy: [{column: 'slot', direction: 'DESC'}]}"
                ),
                new QueryPattern(
                    "Smart contract usage trends",
                    "Filter script_size > 0, GROUP BY epoch, COUNT(*)",
                    "Example: {table: 'transaction', selectFields: [{column: 'epoch'}, {column: 'tx_hash', function: 'COUNT', alias: 'script_tx_count'}], filters: [{column: 'script_size', operator: 'GT', value: 0}], groupBy: [{column: 'epoch'}], orderBy: [{column: 'epoch', direction: 'DESC'}]}"
                ),
                new QueryPattern(
                    "Large transaction analysis",
                    "Filter tx_size > threshold, for block space usage",
                    "Example: {table: 'transaction', selectFields: [{column: 'tx_hash'}, {column: 'tx_size'}, {column: 'fee'}, {column: 'epoch'}], filters: [{column: 'tx_size', operator: 'GT', value: 10000}], orderBy: [{column: 'tx_size', direction: 'DESC'}], limit: 50}"
                )
            )
        );
    }

    private TableSchema createBlockSchema() {
        return new TableSchema(
            "block",
            "Blockchain blocks. Use for block production, pool performance analysis, and network capacity monitoring.",
            tableWhitelist.getPrimaryKeys("block"),
            List.of(
                new ColumnSchema("number", "bigint", "Block number (height)"),
                new ColumnSchema("hash", "string", "Block hash"),
                new ColumnSchema("epoch", "integer", "Epoch number"),
                new ColumnSchema("slot", "bigint", "Absolute slot number"),
                new ColumnSchema("epoch_slot", "integer", "Slot within epoch"),
                new ColumnSchema("slot_leader", "string", "Pool ID that produced this block"),
                new ColumnSchema("body_size", "integer", "Block body size in bytes"),
                new ColumnSchema("total_output", "bigint", "Total output in block"),
                new ColumnSchema("total_fees", "bigint", "Total fees in block"),
                new ColumnSchema("no_of_txs", "integer", "Number of transactions"),
                new ColumnSchema("block_time", "bigint", "Block timestamp"),
                new ColumnSchema("prev_hash", "string", "Previous block hash"),
                new ColumnSchema("era", "integer", "Cardano era (Byron=0, Shelley=1, etc.)")
            ),
            List.of(
                new QueryPattern(
                    "Pool performance by epoch",
                    "GROUP BY slot_leader, epoch, COUNT blocks per pool",
                    "Example: {table: 'block', selectFields: [{column: 'slot_leader'}, {column: 'epoch'}, {column: 'number', function: 'COUNT', alias: 'blocks_produced'}], groupBy: [{column: 'slot_leader'}, {column: 'epoch'}], orderBy: [{column: 'blocks_produced', direction: 'DESC'}], limit: 20}"
                ),
                new QueryPattern(
                    "Top block producers by fee collection",
                    "GROUP BY slot_leader, SUM(total_fees), ORDER BY total_fees DESC",
                    "Example: {table: 'block', selectFields: [{column: 'slot_leader'}, {column: 'total_fees', function: 'SUM', alias: 'total_fees_earned'}, {column: 'number', function: 'COUNT', alias: 'block_count'}], groupBy: [{column: 'slot_leader'}], orderBy: [{column: 'total_fees_earned', direction: 'DESC'}], limit: 10}"
                ),
                new QueryPattern(
                    "Block fullness analysis",
                    "Filter body_size > threshold to find heavily loaded blocks",
                    "Example: {table: 'block', selectFields: [{column: 'number'}, {column: 'body_size'}, {column: 'no_of_txs'}, {column: 'slot_leader'}, {column: 'epoch'}], filters: [{column: 'body_size', operator: 'GT', value: 80000}], orderBy: [{column: 'body_size', direction: 'DESC'}], limit: 50}"
                ),
                new QueryPattern(
                    "Recent blocks by specific pool",
                    "Filter slot_leader = pool_id, ORDER BY slot DESC",
                    "Example: {table: 'block', selectFields: [{column: 'number'}, {column: 'slot'}, {column: 'no_of_txs'}, {column: 'total_fees'}], filters: [{column: 'slot_leader', operator: 'EQ', value: '<pool_id>'}], orderBy: [{column: 'slot', direction: 'DESC'}], limit: 20}"
                ),
                new QueryPattern(
                    "Empty blocks detection",
                    "Filter no_of_txs = 0, for slot lottery analysis",
                    "Example: {table: 'block', selectFields: [{column: 'number'}, {column: 'slot_leader'}, {column: 'epoch'}, {column: 'slot'}], filters: [{column: 'no_of_txs', operator: 'EQ', value: 0}], orderBy: [{column: 'slot', direction: 'DESC'}]}"
                ),
                new QueryPattern(
                    "Block production velocity",
                    "GROUP BY epoch, COUNT blocks, AVG time between blocks",
                    "Example: {table: 'block', selectFields: [{column: 'epoch'}, {column: 'number', function: 'COUNT', alias: 'total_blocks'}], groupBy: [{column: 'epoch'}], orderBy: [{column: 'epoch', direction: 'DESC'}]}"
                )
            )
        );
    }

    private TableSchema createTxInputSchema() {
        return new TableSchema(
            "tx_input",
            "Transaction inputs (spent UTXOs). JOIN with address_utxo to find spent/unspent outputs.",
            tableWhitelist.getPrimaryKeys("tx_input"),
            List.of(
                new ColumnSchema("tx_hash", "string", "UTXO transaction hash (from address_utxo)"),
                new ColumnSchema("output_index", "integer", "UTXO output index (from address_utxo)"),
                new ColumnSchema("spent_tx_hash", "string", "Transaction that spent this UTXO"),
                new ColumnSchema("spent_at_block", "bigint", "Block where UTXO was spent"),
                new ColumnSchema("spent_at_slot", "bigint", "Slot when UTXO was spent"),
                new ColumnSchema("spent_epoch", "integer", "Epoch when UTXO was spent")
            ),
            List.of(
                new QueryPattern(
                    "Find when UTXO was spent",
                    "JOIN address_utxo ON tx_hash and output_index",
                    "Use to track UTXO lifecycle and spending patterns"
                )
            )
        );
    }

    private TableSchema createEpochSchema() {
        return new TableSchema(
            "epoch",
            "Epoch-level aggregated data. Use for epoch statistics, network growth analysis, and historical trends. Pre-aggregated for fast queries.",
            tableWhitelist.getPrimaryKeys("epoch"),
            List.of(
                new ColumnSchema("no", "integer", "Epoch number"),
                new ColumnSchema("start_time", "bigint", "Epoch start timestamp"),
                new ColumnSchema("end_time", "bigint", "Epoch end timestamp"),
                new ColumnSchema("block_count", "integer", "Total blocks in epoch"),
                new ColumnSchema("tx_count", "bigint", "Total transactions in epoch"),
                new ColumnSchema("out_sum", "bigint", "Total output in epoch"),
                new ColumnSchema("fees", "bigint", "Total fees in epoch")
            ),
            List.of(
                new QueryPattern(
                    "Network growth over time",
                    "Select epoch metrics, ORDER BY no ASC to see historical progression",
                    "Example: {table: 'epoch', selectFields: [{column: 'no'}, {column: 'tx_count'}, {column: 'fees'}, {column: 'block_count'}], orderBy: [{column: 'no', direction: 'ASC'}], limit: 100}"
                ),
                new QueryPattern(
                    "Recent epoch statistics",
                    "ORDER BY no DESC, LIMIT 10 for most recent epochs",
                    "Example: {table: 'epoch', selectFields: [{column: 'no'}, {column: 'tx_count'}, {column: 'block_count'}, {column: 'fees'}], orderBy: [{column: 'no', direction: 'DESC'}], limit: 10}"
                ),
                new QueryPattern(
                    "High activity epochs",
                    "Filter tx_count > threshold to find busy epochs",
                    "Example: {table: 'epoch', selectFields: [{column: 'no'}, {column: 'tx_count'}, {column: 'fees'}], filters: [{column: 'tx_count', operator: 'GT', value: 100000}], orderBy: [{column: 'tx_count', direction: 'DESC'}], limit: 20}"
                ),
                new QueryPattern(
                    "Fee revenue analysis",
                    "Analyze fees column, compute averages and trends",
                    "Example: {table: 'epoch', selectFields: [{column: 'no'}, {column: 'fees'}, {column: 'tx_count'}], filters: [{column: 'no', operator: 'GTE', value: 450}], orderBy: [{column: 'no', direction: 'ASC'}]}"
                ),
                new QueryPattern(
                    "Network efficiency trends",
                    "Compare tx_count vs fees over time periods",
                    "Example: {table: 'epoch', selectFields: [{column: 'no'}, {column: 'tx_count'}, {column: 'fees'}, {column: 'block_count'}], filters: [{column: 'no', operator: 'BETWEEN', value: [400, 450]}], orderBy: [{column: 'no', direction: 'ASC'}]}"
                )
            )
        );
    }

    private TableSchema createPoolRegistrationSchema() {
        return new TableSchema(
            "pool_registration",
            "Stake pool registration information.",
            tableWhitelist.getPrimaryKeys("pool_registration"),
            List.of(
                new ColumnSchema("pool_id", "string", "Pool ID (bech32 or hex)"),
                new ColumnSchema("vrf_key_hash", "string", "VRF key hash"),
                new ColumnSchema("pledge", "bigint", "Pool pledge in lovelace"),
                new ColumnSchema("cost", "bigint", "Pool fixed cost"),
                new ColumnSchema("margin", "numeric", "Pool margin (0.0 to 1.0)"),
                new ColumnSchema("reward_addr", "string", "Reward address"),
                new ColumnSchema("epoch", "integer", "Registration epoch")
            ),
            List.of()
        );
    }

    private TableSchema createPoolUpdateSchema() {
        return new TableSchema(
            "pool_update",
            "Pool parameter updates.",
            tableWhitelist.getPrimaryKeys("pool_update"),
            List.of(
                new ColumnSchema("id", "bigint", "Update ID"),
                new ColumnSchema("pool_id", "string", "Pool ID"),
                new ColumnSchema("epoch", "integer", "Update epoch"),
                new ColumnSchema("active_epoch", "integer", "When update becomes active")
            ),
            List.of()
        );
    }

    private TableSchema createStakeRegistrationSchema() {
        return new TableSchema(
            "stake_registration",
            "Stake address registrations.",
            tableWhitelist.getPrimaryKeys("stake_registration"),
            List.of(
                new ColumnSchema("addr_id", "string", "Stake address ID"),
                new ColumnSchema("epoch", "integer", "Registration epoch"),
                new ColumnSchema("tx_hash", "string", "Registration transaction"),
                new ColumnSchema("cert_index", "integer", "Certificate index")
            ),
            List.of()
        );
    }

    private TableSchema createDelegationSchema() {
        return new TableSchema(
            "delegation",
            "Stake delegations to pools.",
            tableWhitelist.getPrimaryKeys("delegation"),
            List.of(
                new ColumnSchema("id", "bigint", "Delegation ID"),
                new ColumnSchema("pool_id", "string", "Delegated pool ID"),
                new ColumnSchema("addr_id", "string", "Delegating stake address"),
                new ColumnSchema("epoch", "integer", "Delegation epoch"),
                new ColumnSchema("tx_hash", "string", "Delegation transaction"),
                new ColumnSchema("cert_index", "integer", "Certificate index")
            ),
            List.of()
        );
    }

    private TableSchema createUtxoAssetsFlatSchema() {
        return new TableSchema(
            "utxo_assets_flat",
            "Flattened view of address_utxo with JSONB amounts expanded into rows. " +
            "Each row represents one asset in one UTXO. Includes spent status. " +
            "RECOMMENDED: Use this instead of querying address_utxo.amounts JSONB directly!",
            tableWhitelist.getPrimaryKeys("utxo_assets_flat"),
            List.of(
                new ColumnSchema("tx_hash", "string", "Transaction hash"),
                new ColumnSchema("output_index", "integer", "Output index"),
                new ColumnSchema("owner_addr", "string", "Owner address"),
                new ColumnSchema("owner_stake_addr", "string", "Stake address"),
                new ColumnSchema("owner_payment_credential", "string", "Payment credential"),
                new ColumnSchema("owner_stake_credential", "string", "Stake credential"),
                new ColumnSchema("lovelace_amount", "bigint", "ADA amount in lovelace"),
                new ColumnSchema("epoch", "integer", "Creation epoch"),
                new ColumnSchema("slot", "bigint", "Creation slot"),
                new ColumnSchema("block", "bigint", "Block number"),
                new ColumnSchema("block_hash", "string", "Block hash"),
                new ColumnSchema("block_time", "bigint", "Block timestamp"),
                new ColumnSchema("update_datetime", "timestamp", "Last update time"),
                new ColumnSchema("asset_unit", "string", "Asset unit (policy_id + asset_name hex)"),
                new ColumnSchema("policy_id", "string", "Token policy ID"),
                new ColumnSchema("asset_name", "string", "Token asset name"),
                new ColumnSchema("quantity", "numeric", "Asset quantity"),
                new ColumnSchema("is_spent", "boolean", "Whether UTXO is spent")
            ),
            List.of(
                new QueryPattern(
                    "Find unspent UTXOs for a token",
                    "Filter by policy_id and is_spent = false",
                    "No JOIN needed - is_spent is pre-computed"
                ),
                new QueryPattern(
                    "Get asset holdings by address",
                    "Filter by owner_addr, GROUP BY policy_id, SUM(quantity)",
                    "Simple aggregation without JSONB operators"
                )
            )
        );
    }

    private TableSchema createUtxoAssetsUnspentSchema() {
        return new TableSchema(
            "utxo_assets_unspent",
            "Pre-filtered view of utxo_assets_flat showing only unspent UTXOs. " +
            "MOST COMMON: Use this for current holdings and active balances (95% of use cases).",
            tableWhitelist.getPrimaryKeys("utxo_assets_unspent"),
            List.of(
                new ColumnSchema("tx_hash", "string", "Transaction hash"),
                new ColumnSchema("output_index", "integer", "Output index"),
                new ColumnSchema("owner_addr", "string", "Owner address"),
                new ColumnSchema("owner_stake_addr", "string", "Stake address"),
                new ColumnSchema("owner_payment_credential", "string", "Payment credential"),
                new ColumnSchema("owner_stake_credential", "string", "Stake credential"),
                new ColumnSchema("lovelace_amount", "bigint", "ADA amount in lovelace"),
                new ColumnSchema("epoch", "integer", "Creation epoch"),
                new ColumnSchema("slot", "bigint", "Creation slot"),
                new ColumnSchema("block", "bigint", "Block number"),
                new ColumnSchema("block_hash", "string", "Block hash"),
                new ColumnSchema("block_time", "bigint", "Block timestamp"),
                new ColumnSchema("update_datetime", "timestamp", "Last update time"),
                new ColumnSchema("asset_unit", "string", "Asset unit (policy_id + asset_name hex)"),
                new ColumnSchema("policy_id", "string", "Token policy ID"),
                new ColumnSchema("asset_name", "string", "Token asset name"),
                new ColumnSchema("quantity", "numeric", "Asset quantity")
            ),
            List.of(
                new QueryPattern(
                    "Current token holdings",
                    "Filter by policy_id or asset_name",
                    "Already filtered to unspent - no is_spent check needed"
                ),
                new QueryPattern(
                    "Token holders for an address",
                    "Filter by owner_addr, SELECT DISTINCT policy_id",
                    "Shows all tokens currently held by address"
                )
            )
        );
    }

    private TableSchema createTokenHolderSummarySchema() {
        return new TableSchema(
            "token_holder_summary",
            "Pre-aggregated statistics per token including holder counts, supply, and quantity distribution. " +
            "FASTEST: Use this for token discovery and ranking queries!",
            tableWhitelist.getPrimaryKeys("token_holder_summary"),
            List.of(
                new ColumnSchema("policy_id", "string", "Token policy ID"),
                new ColumnSchema("asset_name", "string", "Token asset name"),
                new ColumnSchema("asset_unit", "string", "Asset unit (policy_id + asset_name hex)"),
                new ColumnSchema("holder_count", "integer", "Number of unique holders"),
                new ColumnSchema("total_supply", "numeric", "Total token supply (sum of all quantities)"),
                new ColumnSchema("utxo_count", "integer", "Number of UTXOs containing this token"),
                new ColumnSchema("min_quantity", "numeric", "Minimum quantity held in any UTXO"),
                new ColumnSchema("max_quantity", "numeric", "Maximum quantity held in any UTXO"),
                new ColumnSchema("avg_quantity", "numeric", "Average quantity per UTXO"),
                new ColumnSchema("median_quantity", "numeric", "Median quantity per UTXO")
            ),
            List.of(
                new QueryPattern(
                    "Find tokens with N+ holders",
                    "Filter holder_count >= N, ORDER BY holder_count DESC",
                    "Instant results - no aggregation needed"
                ),
                new QueryPattern(
                    "Find widely distributed tokens",
                    "Filter holder_count >= 1000 AND avg_quantity < 1000",
                    "Identifies tokens with many holders and even distribution"
                )
            )
        );
    }

    private TableSchema createAddressLovelaceBalanceSchema() {
        return new TableSchema(
            "address_lovelace_balance",
            "Simplified view for ADA-only balance queries. " +
            "Includes spent status but no asset details. No JSONB processing overhead.",
            tableWhitelist.getPrimaryKeys("address_lovelace_balance"),
            List.of(
                new ColumnSchema("owner_addr", "string", "Owner address"),
                new ColumnSchema("owner_stake_addr", "string", "Stake address"),
                new ColumnSchema("owner_payment_credential", "string", "Payment credential"),
                new ColumnSchema("owner_stake_credential", "string", "Stake credential"),
                new ColumnSchema("tx_hash", "string", "Transaction hash"),
                new ColumnSchema("output_index", "integer", "Output index"),
                new ColumnSchema("lovelace_amount", "bigint", "ADA amount in lovelace"),
                new ColumnSchema("epoch", "integer", "Creation epoch"),
                new ColumnSchema("slot", "bigint", "Creation slot"),
                new ColumnSchema("block", "bigint", "Block number"),
                new ColumnSchema("block_time", "bigint", "Block timestamp"),
                new ColumnSchema("update_datetime", "timestamp", "Last update time"),
                new ColumnSchema("is_spent", "boolean", "Whether UTXO is spent")
            ),
            List.of(
                new QueryPattern(
                    "Current ADA balance",
                    "Filter owner_addr and is_spent = false, SUM(lovelace_amount)",
                    "Fast ADA balance without JSONB overhead"
                ),
                new QueryPattern(
                    "Balance history by epoch",
                    "Filter owner_addr and is_spent = false, GROUP BY epoch",
                    "Track ADA balance changes over time"
                )
            )
        );
    }

    private TableSchema createAssetsSchema() {
        return new TableSchema(
            "assets",
            "Token mint and burn events ONLY. Records when tokens are created or destroyed. " +
            "⚠️ IMPORTANT: This table does NOT track token transfers/movements! " +
            "USE THIS FOR: Minting activity, burn events, token supply changes, new token discovery. " +
            "DO NOT USE FOR: Token transfers (use utxo_assets_flat), current holdings (use utxo_assets_unspent), or holder stats (use token_holder_summary).",
            tableWhitelist.getPrimaryKeys("assets"),
            List.of(
                new ColumnSchema("id", "uuid", "Unique mint/burn event ID"),
                new ColumnSchema("slot", "bigint", "Slot when token was minted/burned"),
                new ColumnSchema("tx_hash", "string", "Transaction hash of mint/burn"),
                new ColumnSchema("policy", "string", "Token policy ID (hex)"),
                new ColumnSchema("asset_name", "string", "Token asset name (hex)"),
                new ColumnSchema("unit", "string", "Asset unit (policy + asset_name concatenated) - USE THIS for token registry lookups!"),
                new ColumnSchema("fingerprint", "string", "CIP-14 asset fingerprint (asset1...)"),
                new ColumnSchema("quantity", "bigint", "Amount minted (+) or burned (-). Negative values indicate burning."),
                new ColumnSchema("mint_type", "string", "MINT or BURN"),
                new ColumnSchema("block", "bigint", "Block number"),
                new ColumnSchema("block_time", "bigint", "Block timestamp (Unix epoch)"),
                new ColumnSchema("update_datetime", "timestamp", "Last update time")
            ),
            List.of(
                new QueryPattern(
                    "Find tokens minted in last hour",
                    "Filter slot > (current_slot - 7200), quantity > 0, GROUP BY policy, asset_name, unit, COUNT(*)",
                    "Example: {table: 'assets', selectFields: [{column: 'policy'}, {column: 'asset_name'}, {column: 'unit'}, {column: 'tx_hash', function: 'COUNT', alias: 'mint_count'}], filters: [{column: 'slot', operator: 'GT', value: <slot_1hr_ago>, logicalOp: 'AND'}, {column: 'quantity', operator: 'GT', value: 0, logicalOp: 'AND'}], groupBy: [{column: 'policy'}, {column: 'asset_name'}, {column: 'unit'}], orderBy: [{column: 'mint_count', direction: 'DESC'}], limit: 20}"
                ),
                new QueryPattern(
                    "Most minted tokens by quantity",
                    "Filter quantity > 0, GROUP BY unit, SUM(quantity), ORDER BY sum DESC",
                    "Example: {table: 'assets', selectFields: [{column: 'policy'}, {column: 'asset_name'}, {column: 'unit'}, {column: 'quantity', function: 'SUM', alias: 'total_minted'}], filters: [{column: 'quantity', operator: 'GT', value: 0}], groupBy: [{column: 'policy'}, {column: 'asset_name'}, {column: 'unit'}], orderBy: [{column: 'total_minted', direction: 'DESC'}], limit: 20}"
                ),
                new QueryPattern(
                    "Track token supply changes",
                    "GROUP BY unit, SUM(quantity) for net supply (positive = net minting, negative = net burning)",
                    "Example: {table: 'assets', selectFields: [{column: 'unit'}, {column: 'quantity', function: 'SUM', alias: 'net_supply'}], groupBy: [{column: 'unit'}], orderBy: [{column: 'net_supply', direction: 'DESC'}]}"
                ),
                new QueryPattern(
                    "Burn events for specific policy",
                    "Filter policy = '<policy_id>' AND quantity < 0",
                    "Example: {table: 'assets', selectFields: [{column: 'asset_name'}, {column: 'unit'}, {column: 'quantity'}, {column: 'slot'}], filters: [{column: 'policy', operator: 'EQ', value: '<policy_hex>', logicalOp: 'AND'}, {column: 'quantity', operator: 'LT', value: 0, logicalOp: 'AND'}], orderBy: [{column: 'slot', direction: 'DESC'}]}"
                ),
                new QueryPattern(
                    "Minting velocity analysis",
                    "COUNT mints per time period, GROUP BY epoch or time ranges",
                    "Shows how fast tokens are being minted over time"
                )
            )
        );
    }
}
