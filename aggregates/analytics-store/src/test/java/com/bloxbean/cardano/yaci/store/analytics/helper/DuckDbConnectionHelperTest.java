package com.bloxbean.cardano.yaci.store.analytics.helper;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DuckDbConnectionHelperTest {

    private static final String LIBPQ_ERROR = "IO Error: Unable to connect to Postgres at "
            + "\"dbname=yaci user=usr password=SECRETPW host=db.example port=5432 options=-csearch_path=public\": "
            + "Connection refused";

    @TempDir
    Path tempDir;

    @Test
    void placesStatementTimeoutInEveryPostgresScannerConnectionOption() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = statementWithNothingAttached(connection);

        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(environment(), new AnalyticsStoreProperties());
        helper.attachSourceDatabase(connection, "pg_live", 30);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(statement, times(1)).execute(sql.capture());
        assertTrue(sql.getValue().contains("-csearch_path=cardano -cstatement_timeout=30000"));
        assertTrue(sql.getValue().contains("READ_ONLY"));
        assertFalse(sql.getValue().contains("postgres_execute"));
    }

    @Test
    void sourceAttachFailureDoesNotLeakPassword() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = statementWithNothingAttached(connection);
        when(statement.execute(anyString())).thenThrow(new SQLException(LIBPQ_ERROR, "HY000", 7));

        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(environment(), new AnalyticsStoreProperties());
        SQLException thrown = assertThrows(SQLException.class,
                () -> helper.attachSourceDatabase(connection, "pg_live", 30));

        assertRedacted(thrown);
        assertTrue(thrown.getMessage().contains("pg_live"));
    }

    @Test
    void postgresCatalogAttachFailureDoesNotLeakPassword() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = statementWithNothingAttached(connection);
        when(statement.execute(startsWith("ATTACH"))).thenThrow(new SQLException(LIBPQ_ERROR, "HY000", 7));

        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(tempDir.resolve("analytics").toString());
        properties.getDucklake().setCatalogType("postgresql");
        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(environment(), properties);

        SQLException thrown = assertThrows(SQLException.class,
                () -> helper.attachDuckLakeCatalog(connection, true));

        assertRedacted(thrown);
        assertTrue(thrown.getMessage().contains("DuckLake catalog"));
    }

    @Test
    void catalogSchemaBootstrapAttachFailureDoesNotLeakPassword() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = statementWithNothingAttached(connection);
        when(statement.execute(startsWith("ATTACH"))).thenThrow(new SQLException(LIBPQ_ERROR, "HY000", 7));

        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.getDucklake().setCatalogType("postgresql");
        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(environment(), properties);

        SQLException thrown = assertThrows(SQLException.class,
                () -> helper.ensureCatalogSchemaExists(connection));

        assertRedacted(thrown);
    }

    @Test
    void crossInstanceFileHandleConflictIsNotMistakenForAlreadyAttached() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = statementWithNothingAttached(connection);
        when(statement.execute(startsWith("ATTACH"))).thenThrow(new SQLException(
                "Binder Error: Failed to attach DuckLake MetaData \"__ducklake_metadata_ducklake_catalog\" at path + "
                        + "\"./data/analytics/ducklake.catalog.db\"Unique file handle conflict: Cannot attach "
                        + "\"__ducklake_metadata_ducklake_catalog\" - the database file \"./data/analytics/ducklake.catalog.db\" "
                        + "is already attached by database \"__ducklake_metadata_ducklake_catalog\""));

        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(tempDir.resolve("analytics").toString());
        properties.getDucklake().setCatalogType("duckdb");
        properties.getDucklake().setCatalogPath(tempDir.resolve("analytics/ducklake.catalog.db").toString());
        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(environment(), properties);

        SQLException thrown = assertThrows(SQLException.class,
                () -> helper.attachDuckLakeCatalog(connection, true));
        assertTrue(thrown.getMessage().contains("another DuckDB instance"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("Unique file handle conflict"), thrown.getMessage());
    }

    @Test
    void sameAliasAlreadyExistsIsTreatedAsAlreadyAttached() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = statementWithNothingAttached(connection);
        when(statement.execute(startsWith("ATTACH"))).thenThrow(new SQLException(
                "Binder Error: Failed to attach database: database with name \"ducklake_catalog\" already exists"));

        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(tempDir.resolve("analytics").toString());
        properties.getDucklake().setCatalogType("duckdb");
        properties.getDucklake().setCatalogPath(tempDir.resolve("analytics/ducklake.catalog.db").toString());
        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(environment(), properties);

        helper.attachDuckLakeCatalog(connection, true); // must not throw
    }

    @Test
    void skipsAttachWhenCatalogIsAlreadyAttachedOnTheConnection() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet attached = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(attached);
        when(attached.next()).thenReturn(true);

        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(tempDir.resolve("analytics").toString());
        properties.getDucklake().setCatalogType("duckdb");
        properties.getDucklake().setCatalogPath(tempDir.resolve("analytics/ducklake.catalog.db").toString());
        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(environment(), properties);

        helper.attachDuckLakeCatalog(connection, false);
        verify(statement, never()).execute(startsWith("ATTACH"));
    }

    @Test
    void redactsQuotedAndBarePasswordValues() {
        assertEquals("dbname=x password=*** host=h",
                DuckDbConnectionHelper.redactSecrets("dbname=x password=SECRETPW host=h"));
        assertEquals("dbname='x' password=*** host='h'",
                DuckDbConnectionHelper.redactSecrets("dbname='x' password='se cret\\'s' host='h'"));
        assertEquals("Password=*** trailing",
                DuckDbConnectionHelper.redactSecrets("Password=abc trailing"));
        assertEquals("no secrets here", DuckDbConnectionHelper.redactSecrets("no secrets here"));
        assertNull(DuckDbConnectionHelper.redactSecrets(null));
    }

    @Test
    void sanitizeKeepsDiagnosticsButDropsCauseChain() {
        SQLException raw = new SQLException(LIBPQ_ERROR, "08001", 42, new RuntimeException(LIBPQ_ERROR));
        SQLException safe = DuckDbConnectionHelper.sanitize(raw);

        assertRedacted(safe);
        assertEquals("08001", safe.getSQLState());
        assertEquals(42, safe.getErrorCode());
        assertNull(safe.getCause());
        assertEquals(raw.getStackTrace()[0], safe.getStackTrace()[0]);
    }

    private static void assertRedacted(SQLException e) {
        assertFalse(e.getMessage().contains("SECRETPW"), e.getMessage());
        assertTrue(e.getMessage().contains("password=***"), e.getMessage());
        assertTrue(e.getMessage().contains("Connection refused"), e.getMessage());
        assertNull(e.getCause());
        assertTrue("HY000".equals(e.getSQLState()) || "08001".equals(e.getSQLState()), e.getSQLState());
    }

    /** A mocked statement whose duckdb_databases() probe reports nothing attached. */
    private static Statement statementWithNothingAttached(Connection connection) throws SQLException {
        Statement statement = mock(Statement.class);
        ResultSet notAttached = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(notAttached);
        when(notAttached.next()).thenReturn(false);
        return statement;
    }

    private static Environment environment() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.datasource.url"))
                .thenReturn("jdbc:postgresql://db.example:5432/yaci?currentSchema=cardano");
        when(environment.getProperty("spring.datasource.username")).thenReturn("reader");
        when(environment.getProperty("spring.datasource.password")).thenReturn("SECRETPW");
        return environment;
    }
}
