package com.bloxbean.cardano.yaci.store.snapshot.load;

import java.util.Locale;
import java.util.Set;

/**
 * Decides whether a DuckLake source type may be written to a PostgreSQL target column without an
 * explicit converter.
 *
 * <p>The rule is conservative: widening within the same family is allowed, everything else must be
 * declared. In particular a {@code timestamp} source never silently becomes a {@code bigint} Unix
 * time, and a {@code varchar} never silently becomes {@code jsonb} — both are real conversions in
 * this schema and both get a named converter.
 */
public final class TypeCompatibility {

    private static final Set<String> INTEGERS =
            Set.of("tinyint", "smallint", "int", "integer", "bigint", "hugeint",
                    "int8", "int16", "int32", "int64", "utinyint", "usmallint", "uinteger", "ubigint");
    private static final Set<String> TEXTS = Set.of("varchar", "text", "string", "char", "bpchar", "name");
    private static final Set<String> BOOLS = Set.of("boolean", "bool");
    private static final Set<String> DATES = Set.of("date");
    private static final Set<String> UUIDS = Set.of("uuid");

    private TypeCompatibility() {
    }

    /**
     * @return {@code null} when the assignment is safe, otherwise a human-readable reason
     */
    public static String reject(String sourceType, String targetType) {
        String s = normalize(sourceType);
        String t = normalize(targetType);
        if (s.isEmpty() || t.isEmpty()) {
            return "unknown type (source '" + sourceType + "', target '" + targetType + "')";
        }
        if (s.equals(t)) {
            return null;
        }

        Family fs = family(s);
        Family ft = family(t);
        if (fs == Family.UNKNOWN || ft == Family.UNKNOWN) {
            return "unsupported type pair (source '" + sourceType + "' -> target '" + targetType + "')";
        }
        if (fs != ft) {
            return "type '" + sourceType + "' is not assignable to '" + targetType + "'";
        }

        if (fs == Family.INTEGER) {
            return width(s) <= width(t) ? null
                    : "narrowing '" + sourceType + "' to '" + targetType + "' can overflow";
        }
        // Text, boolean, date, uuid, decimal and timestamp within a family are accepted; PostgreSQL
        // enforces any remaining length or precision constraint at insert time.
        return null;
    }

    private enum Family {INTEGER, DECIMAL, TEXT, BOOLEAN, DATE, TIMESTAMP, UUID, JSON, BLOB, UNKNOWN}

    private static Family family(String t) {
        if (INTEGERS.contains(t)) {
            return Family.INTEGER;
        }
        if (t.startsWith("decimal") || t.startsWith("numeric") || t.equals("double")
                || t.equals("float") || t.equals("real") || t.equals("hugeint")) {
            return Family.DECIMAL;
        }
        if (TEXTS.contains(t)) {
            return Family.TEXT;
        }
        if (BOOLS.contains(t)) {
            return Family.BOOLEAN;
        }
        if (DATES.contains(t)) {
            return Family.DATE;
        }
        if (t.startsWith("timestamp")) {
            return Family.TIMESTAMP;
        }
        if (UUIDS.contains(t)) {
            return Family.UUID;
        }
        if (t.equals("json") || t.equals("jsonb")) {
            return Family.JSON;
        }
        if (t.equals("blob") || t.equals("bytea")) {
            return Family.BLOB;
        }
        return Family.UNKNOWN;
    }

    private static int width(String t) {
        return switch (t) {
            case "tinyint", "utinyint" -> 1;
            case "smallint", "int16", "usmallint" -> 2;
            case "int", "integer", "int32", "uinteger" -> 4;
            case "bigint", "int64", "int8", "ubigint" -> 8;
            case "hugeint" -> 16;
            default -> 8;
        };
    }

    /** Strip PostgreSQL modifiers so {@code character varying(64)} compares as {@code varchar}. */
    static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim().toLowerCase(Locale.ROOT);
        t = t.replace("character varying", "varchar")
                .replace("timestamp without time zone", "timestamp")
                .replace("timestamp with time zone", "timestamptz")
                .replace("double precision", "double")
                .replace("character", "char");
        if (t.startsWith("varchar")) {
            return "varchar";
        }
        if (t.startsWith("char")) {
            return "char";
        }
        if (t.startsWith("numeric")) {
            return t.replaceAll("\\s+", "");
        }
        if (t.startsWith("decimal")) {
            return t.replaceAll("\\s+", "");
        }
        int paren = t.indexOf('(');
        if (paren > 0 && !t.startsWith("decimal") && !t.startsWith("numeric")) {
            t = t.substring(0, paren);
        }
        return t.trim();
    }
}
