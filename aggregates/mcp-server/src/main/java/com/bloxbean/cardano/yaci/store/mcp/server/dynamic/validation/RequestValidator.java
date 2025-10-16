package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.validation;

import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.config.ColumnWhitelist;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.config.OperationWhitelist;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.config.TableWhitelist;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates dynamic query requests against whitelists and structural rules.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RequestValidator {

    private final TableWhitelist tableWhitelist;
    private final ColumnWhitelist columnWhitelist;
    private final OperationWhitelist operationWhitelist;

    /**
     * Validates a dynamic query request.
     * Returns ValidationResult with errors if validation fails.
     */
    public ValidationResult validate(DynamicQueryRequest request) {
        List<String> errors = new ArrayList<>();

        // 1. Validate table
        validateTable(request.table(), errors);

        // 2. Validate select fields
        validateSelectFields(request.table(), request.selectFields(), errors);

        // 3. Validate filters
        validateFilters(request.table(), request.filters(), errors);

        // 4. Validate GROUP BY
        validateGroupBy(request.table(), request.groupBy(), errors);

        // 5. Validate HAVING
        validateHaving(request.table(), request.having(), errors);

        // 6. Validate ORDER BY (with SELECT clause context for aliases)
        validateOrderByWithSelectContext(request.table(), request.orderBy(), request.selectFields(), errors);

        // 7. Validate JOINs
        validateJoins(request.joins(), errors);

        // 8. Validate structural consistency
        validateStructure(request, errors);

        return errors.isEmpty()
            ? ValidationResult.success()
            : ValidationResult.failure(errors);
    }

    private void validateTable(String table, List<String> errors) {
        if (table == null || table.isBlank()) {
            errors.add("Table name is required");
            return;
        }

        if (!tableWhitelist.isAllowed(table)) {
            errors.add("Table not allowed: " + table);
        }
    }

    private void validateSelectFields(String table, List<AggregationField> fields, List<String> errors) {
        if (fields.isEmpty()) {
            errors.add("At least one select field is required");
            return;
        }

        for (AggregationField field : fields) {
            // Validate column exists
            if (!columnWhitelist.isColumnAllowed(table, field.column())) {
                errors.add("Column not allowed in SELECT: " + field.column());
            }

            // Validate aggregation function if present
            if (field.function() != null && !operationWhitelist.isAggregationAllowed(field.function())) {
                errors.add("Aggregation function not allowed: " + field.function());
            }

            // Validate alias if present (no special characters)
            if (field.alias() != null && !field.alias().matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
                errors.add("Invalid alias format: " + field.alias());
            }
        }
    }

    private void validateFilters(String table, List<FilterCondition> filters, List<String> errors) {
        for (FilterCondition filter : filters) {
            // Validate column exists
            if (!columnWhitelist.isColumnAllowed(table, filter.column())) {
                errors.add("Column not allowed in WHERE: " + filter.column());
            }

            // Validate operator
            if (!operationWhitelist.isOperatorAllowed(filter.operator())) {
                errors.add("Filter operator not allowed: " + filter.operator());
            }

            // Validate value is present when required
            if (filter.requiresValue() && filter.value() == null) {
                errors.add("Filter value required for operator: " + filter.operator());
            }

            // Validate BETWEEN has array of 2 values
            if (filter.operator() == FilterOperator.BETWEEN) {
                if (!(filter.value() instanceof List<?> list) || list.size() != 2) {
                    errors.add("BETWEEN operator requires array of 2 values");
                }
            }

            // Validate IN has array
            if (filter.operator() == FilterOperator.IN || filter.operator() == FilterOperator.NOT_IN) {
                if (!(filter.value() instanceof List<?>)) {
                    errors.add("IN operator requires array of values");
                }
            }
        }
    }

    private void validateGroupBy(String table, List<GroupByField> groupBy, List<String> errors) {
        for (GroupByField field : groupBy) {
            if (!columnWhitelist.isColumnAllowed(table, field.column())) {
                errors.add("Column not allowed in GROUP BY: " + field.column());
            }
        }
    }

    private void validateHaving(String table, List<FilterCondition> having, List<String> errors) {
        for (FilterCondition filter : having) {
            // HAVING clause can reference aggregated columns, so we're less strict
            // but still validate operators
            if (!operationWhitelist.isOperatorAllowed(filter.operator())) {
                errors.add("Filter operator not allowed in HAVING: " + filter.operator());
            }
        }
    }

    /**
     * Validates ORDER BY with SELECT clause context.
     * Allows ordering by both whitelisted columns and SELECT aliases.
     */
    private void validateOrderByWithSelectContext(String table, List<OrderByField> orderBy,
                                                   List<AggregationField> selectFields, List<String> errors) {
        // Collect all valid aliases from SELECT clause
        List<String> validAliases = selectFields.stream()
            .filter(field -> field.alias() != null)
            .map(AggregationField::alias)
            .toList();

        for (OrderByField field : orderBy) {
            String column = field.column();

            // Check if it's a valid alias from SELECT clause
            boolean isAlias = validAliases.contains(column);

            // Check if it's a whitelisted column
            boolean isWhitelistedColumn = columnWhitelist.isColumnAllowed(table, column);

            if (!isAlias && !isWhitelistedColumn) {
                errors.add("Column not allowed in ORDER BY: " + column);
            }
        }
    }

    private void validateJoins(List<JoinSpec> joins, List<String> errors) {
        for (JoinSpec join : joins) {
            // Validate join table is allowed
            if (!tableWhitelist.isAllowed(join.joinTable())) {
                errors.add("Join table not allowed: " + join.joinTable());
            }

            // Note: We don't validate join columns against whitelist here
            // because they might reference different tables
        }
    }

    private void validateStructure(DynamicQueryRequest request, List<String> errors) {
        // If GROUP BY is used, all non-aggregated SELECT fields must be in GROUP BY
        if (!request.groupBy().isEmpty()) {
            List<String> groupByColumns = request.groupBy().stream()
                .map(GroupByField::column)
                .toList();

            for (AggregationField field : request.selectFields()) {
                if (!field.isAggregated() && !groupByColumns.contains(field.column())) {
                    errors.add("Non-aggregated SELECT field must be in GROUP BY: " + field.column());
                }
            }
        }

        // HAVING can only be used with GROUP BY
        if (!request.having().isEmpty() && request.groupBy().isEmpty()) {
            errors.add("HAVING clause requires GROUP BY");
        }

        // Validate pagination limits
        if (request.limit() != null && request.limit() < 1) {
            errors.add("LIMIT must be greater than 0");
        }

        if (request.offset() != null && request.offset() < 0) {
            errors.add("OFFSET must be non-negative");
        }
    }
}
