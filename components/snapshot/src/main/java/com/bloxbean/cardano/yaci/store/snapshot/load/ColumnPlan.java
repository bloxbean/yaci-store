package com.bloxbean.cardano.yaci.store.snapshot.load;

import java.util.List;

/**
 * The resolved, executable mapping for one table: which target columns are written and the DuckDB
 * expression that produces each.
 *
 * @param targetColumns target column names, in insert order
 * @param expressions   parallel DuckDB SQL expressions
 * @param defaulted     target columns intentionally left to their PostgreSQL default
 */
public record ColumnPlan(List<String> targetColumns, List<String> expressions, List<String> defaulted) {

    public String selectList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expressions.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(expressions.get(i));
        }
        return sb.toString();
    }

    public String targetColumnList() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < targetColumns.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers.quote(targetColumns.get(i)));
        }
        return sb.toString();
    }
}
