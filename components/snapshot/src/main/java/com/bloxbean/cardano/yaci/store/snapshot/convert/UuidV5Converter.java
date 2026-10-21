package com.bloxbean.cardano.yaci.store.snapshot.convert;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Deterministic RFC 4122 version 5 (SHA-1) UUID generation.
 *
 * <p>Some analytics exports omit the surrogate {@code id} of their PostgreSQL table
 * ({@code voting_procedure}, {@code mir}). A random replacement would make two imports of the same
 * snapshot differ, so ids are derived from a fixed namespace and the row's natural key: re-importing
 * the same snapshot reproduces exactly the same ids.
 *
 * <p>DuckDB has no {@code uuidv5()} builtin, so the digest is reassembled in SQL. The SQL form and
 * {@link #compute(String)} are pinned against each other by {@code UuidV5ConverterTest}.
 */
public class UuidV5Converter implements ValueConverter {

    /** ASCII unit separator; cannot occur inside a hash, address or hex identifier. */
    static final String KEY_SEPARATOR = String.valueOf((char) 31);

    private final String name;
    private final String[] keyColumns;

    public UuidV5Converter(String name, String[] keyColumns) {
        this.name = name;
        this.keyColumns = Arrays.stream(keyColumns).map(String::trim).toArray(String[]::new);
        if (this.keyColumns.length == 0) {
            throw new IllegalArgumentException("uuid-v5 requires at least one key column");
        }
        for (String c : this.keyColumns) {
            Identifiers.requireSqlIdentifier(c, "uuid-v5 key column");
        }
    }

    @Override
    public String name() {
        return name;
    }

    public String[] keyColumns() {
        return keyColumns.clone();
    }

    /** The value hashed for a row: key columns joined with a unit separator. */
    String nameExpression() {
        String sep = "chr(31)";
        return Arrays.stream(keyColumns)
                .map(c -> "COALESCE(CAST(" + Identifiers.quote(c) + " AS VARCHAR), '')")
                .collect(Collectors.joining(" || " + sep + " || "));
    }

    /**
     * The source expression is ignored: a UUIDv5 is a function of the declared key columns, not of a
     * single source column.
     */
    @Override
    public String toSql(String sourceExpression) {
        String h = "sha1(from_hex('" + ConverterRegistry.UUID_NAMESPACE + "') || CAST("
                + nameExpression() + " AS BLOB))";
        // Canonical 8-4-4-4-12 form with the version nibble forced to 5 and the RFC 4122 variant
        // nibble forced to one of 8/9/a/b.
        String variant = "substr('89ab', ((position(substr(" + h + ", 17, 1) IN '0123456789abcdef') - 1) % 4) + 1, 1)";
        return "CAST("
                + "substr(" + h + ", 1, 8) || '-'"
                + " || substr(" + h + ", 9, 4) || '-'"
                + " || '5' || substr(" + h + ", 14, 3) || '-'"
                + " || " + variant + " || substr(" + h + ", 18, 3) || '-'"
                + " || substr(" + h + ", 21, 12)"
                + " AS VARCHAR)";
    }

    @Override
    public String reject(String sourceType, String targetType) {
        return targetType.equals("uuid") ? null
                : "uuid-v5 targets a uuid column but found " + targetType;
    }

    /** Reference implementation used by tests and by handlers that build rows in Java. */
    public static UUID compute(String name) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(HexFormat.of().parseHex(ConverterRegistry.UUID_NAMESPACE));
            sha1.update(name.getBytes(StandardCharsets.UTF_8));
            byte[] d = sha1.digest();
            d[6] = (byte) ((d[6] & 0x0F) | 0x50);
            d[8] = (byte) ((d[8] & 0x3F) | 0x80);
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (d[i] & 0xFF);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (d[i] & 0xFF);
            }
            return new UUID(msb, lsb);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 unavailable", e);
        }
    }

    /** Join key values exactly as {@link #nameExpression()} does in SQL. */
    public static String keyOf(String... values) {
        return String.join(KEY_SEPARATOR, values);
    }
}
