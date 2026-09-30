package com.bloxbean.cardano.yaci.store.snapshot.load;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The PostgreSQL side of the mapping contract, read from {@code information_schema} rather than
 * assumed from migration files, so what is checked is what actually exists.
 *
 * @param columns  column name to type, in ordinal order
 * @param nullable column name to nullability
 * @param defaults column name to default expression (absent when there is none)
 */
public record TargetTable(String schema,
                          String name,
                          Map<String, String> columns,
                          Map<String, Boolean> nullable,
                          Map<String, String> defaults,
                          List<String> primaryKey,
                          boolean partitioned) {

    public boolean hasColumn(String c) {
        return columns.containsKey(c);
    }

    public String type(String c) {
        return columns.get(c);
    }

    /** Column names in a stable order, for fingerprinting. */
    public String fingerprintSource() {
        StringBuilder sb = new StringBuilder();
        new LinkedHashMap<>(columns).forEach((k, v) -> sb.append(k).append(':').append(v).append(';'));
        return sb.toString();
    }
}
