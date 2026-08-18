package com.bloxbean.cardano.yaci.store.analytics.query.controller;

import com.bloxbean.cardano.yaci.store.analytics.query.executor.AnalyticsQueryExecutor;
import com.bloxbean.cardano.yaci.store.analytics.query.model.SchemaOverview;
import com.bloxbean.cardano.yaci.store.analytics.query.model.TableDescription;
import com.bloxbean.cardano.yaci.store.analytics.query.service.AnalyticsBlockQueryService;
import com.bloxbean.cardano.yaci.store.analytics.query.service.AnalyticsSchemaService;
import com.bloxbean.cardano.yaci.store.analytics.query.service.AnalyticsTransactionQueryService;
import com.bloxbean.cardano.yaci.store.analytics.query.service.SqlValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API of the analytics query layer ({@code /api/v1/analytics/query/*}): schema discovery,
 * a few pre-built statistics, and ad-hoc read-only SQL over the exported analytics data
 * (DuckLake / Parquet, optionally federated with live PostgreSQL). The operations API lives
 * next to it under {@code /api/v1/analytics/admin/*} (analytics-store).
 *
 * <p>Requires {@code yaci.store.analytics.query.enabled=true} and can be disabled independently
 * of the MCP tools by setting {@code yaci.store.analytics.query.rest-api-enabled=false}.
 * This allows production deployments to restrict analytics access to the MCP
 * interface only, while keeping the REST API available in development.</p>
 */
@RestController
@RequestMapping("/api/v1/analytics/query")
@Tag(name = "Analytics Query API",
        description = "Read-only analytics over the exported blockchain data (DuckLake/Parquet exports of the "
                + "yaci-store tables, queried in-process with DuckDB). Start with GET /schema to discover the "
                + "available tables and columns, use the pre-built endpoints for common statistics, or run "
                + "ad-hoc SELECT queries with POST /sql. Data is as of the last completed export (see "
                + "'dataAsOf' in /schema) unless live PostgreSQL federation is enabled. Requires "
                + "yaci.store.analytics.query.enabled=true and yaci.store.analytics.query.rest-api-enabled=true.")
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = {"yaci.store.analytics.query.enabled", "yaci.store.analytics.query.rest-api-enabled"},
        havingValue = "true"
)
public class AnalyticsQueryController {

    private static final String EPOCH_RANGE_NOTE =
            "Epochs are inclusive on both ends; keep ranges small (a few epochs) for fast responses.";

    private final AnalyticsBlockQueryService blockQueryService;
    private final AnalyticsTransactionQueryService txQueryService;
    private final AnalyticsQueryExecutor queryExecutor;
    private final AnalyticsSchemaService schemaService;

    // --- Schema discovery endpoints ---

    @Operation(summary = "List analytics tables",
            description = "Overview of every table available to the analytics SQL engine: name, description, "
                    + "row count, partitioning (DAILY on 'date' or EPOCH on 'epoch') and covered date range, "
                    + "plus global context (SQL dialect, data freshness 'dataAsOf', query hints). "
                    + "Table names mirror the yaci-store PostgreSQL tables (block, transaction, address_utxo, "
                    + "tx_input, epoch_stake, ...). Use it as the entry point before writing SQL.")
    @ApiResponse(responseCode = "200", description = "Schema overview")
    @GetMapping("/schema")
    public SchemaOverview listTables() {
        return schemaService.listTables();
    }

    @Operation(summary = "Describe an analytics table",
            description = "Columns and DuckDB types of one table, with row count, partition column and "
                    + "table-specific query hints (units such as lovelace, JSON columns to avoid, ...).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Table description"),
            @ApiResponse(responseCode = "400", description = "Unknown table", content = @Content(
                    examples = @ExampleObject(value = "{\"error\":\"Unknown table: transactions. Use analytics-list-tables to see available tables.\"}")))
    })
    @GetMapping("/schema/{tableName}")
    public TableDescription describeTable(
            @Parameter(description = "Table name as listed by GET /schema, e.g. 'transaction'", example = "transaction")
            @PathVariable String tableName) {
        return schemaService.describeTable(tableName);
    }

    // --- Pre-built query endpoints ---

    @Operation(summary = "Block statistics per epoch",
            description = "Network-wide block statistics for each epoch in the range: block_count, total_transactions, "
                    + "total_fees (lovelace), avg_txs_per_block, avg_block_size (bytes) and unique_pool_count. "
                    + EPOCH_RANGE_NOTE)
    @ApiResponse(responseCode = "200", description = "One row per epoch, ordered by epoch")
    @GetMapping("/blocks/epoch-stats")
    public List<Map<String, Object>> getEpochBlockStats(
            @Parameter(description = "First epoch (inclusive)", example = "300") @RequestParam int startEpoch,
            @Parameter(description = "Last epoch (inclusive)", example = "302") @RequestParam int endEpoch) {
        return blockQueryService.getEpochBlockStatistics(startEpoch, endEpoch);
    }

    @Operation(summary = "Block production of one stake pool per epoch",
            description = "For the given pool and epoch range: blocks_produced, total_transactions, total_fees "
                    + "(lovelace) and avg_txs_per_block per epoch. The pool is identified by its pool id hash "
                    + "(56 hex characters, as stored in block.slot_leader), not the bech32 'pool1...' form. "
                    + EPOCH_RANGE_NOTE)
    @ApiResponse(responseCode = "200", description = "One row per epoch in which the pool produced blocks (may be empty)")
    @GetMapping("/blocks/pool-production")
    public List<Map<String, Object>> getPoolBlockProduction(
            @Parameter(description = "Pool id hash (hex, 56 chars) as in block.slot_leader",
                    example = "85eb86b47f9f8e736e729c0176ba45cf9778e238b4452b7a6428a6e9")
            @RequestParam String poolId,
            @Parameter(description = "First epoch (inclusive)", example = "300") @RequestParam int startEpoch,
            @Parameter(description = "Last epoch (inclusive)", example = "302") @RequestParam int endEpoch) {
        return blockQueryService.getPoolBlockProductionStats(poolId, startEpoch, endEpoch);
    }

    @Operation(summary = "Transaction statistics per epoch",
            description = "For each epoch in the range: tx_count, total_fees and avg_fee (lovelace), block_count, "
                    + "valid_tx_count and invalid_tx_count (phase-2 validation failures). " + EPOCH_RANGE_NOTE)
    @ApiResponse(responseCode = "200", description = "One row per epoch, ordered by epoch")
    @GetMapping("/transactions/epoch-stats")
    public List<Map<String, Object>> getEpochTxStats(
            @Parameter(description = "First epoch (inclusive)", example = "300") @RequestParam int startEpoch,
            @Parameter(description = "Last epoch (inclusive)", example = "302") @RequestParam int endEpoch) {
        return txQueryService.getEpochTransactionStatistics(startEpoch, endEpoch);
    }

    @Operation(summary = "Per-block transaction statistics",
            description = "Blocks in the epoch range with their transaction load: block_number, block_hash, epoch, "
                    + "slot, tx_count, total_fees, avg_fee, valid_tx_count, invalid_tx_count. Use minTxCount to "
                    + "keep only busy blocks. Results are capped at 10,000 rows. " + EPOCH_RANGE_NOTE)
    @ApiResponse(responseCode = "200", description = "One row per block")
    @GetMapping("/transactions/block-stats")
    public List<Map<String, Object>> getBlockTxStats(
            @Parameter(description = "First epoch (inclusive)", example = "300") @RequestParam int startEpoch,
            @Parameter(description = "Last epoch (inclusive)", example = "300") @RequestParam int endEpoch,
            @Parameter(description = "Only blocks with at least this many transactions (default 0 = all)", example = "50")
            @RequestParam(defaultValue = "0") int minTxCount) {
        return txQueryService.getBlockTransactionStatistics(startEpoch, endEpoch, minTxCount);
    }

    @Operation(summary = "Transaction fee distribution per epoch",
            description = "Fee percentiles per epoch (lovelace): min_fee, max_fee, avg_fee, median_fee, p25_fee, "
                    + "p75_fee, p90_fee, p95_fee, p99_fee plus tx_count and total_fees. " + EPOCH_RANGE_NOTE)
    @ApiResponse(responseCode = "200", description = "One row per epoch, ordered by epoch")
    @GetMapping("/transactions/fee-distribution")
    public List<Map<String, Object>> getFeeDistribution(
            @Parameter(description = "First epoch (inclusive)", example = "300") @RequestParam int startEpoch,
            @Parameter(description = "Last epoch (inclusive)", example = "301") @RequestParam int endEpoch) {
        return txQueryService.getFeeDistributionAnalysis(startEpoch, endEpoch);
    }

    @Operation(summary = "Transaction count",
            description = "Number of transactions, either overall (no parameters) or within an inclusive day range "
                    + "on the export 'date' partition (UTC). Give startDate and endDate together; use the same "
                    + "value for a single day. Counts only cover exported data (see 'dataAsOf' in /schema).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The count, echoing the range when one was given",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"count\":54943,\"startDate\":\"2025-06-01\",\"endDate\":\"2025-06-07\"}"))),
            @ApiResponse(responseCode = "400", description = "Only one of the two dates given, or endDate before startDate")
    })
    @GetMapping("/transactions/count")
    public Map<String, Object> getTransactionCount(
            @Parameter(description = "First day (inclusive), yyyy-MM-dd; requires endDate", example = "2025-06-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Last day (inclusive), yyyy-MM-dd; requires startDate. Same as startDate for one day",
                    example = "2025-06-07")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException("'startDate' and 'endDate' must be given together");
        }
        if (startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("'endDate' must not be before 'startDate'");
        }
        long count = txQueryService.getTransactionCount(startDate, endDate);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", count);
        if (startDate != null) {
            result.put("startDate", startDate.toString());
            result.put("endDate", endDate.toString());
        }
        return result;
    }

    /**
     * Ad-hoc SQL query endpoint (read-only, against analytics data).
     *
     * <p>Security is enforced primarily by DuckDB's engine-level external-access sandbox.
     * {@link SqlValidator} additionally enforces SELECT/WITH-only statements and blocks
     * dangerous functions as defense in depth.</p>
     *
     * <p>Rows are limited the way SQL front-ends do it: the statement is wrapped in a
     * {@code LIMIT} ({@code maxRows}, default {@code yaci.store.analytics.query.default-max-rows},
     * capped by {@code yaci.store.analytics.query.max-rows}); truncation is reported in the
     * {@code X-Analytics-Truncated} response header.</p>
     */
    @Operation(summary = "Run a read-only SQL query",
            description = "Executes a single SELECT/WITH statement (DuckDB SQL, PostgreSQL-compatible: CTEs, window "
                    + "functions, QUALIFY, list/struct functions) against the analytics tables listed by GET /schema. "
                    + "Rows are returned as JSON objects keyed by column label. The result is limited to 'maxRows' "
                    + "(default 100, hard cap 10,000 unless configured lower): the statement runs as "
                    + "SELECT * FROM (<sql>) LIMIT maxRows+1, so DuckDB stops at the limit and an inner ORDER BY / "
                    + "LIMIT keeps its meaning. When more rows exist the response carries "
                    + "'X-Analytics-Truncated: true'; 'X-Analytics-Row-Limit' always states the applied limit. "
                    + "Each query is subject to a timeout. Filter on the partition column ('date' or 'epoch') for "
                    + "fast responses. Statements other than SELECT/WITH, multiple statements, file/URL access, "
                    + "extension management, catalog/metadata functions and access to the attached PostgreSQL "
                    + "database are rejected with 400.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Result rows (headers: X-Analytics-Row-Limit, X-Analytics-Truncated)",
                    content = @Content(examples = @ExampleObject(value = "[{\"epoch\":306,\"blocks\":19342},{\"epoch\":307,\"blocks\":15583}]"))),
            @ApiResponse(responseCode = "400", description = "Rejected by the validator or failed in DuckDB (unknown table/column, syntax error, timeout)",
                    content = @Content(examples = @ExampleObject(value = "{\"error\":\"Blocked SQL token 'SHOW' is not allowed in ad-hoc queries\"}")))
    })
    @PostMapping("/sql")
    public ResponseEntity<List<Map<String, Object>>> executeSql(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The SQL statement to run and, optionally, the row limit",
                    required = true, content = @Content(examples = @ExampleObject(
                            value = "{\"sql\":\"SELECT epoch, count(*) AS blocks FROM block WHERE epoch >= 306 GROUP BY epoch ORDER BY epoch\", \"maxRows\": 100}")))
            @RequestBody SqlQueryRequest request) {
        String sql = request == null ? null : request.sql();
        SqlValidator.validate(sql);
        AnalyticsQueryExecutor.QueryResult result = queryExecutor.execute(sql.trim(),
                request == null ? null : request.maxRows());
        return ResponseEntity.ok()
                .header(ROW_LIMIT_HEADER, String.valueOf(result.rowLimit()))
                .header(TRUNCATED_HEADER, String.valueOf(result.truncated()))
                .body(result.rows());
    }

    /** Response header: the row limit applied to the query. */
    public static final String ROW_LIMIT_HEADER = "X-Analytics-Row-Limit";
    /** Response header: {@code true} when more rows existed than were returned. */
    public static final String TRUNCATED_HEADER = "X-Analytics-Truncated";

    @Schema(description = "Ad-hoc SQL request")
    public record SqlQueryRequest(
            @Schema(description = "A single SELECT or WITH statement over the analytics tables",
                    example = "SELECT count(*) AS txs FROM transaction WHERE date = DATE '2025-06-01'")
            String sql,
            @Schema(description = "Maximum rows to return (default 100; values above the server's hard cap are reduced to it)",
                    example = "100", nullable = true)
            Integer maxRows) {}

    /**
     * Rejected SQL (validator) and unknown tables are client errors: report them as
     * {@code 400 Bad Request} with the reason instead of a generic 500, so callers (and LLM
     * agents driving this endpoint) can correct the query.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /**
     * Queries that fail inside DuckDB (syntax error, unknown column, timeout, ...) are
     * reported as {@code 400 Bad Request}; the message is already sanitized by the executor.
     */
    @ExceptionHandler(AnalyticsQueryExecutor.QueryExecutionException.class)
    public ResponseEntity<Map<String, String>> handleQueryFailure(AnalyticsQueryExecutor.QueryExecutionException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
