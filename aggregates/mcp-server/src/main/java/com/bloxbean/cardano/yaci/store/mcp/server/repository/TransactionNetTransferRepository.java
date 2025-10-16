package com.bloxbean.cardano.yaci.store.mcp.server.repository;

import com.bloxbean.cardano.yaci.store.mcp.server.model.AddressNetActivitySummary;
import com.bloxbean.cardano.yaci.store.mcp.server.model.PagedResult;
import com.bloxbean.cardano.yaci.store.mcp.server.model.PaginationCursor;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TransactionNetTransferDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Repository for batch net transfer queries using JdbcTemplate.
 * Uses CTEs for optimized SQL aggregation.
 */
@Repository
@RequiredArgsConstructor
public class TransactionNetTransferRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Get net transfers for transactions in slot range.
     * Returns one row per address per transaction.
     *
     * @param startSlot Start slot (inclusive)
     * @param endSlot End slot (inclusive)
     * @param limit Maximum number of transactions to process
     * @param minNetLovelace Minimum absolute net lovelace to include (filters noise)
     * @param sortBy Sort order: "net_amount_desc", "net_amount_asc", "slot_desc", "slot_asc"
     * @return List of net transfer DTOs
     */
    public List<TransactionNetTransferDto> getNetTransfersBatch(
            Long startSlot,
            Long endSlot,
            Integer limit,
            Long minNetLovelace,
            String sortBy) {
        String sql = """
            WITH target_txs AS (
                -- Get transactions in slot range
                SELECT
                    tx_hash,
                    slot,
                    epoch,
                    block,
                    fee,
                    inputs,
                    outputs
                FROM transaction
                WHERE slot >= ? AND slot <= ?
                AND invalid = false
                ORDER BY slot DESC
                LIMIT ?
            ),
            inputs_expanded AS (
                -- Expand inputs JSONB array to get individual input references
                SELECT
                    t.tx_hash as spending_tx_hash,
                    t.slot,
                    t.epoch,
                    t.block,
                    t.fee,
                    inp->>'tx_hash' as input_tx_hash,
                    (inp->>'output_index')::int as input_output_index
                FROM target_txs t
                CROSS JOIN LATERAL jsonb_array_elements(t.inputs) AS inp
            ),
            inputs_agg AS (
                -- Join with address_utxo to get input addresses and amounts
                SELECT
                    ie.spending_tx_hash as tx_hash,
                    au.owner_addr as address,
                    au.owner_stake_addr as stake_address,
                    SUM(au.lovelace_amount) as input_lovelace
                FROM inputs_expanded ie
                INNER JOIN address_utxo au
                    ON au.tx_hash = ie.input_tx_hash
                    AND au.output_index = ie.input_output_index
                GROUP BY ie.spending_tx_hash, au.owner_addr, au.owner_stake_addr
            ),
            outputs_expanded AS (
                -- Expand outputs JSONB array (though outputs reference the same tx)
                SELECT
                    t.tx_hash,
                    t.slot,
                    t.epoch,
                    t.block,
                    t.fee,
                    out->>'tx_hash' as output_tx_hash,
                    (out->>'output_index')::int as output_index
                FROM target_txs t
                CROSS JOIN LATERAL jsonb_array_elements(t.outputs) AS out
            ),
            outputs_agg AS (
                -- Join with address_utxo to get output addresses and amounts
                SELECT
                    oe.tx_hash,
                    au.owner_addr as address,
                    au.owner_stake_addr as stake_address,
                    SUM(au.lovelace_amount) as output_lovelace
                FROM outputs_expanded oe
                INNER JOIN address_utxo au
                    ON au.tx_hash = oe.output_tx_hash
                    AND au.output_index = oe.output_index
                GROUP BY oe.tx_hash, au.owner_addr, au.owner_stake_addr
            ),
            -- Get all unique addresses involved in the transaction
            all_addresses AS (
                SELECT tx_hash, address, stake_address FROM inputs_agg
                UNION
                SELECT tx_hash, address, stake_address FROM outputs_agg
            )
            -- Combine and calculate net transfers
            SELECT
                t.tx_hash,
                t.slot,
                t.epoch,
                t.block,
                t.fee,
                a.address,
                a.stake_address,
                COALESCE(o.output_lovelace, 0) - COALESCE(i.input_lovelace, 0) as net_lovelace,
                COALESCE(i.input_lovelace, 0) as input_lovelace,
                COALESCE(o.output_lovelace, 0) as output_lovelace,
                CASE
                    WHEN COALESCE(o.output_lovelace, 0) - COALESCE(i.input_lovelace, 0) < 0 THEN true
                    ELSE false
                END as is_sender,
                CASE
                    WHEN COALESCE(o.output_lovelace, 0) - COALESCE(i.input_lovelace, 0) > 0 THEN true
                    ELSE false
                END as is_receiver
            FROM target_txs t
            INNER JOIN all_addresses a ON t.tx_hash = a.tx_hash
            LEFT JOIN inputs_agg i ON t.tx_hash = i.tx_hash AND a.address = i.address
            LEFT JOIN outputs_agg o ON t.tx_hash = o.tx_hash AND a.address = o.address
            WHERE ABS(COALESCE(o.output_lovelace, 0) - COALESCE(i.input_lovelace, 0)) >= ?
            ORDER BY
                CASE WHEN ? = 'net_amount_desc' THEN ABS(COALESCE(o.output_lovelace, 0) - COALESCE(i.input_lovelace, 0)) END DESC NULLS LAST,
                CASE WHEN ? = 'net_amount_asc' THEN ABS(COALESCE(o.output_lovelace, 0) - COALESCE(i.input_lovelace, 0)) END ASC NULLS LAST,
                CASE WHEN ? = 'slot_desc' THEN t.slot END DESC NULLS LAST,
                CASE WHEN ? = 'slot_asc' THEN t.slot END ASC NULLS LAST,
                t.slot DESC,
                t.tx_hash,
                address
            """;

        return jdbcTemplate.query(sql, this::mapRowToDto,
                startSlot, endSlot, limit,
                minNetLovelace,
                sortBy, sortBy, sortBy, sortBy);
    }

    /**
     * Map SQL ResultSet row to TransactionNetTransferDto
     */
    private TransactionNetTransferDto mapRowToDto(ResultSet rs, int rowNum) throws SQLException {
        return TransactionNetTransferDto.builder()
                .txHash(rs.getString("tx_hash"))
                .slot(rs.getLong("slot"))
                .epoch(rs.getInt("epoch"))
                .block(rs.getLong("block"))
                .fee(new BigInteger(rs.getString("fee")))
                .address(rs.getString("address"))
                .stakeAddress(rs.getString("stake_address"))
                .netLovelace(new BigInteger(rs.getString("net_lovelace")))
                .inputLovelace(new BigInteger(rs.getString("input_lovelace")))
                .outputLovelace(new BigInteger(rs.getString("output_lovelace")))
                .isSender(rs.getBoolean("is_sender"))
                .isReceiver(rs.getBoolean("is_receiver"))
                .build();
    }

    /**
     * Get aggregated net activity per address across multiple transactions.
     * Returns one row per address with totals across all transactions in range.
     *
     * This is much more efficient for large datasets than per-transaction analysis
     * because it returns O(unique addresses) instead of O(transactions × addresses).
     *
     * @param startSlot Start slot (inclusive)
     * @param endSlot End slot (inclusive)
     * @param maxTransactions Maximum number of transactions to analyze
     * @param minTotalNetLovelace Minimum absolute net lovelace per address to include
     * @param sortBy Sort order
     * @param page Page number (0-based)
     * @param pageSize Results per page
     * @return List of address activity summaries
     */
    public List<AddressNetActivitySummary> getAddressNetActivityAggregated(
            Long startSlot,
            Long endSlot,
            Integer maxTransactions,
            Long minTotalNetLovelace,
            String sortBy,
            Integer page,
            Integer pageSize) {

        String sql = """
            WITH target_txs AS (
                SELECT tx_hash, slot, epoch, block, fee, inputs, outputs
                FROM transaction
                WHERE slot >= ? AND slot <= ?
                AND invalid = false
                ORDER BY slot DESC
                LIMIT ?
            ),
            inputs_expanded AS (
                SELECT
                    t.tx_hash as spending_tx_hash,
                    t.slot,
                    inp->>'tx_hash' as input_tx_hash,
                    (inp->>'output_index')::int as input_output_index
                FROM target_txs t
                CROSS JOIN LATERAL jsonb_array_elements(t.inputs) AS inp
            ),
            inputs_agg AS (
                SELECT
                    ie.spending_tx_hash as tx_hash,
                    ie.slot,
                    au.owner_addr as address,
                    au.owner_stake_addr as stake_address,
                    SUM(au.lovelace_amount) as input_lovelace
                FROM inputs_expanded ie
                INNER JOIN address_utxo au
                    ON au.tx_hash = ie.input_tx_hash
                    AND au.output_index = ie.input_output_index
                GROUP BY ie.spending_tx_hash, ie.slot, au.owner_addr, au.owner_stake_addr
            ),
            outputs_expanded AS (
                SELECT
                    t.tx_hash,
                    t.slot,
                    out->>'tx_hash' as output_tx_hash,
                    (out->>'output_index')::int as output_index
                FROM target_txs t
                CROSS JOIN LATERAL jsonb_array_elements(t.outputs) AS out
            ),
            outputs_agg AS (
                SELECT
                    oe.tx_hash,
                    oe.slot,
                    au.owner_addr as address,
                    au.owner_stake_addr as stake_address,
                    SUM(au.lovelace_amount) as output_lovelace
                FROM outputs_expanded oe
                INNER JOIN address_utxo au
                    ON au.tx_hash = oe.output_tx_hash
                    AND au.output_index = oe.output_index
                GROUP BY oe.tx_hash, oe.slot, au.owner_addr, au.owner_stake_addr
            ),
            all_activities AS (
                SELECT tx_hash, slot, address, stake_address, input_lovelace, 0::bigint as output_lovelace FROM inputs_agg
                UNION ALL
                SELECT tx_hash, slot, address, stake_address, 0::bigint as input_lovelace, output_lovelace FROM outputs_agg
            ),
            net_per_tx_address AS (
                SELECT
                    address,
                    stake_address,
                    tx_hash,
                    slot,
                    SUM(output_lovelace) - SUM(input_lovelace) as net_lovelace
                FROM all_activities
                GROUP BY address, stake_address, tx_hash, slot
            ),
            address_aggregates AS (
                SELECT
                    address,
                    stake_address,
                    COUNT(DISTINCT tx_hash) as transaction_count,
                    SUM(net_lovelace) as total_net_lovelace,
                    SUM(CASE WHEN net_lovelace < 0 THEN ABS(net_lovelace) ELSE 0 END) as total_sent,
                    SUM(CASE WHEN net_lovelace > 0 THEN net_lovelace ELSE 0 END) as total_received,
                    ARRAY_AGG(DISTINCT tx_hash) as involved_transactions,
                    MIN(slot) as first_slot,
                    MAX(slot) as last_slot
                FROM net_per_tx_address
                GROUP BY address, stake_address
                HAVING ABS(SUM(net_lovelace)) >= ?
            )
            SELECT
                address,
                stake_address,
                transaction_count,
                total_net_lovelace,
                total_sent,
                total_received,
                involved_transactions,
                first_slot,
                last_slot,
                CASE
                    WHEN total_net_lovelace < 0 THEN 'NET_SENDER'
                    WHEN total_net_lovelace > 0 THEN 'NET_RECEIVER'
                    WHEN transaction_count > 10 THEN 'HIGH_FREQUENCY'
                    ELSE 'BALANCED'
                END as classification
            FROM address_aggregates
            ORDER BY
                CASE WHEN ? = 'total_net_desc' THEN ABS(total_net_lovelace) END DESC NULLS LAST,
                CASE WHEN ? = 'total_net_asc' THEN ABS(total_net_lovelace) END ASC NULLS LAST,
                CASE WHEN ? = 'tx_count_desc' THEN transaction_count END DESC NULLS LAST,
                CASE WHEN ? = 'total_sent_desc' THEN total_sent END DESC NULLS LAST,
                CASE WHEN ? = 'total_received_desc' THEN total_received END DESC NULLS LAST,
                address
            LIMIT ?
            OFFSET ?
            """;

        int offset = page * pageSize;
        return jdbcTemplate.query(sql, this::mapRowToAddressSummary,
                startSlot, endSlot, maxTransactions,
                minTotalNetLovelace,
                sortBy, sortBy, sortBy, sortBy, sortBy,
                pageSize, offset);
    }

    /**
     * Map SQL ResultSet row to AddressNetActivitySummary
     */
    private AddressNetActivitySummary mapRowToAddressSummary(ResultSet rs, int rowNum) throws SQLException {
        // Extract ARRAY of tx hashes
        Array txArray = rs.getArray("involved_transactions");
        List<String> txList = txArray != null
                ? Arrays.asList((String[]) txArray.getArray())
                : Collections.emptyList();

        return AddressNetActivitySummary.builder()
                .address(rs.getString("address"))
                .stakeAddress(rs.getString("stake_address"))
                .transactionCount(rs.getInt("transaction_count"))
                .totalNetLovelace(new BigInteger(rs.getString("total_net_lovelace")))
                .totalSent(new BigInteger(rs.getString("total_sent")))
                .totalReceived(new BigInteger(rs.getString("total_received")))
                .involvedTransactions(txList)
                .firstSlot(rs.getLong("first_slot"))
                .lastSlot(rs.getLong("last_slot"))
                .classification(AddressNetActivitySummary.AddressClassification.valueOf(rs.getString("classification")))
                .build();
    }

    /**
     * Get net transfers with keyset-based pagination for stable, performant pagination.
     *
     * Uses cursor-based (keyset) pagination instead of OFFSET, providing:
     * - O(log N) performance (vs O(N) for OFFSET)
     * - Stable results (new data doesn't shift pages)
     * - Efficient for deep pagination
     *
     * @param startSlot Start slot (inclusive)
     * @param endSlot End slot (inclusive)
     * @param pageSize Number of results per page
     * @param cursor Continuation cursor (null for first page)
     * @param minNetLovelace Minimum absolute net lovelace
     * @param sortBy Sort order (only slot_desc supported for keyset pagination)
     * @return Paged result with cursor for next page
     */
    public PagedResult<TransactionNetTransferDto> getNetTransfersBatchPaged(
            Long startSlot,
            Long endSlot,
            Integer pageSize,
            PaginationCursor cursor,
            Long minNetLovelace,
            String sortBy) {

        // For keyset pagination, we need consistent sort order
        // Currently only supports slot DESC (most common use case)
        if (!"slot_desc".equals(sortBy)) {
            sortBy = "slot_desc";  // Default to slot_desc for cursor stability
        }

        String sql = """
            WITH target_txs AS (
                SELECT
                    tx_hash,
                    slot,
                    epoch,
                    block,
                    fee,
                    inputs,
                    outputs
                FROM transaction
                WHERE slot >= ? AND slot <= ?
                AND invalid = false
                AND (? IS NULL OR (slot < ? OR (slot = ? AND tx_hash < ?)))
                ORDER BY slot DESC, tx_hash DESC
                LIMIT ?
            ),
            inputs_expanded AS (
                SELECT
                    t.tx_hash as spending_tx_hash,
                    t.slot,
                    t.epoch,
                    t.block,
                    t.fee,
                    inp->>'tx_hash' as input_tx_hash,
                    (inp->>'output_index')::int as input_output_index
                FROM target_txs t
                CROSS JOIN LATERAL jsonb_array_elements(t.inputs) AS inp
            ),
            inputs_agg AS (
                SELECT
                    ie.spending_tx_hash as tx_hash,
                    au.owner_addr as address,
                    au.owner_stake_addr as stake_address,
                    SUM(au.lovelace_amount) as input_lovelace
                FROM inputs_expanded ie
                INNER JOIN address_utxo au
                    ON au.tx_hash = ie.input_tx_hash
                    AND au.output_index = ie.input_output_index
                GROUP BY ie.spending_tx_hash, au.owner_addr, au.owner_stake_addr
            ),
            outputs_expanded AS (
                SELECT
                    t.tx_hash,
                    t.slot,
                    t.epoch,
                    t.block,
                    t.fee,
                    out->>'tx_hash' as output_tx_hash,
                    (out->>'output_index')::int as output_index
                FROM target_txs t
                CROSS JOIN LATERAL jsonb_array_elements(t.outputs) AS out
            ),
            outputs_agg AS (
                SELECT
                    oe.tx_hash,
                    au.owner_addr as address,
                    au.owner_stake_addr as stake_address,
                    SUM(au.lovelace_amount) as output_lovelace
                FROM outputs_expanded oe
                INNER JOIN address_utxo au
                    ON au.tx_hash = oe.output_tx_hash
                    AND au.output_index = oe.output_index
                GROUP BY oe.tx_hash, au.owner_addr, au.owner_stake_addr
            ),
            all_addresses AS (
                SELECT tx_hash, address, stake_address FROM inputs_agg
                UNION
                SELECT tx_hash, address, stake_address FROM outputs_agg
            )
            SELECT
                t.tx_hash,
                t.slot,
                t.epoch,
                t.block,
                t.fee,
                a.address,
                a.stake_address,
                COALESCE(o.output_lovelace, 0) - COALESCE(i.input_lovelace, 0) as net_lovelace,
                COALESCE(i.input_lovelace, 0) as input_lovelace,
                COALESCE(o.output_lovelace, 0) as output_lovelace,
                CASE
                    WHEN COALESCE(o.output_lovelace, 0) - COALESCE(i.input_lovelace, 0) < 0 THEN true
                    ELSE false
                END as is_sender,
                CASE
                    WHEN COALESCE(o.output_lovelace, 0) - COALESCE(i.input_lovelace, 0) > 0 THEN true
                    ELSE false
                END as is_receiver
            FROM target_txs t
            INNER JOIN all_addresses a ON t.tx_hash = a.tx_hash
            LEFT JOIN inputs_agg i ON t.tx_hash = i.tx_hash AND a.address = i.address
            LEFT JOIN outputs_agg o ON t.tx_hash = o.tx_hash AND a.address = o.address
            WHERE ABS(COALESCE(o.output_lovelace, 0) - COALESCE(i.input_lovelace, 0)) >= ?
            ORDER BY t.slot DESC, t.tx_hash DESC, address
            """;

        // Prepare cursor parameters
        Long cursorSlot = cursor != null ? cursor.getLastSlot() : null;
        String cursorTxHash = cursor != null ? cursor.getLastTxHash() : null;

        // Fetch pageSize + 1 to check if more results exist
        int fetchSize = pageSize + 1;

        List<TransactionNetTransferDto> results = jdbcTemplate.query(sql, this::mapRowToDto,
                startSlot, endSlot,
                cursorSlot, cursorSlot, cursorSlot, cursorTxHash,
                fetchSize,
                minNetLovelace);

        // Check if more results available
        boolean hasMore = results.size() > pageSize;
        if (hasMore) {
            results = results.subList(0, pageSize);  // Trim to requested page size
        }

        // Build next cursor
        PaginationCursor nextCursor = null;
        if (hasMore && !results.isEmpty()) {
            TransactionNetTransferDto last = results.get(results.size() - 1);
            nextCursor = PaginationCursor.builder()
                    .lastSlot(last.getSlot())
                    .lastTxHash(last.getTxHash())
                    .lastAddress(last.getAddress())
                    .totalProcessed((cursor != null ? cursor.getTotalProcessed() : 0) + pageSize)
                    .build();
        }

        int totalProcessed = (cursor != null ? cursor.getTotalProcessed() : 0) + results.size();

        return PagedResult.<TransactionNetTransferDto>builder()
                .results(results)
                .pageSize(pageSize)
                .nextCursor(nextCursor != null ? nextCursor.encode() : null)
                .hasMore(hasMore)
                .totalProcessed(totalProcessed)
                .build();
    }
}
