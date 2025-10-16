package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.builder;

import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model.*;
import com.bloxbean.cardano.yaci.store.mcp.server.dynamic.validation.QueryComplexityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds SQL queries from DynamicQueryRequest objects.
 * Constructs safe, parameterized SQL without string concatenation.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SqlQueryBuilder {

    private final QueryComplexityValidator complexityValidator;

    /**
     * Builds a complete SQL query from the request.
     */
    public String buildQuery(DynamicQueryRequest request) {
        StringBuilder sql = new StringBuilder();

        // 1. SELECT clause
        sql.append("SELECT ");
        sql.append(buildSelectClause(request.selectFields()));

        // 2. FROM clause
        sql.append(" FROM ").append(request.table());

        // 3. JOIN clauses
        if (!request.joins().isEmpty()) {
            sql.append(buildJoinClauses(request.joins()));
        }

        // 4. WHERE clause
        if (!request.filters().isEmpty()) {
            sql.append(" WHERE ");
            sql.append(buildWhereClause(request.filters()));
        }

        // 5. GROUP BY clause
        if (!request.groupBy().isEmpty()) {
            sql.append(" GROUP BY ");
            sql.append(buildGroupByClause(request.groupBy()));
        }

        // 6. HAVING clause
        if (!request.having().isEmpty()) {
            sql.append(" HAVING ");
            sql.append(buildHavingClause(request.having()));
        }

        // 7. ORDER BY clause
        if (!request.orderBy().isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(buildOrderByClause(request.orderBy()));
        }

        // 8. LIMIT (enforce max)
        int effectiveLimit = complexityValidator.getEffectiveLimit(request);
        sql.append(" LIMIT ").append(effectiveLimit);

        // 9. OFFSET
        if (request.offset() != null && request.offset() > 0) {
            sql.append(" OFFSET ").append(request.offset());
        }

        String query = sql.toString();
        log.debug("Generated SQL: {}", query);
        return query;
    }

    private String buildSelectClause(List<AggregationField> fields) {
        return fields.stream()
            .map(this::buildSelectField)
            .collect(Collectors.joining(", "));
    }

    private String buildSelectField(AggregationField field) {
        if (field.function() == null) {
            // Simple column reference
            return field.column();
        }

        // Build aggregation function
        String funcSql = switch (field.function()) {
            case SUM -> "SUM(" + field.column() + ")";
            case AVG -> "AVG(" + field.column() + ")";
            case COUNT -> "COUNT(" + field.column() + ")";
            case COUNT_DISTINCT -> "COUNT(DISTINCT " + field.column() + ")";
            case MIN -> "MIN(" + field.column() + ")";
            case MAX -> "MAX(" + field.column() + ")";
            case PERCENTILE_50 ->
                "PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY " + field.column() + ")";
            case PERCENTILE_90 ->
                "PERCENTILE_CONT(0.90) WITHIN GROUP (ORDER BY " + field.column() + ")";
            case PERCENTILE_95 ->
                "PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY " + field.column() + ")";
            case PERCENTILE_99 ->
                "PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY " + field.column() + ")";
            case STDDEV -> "STDDEV(" + field.column() + ")";
            case VARIANCE -> "VARIANCE(" + field.column() + ")";
            case JSONB_ARRAY_LENGTH -> "jsonb_array_length(" + field.column() + ")";
        };

        // Add alias if specified
        return field.alias() != null ? funcSql + " AS " + field.alias() : funcSql;
    }

    private String buildJoinClauses(List<JoinSpec> joins) {
        StringBuilder sql = new StringBuilder();
        for (JoinSpec join : joins) {
            sql.append(" ");
            sql.append(switch (join.joinType()) {
                case INNER -> "INNER JOIN";
                case LEFT -> "LEFT JOIN";
                case RIGHT -> "RIGHT JOIN";
            });
            sql.append(" ").append(join.joinTable());
            sql.append(" ON ");

            // Support both single-condition (legacy) and multi-condition JOINs
            if (join.conditions() != null && !join.conditions().isEmpty()) {
                // Multi-condition JOIN (e.g., composite keys)
                for (int i = 0; i < join.conditions().size(); i++) {
                    if (i > 0) {
                        sql.append(" AND ");
                    }
                    var condition = join.conditions().get(i);
                    sql.append(condition.leftColumn());
                    sql.append(" = ");
                    sql.append(condition.rightColumn());
                }
            } else {
                // Single-condition JOIN (legacy support)
                sql.append(join.leftColumn());
                sql.append(" = ").append(join.joinTable()).append(".").append(join.rightColumn());
            }
        }
        return sql.toString();
    }

    private String buildWhereClause(List<FilterCondition> filters) {
        return buildFilterClause(filters, "param");
    }

    private String buildHavingClause(List<FilterCondition> filters) {
        return buildFilterClause(filters, "having_param");
    }

    private String buildFilterClause(List<FilterCondition> filters, String paramPrefix) {
        StringBuilder where = new StringBuilder();

        for (int i = 0; i < filters.size(); i++) {
            FilterCondition filter = filters.get(i);

            if (i > 0) {
                where.append(filter.logicalOp() == LogicalOperator.OR ? " OR " : " AND ");
            }

            // Handle JSONB operators specially - they wrap the column in a function or use special operators
            switch (filter.operator()) {
                case JSONB_ARRAY_LENGTH_EQ:
                    where.append("jsonb_array_length(").append(filter.column()).append(") = :").append(paramPrefix).append(i);
                    break;
                case JSONB_ARRAY_LENGTH_GT:
                    where.append("jsonb_array_length(").append(filter.column()).append(") > :").append(paramPrefix).append(i);
                    break;
                case JSONB_ARRAY_LENGTH_GTE:
                    where.append("jsonb_array_length(").append(filter.column()).append(") >= :").append(paramPrefix).append(i);
                    break;
                case JSONB_ARRAY_LENGTH_LT:
                    where.append("jsonb_array_length(").append(filter.column()).append(") < :").append(paramPrefix).append(i);
                    break;
                case JSONB_ARRAY_LENGTH_LTE:
                    where.append("jsonb_array_length(").append(filter.column()).append(") <= :").append(paramPrefix).append(i);
                    break;
                case JSONB_CONTAINS:
                    where.append(filter.column()).append(" @> :").append(paramPrefix).append(i).append("::jsonb");
                    break;
                case JSONB_PATH_MATCH:
                    where.append(filter.column()).append(" @@ :").append(paramPrefix).append(i).append("::jsonpath");
                    break;
                default:
                    // Standard operators
                    where.append(filter.column());
                    where.append(switch (filter.operator()) {
                        case EQ -> " = :" + paramPrefix + i;
                        case NE -> " != :" + paramPrefix + i;
                        case GT -> " > :" + paramPrefix + i;
                        case LT -> " < :" + paramPrefix + i;
                        case GTE -> " >= :" + paramPrefix + i;
                        case LTE -> " <= :" + paramPrefix + i;
                        case IN -> " IN (:" + paramPrefix + i + ")";
                        case NOT_IN -> " NOT IN (:" + paramPrefix + i + ")";
                        case BETWEEN -> " BETWEEN :" + paramPrefix + i + "_min AND :" + paramPrefix + i + "_max";
                        case LIKE -> " LIKE :" + paramPrefix + i;
                        case NOT_LIKE -> " NOT LIKE :" + paramPrefix + i;
                        case IS_NULL -> " IS NULL";
                        case IS_NOT_NULL -> " IS NOT NULL";
                        default -> throw new IllegalArgumentException("Unsupported operator: " + filter.operator());
                    });
            }
        }

        return where.toString();
    }

    private String buildGroupByClause(List<GroupByField> groupBy) {
        return groupBy.stream()
            .map(GroupByField::column)
            .collect(Collectors.joining(", "));
    }

    private String buildOrderByClause(List<OrderByField> orderBy) {
        return orderBy.stream()
            .map(field -> field.column() + " " + field.direction())
            .collect(Collectors.joining(", "));
    }
}
