package com.bloxbean.cardano.yaci.store.snapshot.convert;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The built-in named converters.
 *
 * <p>These cover the conversions the mapping survey actually found: timestamp to Unix seconds (most
 * tables), checked numeric narrowing, JSON text to {@code jsonb}, and deterministic UUIDv5 for rows
 * whose identity the export omits.
 */
public class ConverterRegistry {

    /** RFC 4122 URL namespace. Fixed for the life of the snapshot format. */
    public static final String UUID_NAMESPACE = "6ba7b8119dad11d180b400c04fd430c8";

    private static final String UUID_V5_PREFIX = "uuid-v5:";

    private final Map<String, ValueConverter> converters = new LinkedHashMap<>();

    public ConverterRegistry() {
        register(new SimpleConverter("timestamp-to-epoch-seconds",
                src -> "CAST(epoch(" + src + ") AS BIGINT)",
                (s, t) -> s.startsWith("timestamp") ? null
                        : "timestamp-to-epoch-seconds expects a timestamp source but found " + s));

        register(new SimpleConverter("timestamp-to-date",
                src -> "CAST(" + src + " AS DATE)", null));

        // A plain DuckDB CAST raises on overflow, which is exactly the "checked" behaviour wanted:
        // a value that does not fit fails the batch instead of wrapping silently.
        register(new SimpleConverter("to-smallint", src -> "CAST(" + src + " AS SMALLINT)", null));
        register(new SimpleConverter("to-integer", src -> "CAST(" + src + " AS INTEGER)", null));
        register(new SimpleConverter("to-bigint", src -> "CAST(" + src + " AS BIGINT)", null));
        register(new SimpleConverter("to-varchar", src -> "CAST(" + src + " AS VARCHAR)", null));
        register(new SimpleConverter("to-decimal38", src -> "CAST(" + src + " AS DECIMAL(38,0))", null));

        // PostgreSQL jsonb is surfaced by the DuckDB postgres extension as VARCHAR and validated by
        // PostgreSQL on insert. Declaring the converter makes the varchar -> jsonb transition
        // explicit, and lets specification validation reject an unconverted mapping.
        register(new SimpleConverter("varchar-to-jsonb", src -> src,
                (s, t) -> t.equals("jsonb") || t.equals("json") ? null
                        : "varchar-to-jsonb targets a jsonb column but found " + t));

        register(new SimpleConverter("null-literal", src -> "NULL", null));
    }

    private void register(ValueConverter c) {
        converters.put(c.name(), c);
    }

    public boolean has(String name) {
        return converters.containsKey(name) || name.startsWith(UUID_V5_PREFIX);
    }

    public Set<String> names() {
        return converters.keySet();
    }

    /**
     * Resolve a converter by name.
     *
     * <p>{@code uuid-v5:col1,col2} is parameterised: it derives a stable UUIDv5 from the listed key
     * columns so a re-import produces byte-identical ids.
     */
    public ValueConverter get(String name) {
        if (name.startsWith(UUID_V5_PREFIX)) {
            String[] cols = name.substring(UUID_V5_PREFIX.length()).split(",");
            for (String c : cols) {
                Identifiers.requireSqlIdentifier(c.trim(), "uuid-v5 key column");
            }
            return new UuidV5Converter(name, cols);
        }
        ValueConverter c = converters.get(name);
        if (c == null) {
            throw new IllegalArgumentException("Unknown converter '" + name + "'. Known: " + converters.keySet()
                    + " plus parameterised '" + UUID_V5_PREFIX + "<key columns>'");
        }
        return c;
    }

    private record SimpleConverter(String name,
                                   Function<String, String> sql,
                                   BiFunction<String, String, String> rejecter)
            implements ValueConverter {

        @Override
        public String toSql(String sourceExpression) {
            return sql.apply(sourceExpression);
        }

        @Override
        public String reject(String sourceType, String targetType) {
            return rejecter == null ? null : rejecter.apply(sourceType, targetType);
        }
    }
}
