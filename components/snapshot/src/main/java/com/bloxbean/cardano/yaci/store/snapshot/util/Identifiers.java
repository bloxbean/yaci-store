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

    private Identifiers() {
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

    /** Single-quote a string literal for embedding in generated SQL. */
    public static String literal(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
