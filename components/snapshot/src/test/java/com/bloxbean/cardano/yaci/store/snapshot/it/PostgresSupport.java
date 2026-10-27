package com.bloxbean.cardano.yaci.store.snapshot.it;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connection settings for the PostgreSQL integration tests.
 *
 * <p>Credentials come from the environment, never from a file in the repository, and the tests skip
 * cleanly when they are absent so an ordinary {@code ./gradlew test} needs no database:
 *
 * <pre>
 * SNAPSHOT_IT_JDBC_URL=jdbc:postgresql://localhost:5432/postgres
 * SNAPSHOT_IT_USER=postgres
 * SNAPSHOT_IT_PASSWORD=...
 * SNAPSHOT_IT_SCHEMA=yaci_snapshot_it   # optional, defaults to yaci_snapshot_it
 * </pre>
 *
 * <p>The tests only ever create, use and drop their own dedicated schema.
 */
final class PostgresSupport {

    static final String DEFAULT_SCHEMA = "yaci_snapshot_it";

    private PostgresSupport() {
    }

    static boolean available() {
        return url() != null && !url().isBlank();
    }

    static String url() {
        return System.getenv("SNAPSHOT_IT_JDBC_URL");
    }

    static String user() {
        return System.getenv().getOrDefault("SNAPSHOT_IT_USER", "postgres");
    }

    static String password() {
        return System.getenv().getOrDefault("SNAPSHOT_IT_PASSWORD", "");
    }

    static String schema() {
        String s = System.getenv().getOrDefault("SNAPSHOT_IT_SCHEMA", DEFAULT_SCHEMA);
        return Identifiers.requireSqlIdentifier(s, "integration test schema");
    }

    static Connection connect() throws SQLException {
        return DriverManager.getConnection(url(), user(), password());
    }

    /** JDBC URL carrying the dedicated test schema, as the importer expects. */
    static String schemaUrl() {
        String u = url();
        return u + (u.contains("?") ? "&" : "?") + "currentSchema=" + schema();
    }

    /**
     * Recreate the dedicated test schema from scratch. Scoped by name to the schema this test owns;
     * it never touches anything else.
     */
    static void resetSchema(Connection conn) throws SQLException, IOException {
        String schema = schema();
        if (schema.equals("public")) {
            throw new IllegalStateException("Refusing to reset the public schema");
        }
        try (Statement st = conn.createStatement()) {
            st.execute("DROP SCHEMA IF EXISTS " + Identifiers.quote(schema) + " CASCADE");
            st.execute("CREATE SCHEMA " + Identifiers.quote(schema));
            st.execute("SET search_path TO " + Identifiers.quote(schema));
            // Comments are stripped before splitting: a semicolon inside a comment would
            // otherwise cut a statement in half.
            for (String statement : stripComments(readSchemaSql()).split(";")) {
                if (!statement.isBlank()) {
                    st.execute(statement);
                }
            }
            // A minimal Flyway history so the fingerprint checks have something real to read.
            st.execute("CREATE TABLE " + Identifiers.quote(schema) + ".flyway_schema_history ("
                    + " installed_rank integer PRIMARY KEY, version varchar(50),"
                    + " description varchar(200), success boolean NOT NULL)");
            st.execute("INSERT INTO " + Identifiers.quote(schema) + ".flyway_schema_history"
                    + " VALUES (1, '0.000.1', 'init', true)");
        }
    }

    static void dropSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("DROP SCHEMA IF EXISTS " + Identifiers.quote(schema()) + " CASCADE");
        }
    }

    private static String stripComments(String sql) {
        StringBuilder out = new StringBuilder();
        for (String line : sql.split("\n")) {
            int i = line.indexOf("--");
            out.append(i >= 0 ? line.substring(0, i) : line).append('\n');
        }
        return out.toString();
    }

    private static String readSchemaSql() throws IOException {
        try (InputStream in = PostgresSupport.class.getResourceAsStream("/it/schema.sql")) {
            if (in == null) {
                throw new IOException("Missing test resource /it/schema.sql");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
