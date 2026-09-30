package com.bloxbean.cardano.yaci.store.snapshot.load;

import java.util.TreeMap;
import java.util.Properties;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A worker's DuckDB connection with the target PostgreSQL database attached.
 *
 * <p>Each worker gets its own connection, memory limit, spill directory and PostgreSQL transaction,
 * so one worker can never exhaust another's resources or hold another's locks.
 *
 * <p>The attached database exposes every schema of the target server, so all generated SQL is
 * schema-qualified and the schema name is identifier-validated before it reaches a statement.
 */
public class DuckPgSession implements AutoCloseable {

    public static final String PG_ALIAS = "pgtarget";

    private final Connection duck;
    private final String schema;

    private DuckPgSession(Connection duck, String schema) {
        this.duck = duck;
        this.schema = schema;
    }

    public static DuckPgSession open(ImportOptions options, Path spillDir, int threads) throws SQLException {
        String schema = Identifiers.requireSqlIdentifier(options.schema(), "target schema");
        String connectionString = pgConnectionString(options);
        Connection duck = DuckDb.open(options.memoryLimit(), spillDir, threads);
        try {
            DuckDb.exec(duck, "INSTALL postgres");
            DuckDb.exec(duck, "LOAD postgres");
            DuckDb.exec(duck, "ATTACH " + Identifiers.literal(connectionString)
                    + " AS " + PG_ALIAS + " (TYPE POSTGRES)");
        } catch (SQLException e) {
            duck.close();
            throw new SQLException(redact(e.getMessage(), options.password()), e.getSQLState());
        }
        return new DuckPgSession(duck, schema);
    }

    /** libpq key/value string built from the already-resolved JDBC settings. */
    static String pgConnectionString(ImportOptions options) {
        Properties defaults = new Properties();
        defaults.setProperty("user", options.user());
        defaults.setProperty("password", options.password());
        Properties properties = org.postgresql.Driver.parseURL(options.jdbcUrl(), defaults);
        if (properties == null) {
            throw new IllegalArgumentException("Snapshot import requires a PostgreSQL JDBC URL");
        }
        Map<String, String> keys = Map.ofEntries(
                Map.entry("PGHOST", "host"), Map.entry("PGPORT", "port"), Map.entry("PGDBNAME", "dbname"),
                Map.entry("user", "user"), Map.entry("password", "password"),
                Map.entry("sslmode", "sslmode"), Map.entry("sslrootcert", "sslrootcert"),
                Map.entry("sslcert", "sslcert"), Map.entry("sslkey", "sslkey"),
                Map.entry("sslpassword", "sslpassword"), Map.entry("connectTimeout", "connect_timeout"),
                Map.entry("ApplicationName", "application_name"), Map.entry("options", "options"));
        for (String name : properties.stringPropertyNames()) {
            if (!keys.containsKey(name) && !name.equals("currentSchema") && !name.equals("ssl")) {
                throw new IllegalArgumentException("Unsupported PostgreSQL JDBC setting for snapshot import: " + name);
            }
        }
        String host = properties.getProperty("PGHOST");
        if (host.contains(",")) {
            throw new IllegalArgumentException("Snapshot import requires a single PostgreSQL host");
        }
        // pgjdbc retains IPv6 brackets; libpq's keyword format takes the unbracketed address.
        if (host.startsWith("[") && host.endsWith("]")) {
            properties.setProperty("PGHOST", host.substring(1, host.length() - 1));
        }
        if (properties.getProperty("sslmode") == null && properties.containsKey("ssl")) {
            String ssl = properties.getProperty("ssl");
            if (ssl.isEmpty() || ssl.equalsIgnoreCase("true")) {
                properties.setProperty("sslmode", "verify-full");
            }
        }
        StringBuilder result = new StringBuilder();
        new TreeMap<>(keys).forEach((jdbc, pq) -> {
            String value = properties.getProperty(jdbc);
            if (value != null) {
                result.append(pq).append("='")
                        .append(value.replace("\\", "\\\\").replace("'", "\\'"))
                        .append("' ");
            }
        });
        return result.toString().trim();
    }

    /** Never let a connection string carrying a password reach a log or an exception message. */
    public static String redact(String message, String password) {
        if (message == null) {
            return null;
        }
        String out = message;
        if (password != null && !password.isEmpty()) {
            String escaped = password.replace("\\", "\\\\").replace("'", "\\'");
            out = out.replace(escaped.replace("'", "''"), "****")
                    .replace(escaped, "****").replace(password, "****");
        }
        return out.replaceAll("(?i)(?:sslpassword|password)=(?:'(?:\\\\.|[^'\\\\])*'|\\S+)", "password=****");
    }

    public Connection connection() {
        return duck;
    }

    public String qualify(String table) {
        return PG_ALIAS + "." + Identifiers.quote(schema) + "." + Identifiers.quote(table);
    }

    public void exec(String sql) throws SQLException {
        DuckDb.exec(duck, sql);
    }

    /** Column name to type of a DuckDB query, without executing it over the data. */
    public Map<String, String> describe(String select) throws SQLException {
        Map<String, String> out = new LinkedHashMap<>();
        try (Statement st = duck.createStatement();
             ResultSet rs = st.executeQuery("DESCRIBE " + select)) {
            while (rs.next()) {
                out.put(rs.getString("column_name"), rs.getString("column_type"));
            }
        }
        return out;
    }

    @Override
    public void close() {
        try {
            duck.close();
        } catch (SQLException ignored) {
            // Nothing useful can be done while closing a worker connection.
        }
    }
}
