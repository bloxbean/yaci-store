package com.bloxbean.cardano.yaci.store.mcp.server.dynamic.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Request model for dynamic aggregation queries.
 * Provides a declarative way to specify complex queries without writing SQL.
 */
public record DynamicQueryRequest(
    String table,
    List<AggregationField> selectFields,
    List<FilterCondition> filters,
    List<GroupByField> groupBy,
    List<FilterCondition> having,
    List<OrderByField> orderBy,
    Integer limit,
    Integer offset,
    List<JoinSpec> joins
) {
    /**
     * Creates a request with default empty lists.
     */
    public DynamicQueryRequest {
        selectFields = selectFields != null ? selectFields : new ArrayList<>();
        filters = filters != null ? filters : new ArrayList<>();
        groupBy = groupBy != null ? groupBy : new ArrayList<>();
        having = having != null ? having : new ArrayList<>();
        orderBy = orderBy != null ? orderBy : new ArrayList<>();
        joins = joins != null ? joins : new ArrayList<>();
    }

    /**
     * Builder for creating DynamicQueryRequest instances.
     */
    public static class Builder {
        private String table;
        private List<AggregationField> selectFields = new ArrayList<>();
        private List<FilterCondition> filters = new ArrayList<>();
        private List<GroupByField> groupBy = new ArrayList<>();
        private List<FilterCondition> having = new ArrayList<>();
        private List<OrderByField> orderBy = new ArrayList<>();
        private Integer limit;
        private Integer offset;
        private List<JoinSpec> joins = new ArrayList<>();

        public Builder table(String table) {
            this.table = table;
            return this;
        }

        public Builder select(String column) {
            this.selectFields.add(AggregationField.simple(column));
            return this;
        }

        public Builder select(String column, AggregationFunction function, String alias) {
            this.selectFields.add(new AggregationField(column, function, alias));
            return this;
        }

        public Builder filter(String column, FilterOperator operator, Object value) {
            this.filters.add(FilterCondition.and(column, operator, value));
            return this;
        }

        public Builder filterOr(String column, FilterOperator operator, Object value) {
            this.filters.add(FilterCondition.or(column, operator, value));
            return this;
        }

        public Builder groupBy(String column) {
            this.groupBy.add(GroupByField.of(column));
            return this;
        }

        public Builder having(String column, FilterOperator operator, Object value) {
            this.having.add(FilterCondition.and(column, operator, value));
            return this;
        }

        public Builder orderBy(String column, SortDirection direction) {
            this.orderBy.add(new OrderByField(column, direction));
            return this;
        }

        public Builder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public Builder offset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public Builder join(String joinTable, JoinType joinType, String leftColumn, String rightColumn) {
            this.joins.add(new JoinSpec(joinTable, joinType, leftColumn, rightColumn));
            return this;
        }

        public DynamicQueryRequest build() {
            return new DynamicQueryRequest(
                table, selectFields, filters, groupBy,
                having, orderBy, limit, offset, joins
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
