package com.bloxbean.cardano.yaci.store.analytics.helper;

import lombok.extern.slf4j.Slf4j;
import org.duckdb.DuckDBConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Base class for DuckDB read connection management using {@code DuckDBConnection.duplicate()}.
 *
 * <p>Creates a single parent DuckDB connection at startup and uses {@code duplicate()} to
 * create lightweight read handles that share the same underlying database instance.
 * This is the standard DuckDB practice for concurrent reads — more efficient than
 * HikariCP which would create N separate in-memory databases.</p>
 *
 * <p>Concurrency is controlled via a {@link Semaphore} rather than a connection pool.</p>
 *
 * <p>Subclasses configure the parent connection in their {@code @PostConstruct} method:</p>
 * <ul>
 *   <li>{@code DuckLakeReadConnectionProvider} — attaches DuckLake catalog</li>
 *   <li>{@code ParquetReadConnectionProvider} — creates views for Parquet files</li>
 * </ul>
 */
@Slf4j
public abstract class DuckDbReadConnectionProvider {

    private Connection parentConnection;
    private final Semaphore semaphore;
    private final int queryTimeoutSeconds;

    protected DuckDbReadConnectionProvider(int maxConcurrent, String memoryLimit, int threads, int queryTimeoutSeconds) {
        this.queryTimeoutSeconds = queryTimeoutSeconds;
        this.semaphore = new Semaphore(maxConcurrent);

        try {
            this.parentConnection = DriverManager.getConnection("jdbc:duckdb:");

            try (Statement stmt = parentConnection.createStatement()) {
                if (memoryLimit != null && !memoryLimit.isBlank()) {
                    stmt.execute("SET memory_limit = '" + memoryLimit + "'");
                    log.info("DuckDB memory_limit set to {}", memoryLimit);
                }
                stmt.execute("SET threads = " + threads);
                log.info("DuckDB threads set to {}", threads);
            }

            log.info("DuckDB parent connection created (maxConcurrent={}, queryTimeout={}s)",
                    maxConcurrent, queryTimeoutSeconds);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create DuckDB parent connection", e);
        }
    }

    /**
     * Access the parent connection for subclass initialization (creating views, attaching catalogs).
     */
    protected Connection getParentConnection() {
        return parentConnection;
    }

    /**
     * Get a read connection (via duplicate()) with semaphore-based concurrency control.
     *
     * <p>The returned connection MUST be closed by the caller. Closing the connection
     * releases the semaphore permit automatically.</p>
     *
     * @return a DuckDB read connection that shares the parent's database state
     * @throws RuntimeException if the semaphore cannot be acquired within the timeout
     */
    public Connection getReadConnection() {
        try {
            if (!semaphore.tryAcquire(queryTimeoutSeconds, TimeUnit.SECONDS)) {
                throw new RuntimeException("Max concurrent DuckDB queries reached. Try again later.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for DuckDB read connection", e);
        }

        try {
            DuckDBConnection duckConn = (DuckDBConnection) parentConnection;
            Connection duplicated = duckConn.duplicate();
            return new SemaphoreReleasingConnection(duplicated, semaphore);
        } catch (SQLException e) {
            semaphore.release();
            throw new RuntimeException("Failed to duplicate DuckDB connection", e);
        }
    }

    /**
     * Get the configured query timeout in seconds.
     */
    public int getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    /**
     * Lock down the parent connection after subclass initialization.
     *
     * <p>Must be called by subclasses AFTER they finish creating views (which require
     * {@code read_parquet()} and {@code postgres_scanner}) but BEFORE serving any user queries.</p>
     *
     * <p><b>Security measures applied:</b></p>
     * <ul>
     *   <li>{@code autoload_known_extensions = false} — prevents loading new extensions
     *       (e.g., {@code httpfs}, {@code spatial}) via user queries.</li>
     *   <li>{@code autoinstall_known_extensions = false} — prevents downloading and installing
     *       extensions from the DuckDB extension repository.</li>
     * </ul>
     *
     * <p><b>Why {@code enable_external_access} is NOT disabled:</b>
     * DuckDB views resolve {@code read_parquet()} lazily at query time, not at view creation
     * time. Disabling external access would break all Parquet views. Protection against
     * file-access injection (e.g., {@code read_csv('/etc/passwd')}) is enforced at the
     * application layer by {@code SqlValidator}, which strips comments and blocks dangerous
     * function names before queries reach DuckDB.</p>
     *
     * @throws RuntimeException if any DuckDB SET command fails
     */
    protected void lockDown() {
        try (Statement stmt = parentConnection.createStatement()) {
            // NOTE: enable_external_access CANNOT be set to false here because DuckDB views
            // resolve read_parquet() lazily at query time. Disabling it breaks all Parquet views.
            // File I/O protection is handled by SqlValidator (comment stripping + keyword blocklist).
            stmt.execute("SET autoload_known_extensions = false");
            stmt.execute("SET autoinstall_known_extensions = false");
            log.info("DuckDB locked down: autoload=false, autoinstall=false " +
                    "(external_access=true required for Parquet view resolution)");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to lock down DuckDB parent connection", e);
        }
    }

    /**
     * Execute a statement on the parent connection (e.g., to create or refresh views).
     * This is thread-safe because DuckDB parent connection supports concurrent reads
     * while DDL on parent is serialized by the caller.
     */
    protected void executeOnParent(String sql) throws SQLException {
        try (Statement stmt = parentConnection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Connection wrapper that releases the semaphore when closed.
     */
    private static class SemaphoreReleasingConnection implements Connection, AutoCloseable {
        private final Connection delegate;
        private final Semaphore semaphore;
        private boolean closed = false;

        SemaphoreReleasingConnection(Connection delegate, Semaphore semaphore) {
            this.delegate = delegate;
            this.semaphore = semaphore;
        }

        @Override
        public void close() throws SQLException {
            if (!closed) {
                closed = true;
                try {
                    delegate.close();
                } finally {
                    semaphore.release();
                }
            }
        }

        // Delegate all Connection methods to the underlying connection
        @Override public java.sql.Statement createStatement() throws SQLException { return delegate.createStatement(); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException { return delegate.prepareStatement(sql); }
        @Override public java.sql.CallableStatement prepareCall(String sql) throws SQLException { return delegate.prepareCall(sql); }
        @Override public String nativeSQL(String sql) throws SQLException { return delegate.nativeSQL(sql); }
        @Override public void setAutoCommit(boolean autoCommit) throws SQLException { delegate.setAutoCommit(autoCommit); }
        @Override public boolean getAutoCommit() throws SQLException { return delegate.getAutoCommit(); }
        @Override public void commit() throws SQLException { delegate.commit(); }
        @Override public void rollback() throws SQLException { delegate.rollback(); }
        @Override public boolean isClosed() throws SQLException { return closed || delegate.isClosed(); }
        @Override public java.sql.DatabaseMetaData getMetaData() throws SQLException { return delegate.getMetaData(); }
        @Override public void setReadOnly(boolean readOnly) throws SQLException { delegate.setReadOnly(readOnly); }
        @Override public boolean isReadOnly() throws SQLException { return delegate.isReadOnly(); }
        @Override public void setCatalog(String catalog) throws SQLException { delegate.setCatalog(catalog); }
        @Override public String getCatalog() throws SQLException { return delegate.getCatalog(); }
        @Override public void setTransactionIsolation(int level) throws SQLException { delegate.setTransactionIsolation(level); }
        @Override public int getTransactionIsolation() throws SQLException { return delegate.getTransactionIsolation(); }
        @Override public java.sql.SQLWarning getWarnings() throws SQLException { return delegate.getWarnings(); }
        @Override public void clearWarnings() throws SQLException { delegate.clearWarnings(); }
        @Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.createStatement(resultSetType, resultSetConcurrency); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency); }
        @Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException { return delegate.prepareCall(sql, resultSetType, resultSetConcurrency); }
        @Override public java.util.Map<String, Class<?>> getTypeMap() throws SQLException { return delegate.getTypeMap(); }
        @Override public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException { delegate.setTypeMap(map); }
        @Override public void setHoldability(int holdability) throws SQLException { delegate.setHoldability(holdability); }
        @Override public int getHoldability() throws SQLException { return delegate.getHoldability(); }
        @Override public java.sql.Savepoint setSavepoint() throws SQLException { return delegate.setSavepoint(); }
        @Override public java.sql.Savepoint setSavepoint(String name) throws SQLException { return delegate.setSavepoint(name); }
        @Override public void rollback(java.sql.Savepoint savepoint) throws SQLException { delegate.rollback(savepoint); }
        @Override public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException { delegate.releaseSavepoint(savepoint); }
        @Override public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public java.sql.CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException { return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException { return delegate.prepareStatement(sql, autoGeneratedKeys); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException { return delegate.prepareStatement(sql, columnIndexes); }
        @Override public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException { return delegate.prepareStatement(sql, columnNames); }
        @Override public java.sql.Clob createClob() throws SQLException { return delegate.createClob(); }
        @Override public java.sql.Blob createBlob() throws SQLException { return delegate.createBlob(); }
        @Override public java.sql.NClob createNClob() throws SQLException { return delegate.createNClob(); }
        @Override public java.sql.SQLXML createSQLXML() throws SQLException { return delegate.createSQLXML(); }
        @Override public boolean isValid(int timeout) throws SQLException { return delegate.isValid(timeout); }
        @Override public void setClientInfo(String name, String value) throws java.sql.SQLClientInfoException { delegate.setClientInfo(name, value); }
        @Override public void setClientInfo(java.util.Properties properties) throws java.sql.SQLClientInfoException { delegate.setClientInfo(properties); }
        @Override public String getClientInfo(String name) throws SQLException { return delegate.getClientInfo(name); }
        @Override public java.util.Properties getClientInfo() throws SQLException { return delegate.getClientInfo(); }
        @Override public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException { return delegate.createArrayOf(typeName, elements); }
        @Override public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException { return delegate.createStruct(typeName, attributes); }
        @Override public void setSchema(String schema) throws SQLException { delegate.setSchema(schema); }
        @Override public String getSchema() throws SQLException { return delegate.getSchema(); }
        @Override public void abort(java.util.concurrent.Executor executor) throws SQLException { delegate.abort(executor); }
        @Override public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds) throws SQLException { delegate.setNetworkTimeout(executor, milliseconds); }
        @Override public int getNetworkTimeout() throws SQLException { return delegate.getNetworkTimeout(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { return delegate.unwrap(iface); }
        @Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return delegate.isWrapperFor(iface); }
    }
}
