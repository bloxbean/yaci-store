package com.bloxbean.cardano.yaci.store.mcp.server.dynamic;

import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.builder.ParameterBinder;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.builder.SqlQueryBuilder;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.DynamicQueryRequest;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.DynamicQueryResponse;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.ValidationResult;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.validation.QueryComplexityValidator;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.validation.RequestValidator;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.validation.SecurityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP service providing dynamic aggregation queries with flexible filtering and grouping.
 *
 * Key Features:
 * - Flexible query building without predefined tools
 * - Whitelist-based security (tables, columns, operations)
 * - SQL injection prevention
 * - Resource protection (timeouts, limits)
 * - Complex queries (joins, aggregations, filtering, grouping)
 *
 * Use Cases:
 * - Ad-hoc analysis not covered by predefined tools
 * - Custom aggregations
 * - Multi-table queries
 * - Exploratory data analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "store.mcp-server.aggregation.dynamic-query.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class DynamicAggregationService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RequestValidator requestValidator;
    private final SecurityValidator securityValidator;
    private final QueryComplexityValidator complexityValidator;
    private final SqlQueryBuilder sqlQueryBuilder;
    private final ParameterBinder parameterBinder;

    @Tool(name = "dynamic-aggregation-query",
          description = "⚠️ LAST RESORT TOOL - Use ONLY when other options are insufficient. " +
                        "Execute a dynamic aggregation query with flexible filtering and grouping. " +
                        "IMPORTANT USAGE GUIDELINES: " +
                        "1. ALWAYS try specific Tier 1 tools first (utxos-by-address, blocks-list, transactions-list, etc.) " +
                        "2. Use predefined aggregation tools if available (they are faster and more reliable) " +
                        "3. Only use this tool when: " +
                        "   - Existing tools don't provide the required data combination " +
                        "   - Multiple calls to Tier 1 tools would be too slow or complex " +
                        "   - Custom aggregations across multiple tables are needed " +
                        "4. ALWAYS call 'get-query-schema' FIRST to learn available tables and columns " +
                        "Features: JOINs, aggregations (SUM, AVG, COUNT, JSONB_ARRAY_LENGTH, etc.), " +
                        "WHERE/HAVING filters (including JSONB operators), GROUP BY, ORDER BY, LIMIT/OFFSET. " +
                        "Security: Whitelist-validated tables, columns, and operations. SQL injection prevention enforced.")
    public DynamicQueryResponse executeDynamicQuery(
        @ToolParam(description = "Dynamic query request specification as JSON") DynamicQueryRequest request
    ) {
        long startTime = System.currentTimeMillis();

        log.info("Executing dynamic query on table: {}", request.table());

        try {
            // 1. Validate request structure and whitelists
            ValidationResult validation = requestValidator.validate(request);
            if (!validation.isValid()) {
                throw new IllegalArgumentException("Invalid request: " + validation.getErrorsAsString());
            }

            // 2. Security checks (SQL injection prevention)
            securityValidator.validateSecurity(request);

            // 3. Complexity checks (resource protection)
            complexityValidator.validateComplexity(request);

            // 4. Build SQL query
            String sql = sqlQueryBuilder.buildQuery(request);

            // 5. Bind parameters
            Map<String, Object> params = parameterBinder.bindParameters(request);

            // 6. Execute query
            List<Map<String, Object>> rows = jdbcTemplate.query(
                sql,
                params,
                new ColumnMapRowMapper()
            );

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("Query executed successfully: {} rows in {}ms", rows.size(), executionTime);

            // 7. Build response
            return new DynamicQueryResponse(
                rows,
                rows.size(),
                executionTime,
                extractColumnTypes(rows),
                sql
            );

        } catch (Exception e) {
            log.error("Error executing dynamic query", e);
            throw new RuntimeException("Query execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts column types from the first row of results.
     */
    private Map<String, String> extractColumnTypes(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> firstRow = rows.get(0);
        return firstRow.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue() != null ? e.getValue().getClass().getSimpleName() : "null"
            ));
    }
}
