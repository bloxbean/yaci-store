package com.bloxbean.cardano.yaci.store.analytics.query.controller;

import com.bloxbean.cardano.yaci.store.analytics.query.executor.AnalyticsQueryExecutor;
import com.bloxbean.cardano.yaci.store.analytics.query.model.SchemaOverview;
import com.bloxbean.cardano.yaci.store.analytics.query.model.TableDescription;
import com.bloxbean.cardano.yaci.store.analytics.query.service.AnalyticsBlockQueryService;
import com.bloxbean.cardano.yaci.store.analytics.query.service.AnalyticsSchemaService;
import com.bloxbean.cardano.yaci.store.analytics.query.service.AnalyticsTransactionQueryService;
import com.bloxbean.cardano.yaci.store.analytics.query.service.SqlValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for analytics queries over Parquet data (and optionally live PostgreSQL).
 *
 * <p>Can be disabled independently of the MCP tools by setting
 * {@code yaci.store.analytics.query.rest-api-enabled=false}.
 * This allows production deployments to restrict analytics access to the MCP
 * interface only, while keeping the REST API available in development.</p>
 */
@RestController
@RequestMapping("/api/v1/analytics/parquet")
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = {"yaci.store.analytics.query.enabled", "yaci.store.analytics.query.rest-api-enabled"},
        havingValue = "true",
        matchIfMissing = true
)
public class ParquetAnalyticsController {

    private final AnalyticsBlockQueryService blockQueryService;
    private final AnalyticsTransactionQueryService txQueryService;
    private final AnalyticsQueryExecutor queryExecutor;
    private final AnalyticsSchemaService schemaService;

    // --- Schema discovery endpoints ---

    @GetMapping("/schema")
    public SchemaOverview listTables() {
        return schemaService.listTables();
    }

    @GetMapping("/schema/{tableName}")
    public TableDescription describeTable(@PathVariable String tableName) {
        return schemaService.describeTable(tableName);
    }

    // --- Pre-built query endpoints ---

    @GetMapping("/blocks/epoch-stats")
    public List<Map<String, Object>> getEpochBlockStats(
            @RequestParam int startEpoch,
            @RequestParam int endEpoch) {
        return blockQueryService.getEpochBlockStatistics(startEpoch, endEpoch);
    }

    @GetMapping("/blocks/pool-production")
    public List<Map<String, Object>> getPoolBlockProduction(
            @RequestParam String poolId,
            @RequestParam int startEpoch,
            @RequestParam int endEpoch) {
        return blockQueryService.getPoolBlockProductionStats(poolId, startEpoch, endEpoch);
    }

    @GetMapping("/transactions/epoch-stats")
    public List<Map<String, Object>> getEpochTxStats(
            @RequestParam int startEpoch,
            @RequestParam int endEpoch) {
        return txQueryService.getEpochTransactionStatistics(startEpoch, endEpoch);
    }

    @GetMapping("/transactions/block-stats")
    public List<Map<String, Object>> getBlockTxStats(
            @RequestParam int startEpoch,
            @RequestParam int endEpoch,
            @RequestParam(defaultValue = "0") int minTxCount) {
        return txQueryService.getBlockTransactionStatistics(startEpoch, endEpoch, minTxCount);
    }

    @GetMapping("/transactions/fee-distribution")
    public List<Map<String, Object>> getFeeDistribution(
            @RequestParam int startEpoch,
            @RequestParam int endEpoch) {
        return txQueryService.getFeeDistributionAnalysis(startEpoch, endEpoch);
    }

    /**
     * Ad-hoc SQL query endpoint (read-only, against analytics data).
     *
     * <p>Security is enforced by {@link SqlValidator} which strips SQL comments,
     * enforces SELECT/WITH-only statements, and blocks dangerous functions via
     * a keyword blocklist. This is the primary defense — DuckDB's
     * {@code enable_external_access} cannot be disabled because Parquet views
     * resolve {@code read_parquet()} lazily at query time.</p>
     *
     * <p>Results are capped at {@link AnalyticsQueryExecutor#MAX_RESULT_ROWS} rows.</p>
     */
    @PostMapping("/sql")
    public List<Map<String, Object>> executeSql(@RequestBody SqlQueryRequest request) {
        String sql = request.sql().trim();
        SqlValidator.validate(sql);
        return queryExecutor.queryForList(sql);
    }

    public record SqlQueryRequest(String sql) {}
}
