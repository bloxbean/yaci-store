package com.bloxbean.cardano.yaci.store.snapshot.load;

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
        Connection duck = DuckDb.open(options.memoryLimit(), spillDir, threads);
        try {
            DuckDb.exec(duck, "INSTALL postgres");
            DuckDb.exec(duck, "LOAD postgres");
            DuckDb.exec(duck, "ATTACH " + Identifiers.literal(pgConnectionString(options))
                    + " AS " + PG_ALIAS + " (TYPE POSTGRES)");
        } catch (SQLException e) {
            duck.close();
            throw new SQLException(redact(e.getMessage(), options.password()), e.getSQLState());
        }
        return new DuckPgSession(duck, schema);
    }

    /** libpq key/value string built from the already-resolved JDBC settings. */
    static String pgConnectionString(ImportOptions options) {
        String url = options.jdbcUrl();
        String rest = url.substring(url.indexOf("//") + 2);
        String hostPort = rest.contains("/") ? rest.substring(0, rest.indexOf('/')) : rest;
        String host = hostPort.contains(":") ? hostPort.substring(0, hostPort.indexOf(':')) : hostPort;
        String port = hostPort.contains(":") ? hostPort.substring(hostPort.indexOf(':') + 1) : "5432";
        String dbPart = rest.contains("/") ? rest.substring(rest.indexOf('/') + 1) : "";
        String db = dbPart.contains("?") ? dbPart.substring(0, dbPart.indexOf('?')) : dbPart;
        return "dbname=" + db + " user=" + options.user() + " password=" + options.password()
                + " host=" + host + " port=" + port;
    }

    /** Never let a connection string carrying a password reach a log or an exception message. */
    public static String redact(String message, String password) {
        if (message == null) {
            return null;
        }
        String out = message;
        if (password != null && !password.isEmpty()) {
            out = out.replace(password, "****");
        }
        return out.replaceAll("password=\\S+", "password=****");
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
