package com.bloxbean.cardano.yaci.store.snapshot.util;

import java.util.regex.Pattern;

/**
 * Identifier validation and quoting.
 *
 * <p>Every schema, table and column name that reaches generated SQL passes through here first. A
 * snapshot archive can influence which specs are looked up, so identifiers are validated against a
 * conservative pattern and then double-quoted rather than interpolated raw.
 */
public final class Identifiers {

    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]{0,62}");
    private static final Pattern SPEC_ID = Pattern.compile("[a-z0-9]([a-z0-9-]{0,62}[a-z0-9])?");
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,254}");

    private Identifiers() {
    }

    /**
     * A file name that a snapshot supplies (a part, the manifest, the snapshot id as a directory name).
     * One path segment of letters, digits, dots, underscores and hyphens that does not start with a dot,
     * so it cannot be absolute, name a parent directory or carry a separator.
     */
    public static String requireFileName(String value, String what) {
        if (value == null || !FILE_NAME.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid file name for " + what + ": '" + value + "'");
        }
        return value;
    }

    public static String requireSqlIdentifier(String value, String what) {
        if (value == null || !SQL_IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier for " + what + ": '" + value + "'");
        }
        return value;
    }

    public static String requireSpecId(String value) {
        if (value == null || !SPEC_ID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Invalid snapshot spec id '" + value + "'. Use lowercase letters, digits and hyphens.");
        }
        return value;
    }

    /** Double-quote an already validated identifier. */
    public static String quote(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    /** {@code schema.table}, each part double-quoted; quoting escapes any embedded quote. */
    public static String qualified(String schema, String table) {
        return quote(schema) + "." + quote(table);
    }

    /** Single-quote a string literal for embedding in generated SQL. */
    public static String literal(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
