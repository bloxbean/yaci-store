package com.bloxbean.cardano.yaci.store.analytics.helper;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Connection strings for postgres_scanner and the PostgreSQL DuckLake catalog: quoting, omitted
 * credentials, libpq parameters from the JDBC URL, redaction, and URLs that are not PostgreSQL.
 */
class DuckDbConnectionStringTest {

    /** Space, single quote, double quote and backslash: every character libpq or SQL treats specially. */
    private static final String HARD_PASSWORD = "p w'd\"x\\y z";

    /** Nothing listens on port 1: libpq fails with "Connection refused" after parsing the string. */
    private static final String UNREACHABLE = "jdbc:postgresql://127.0.0.1:1/yaci?currentSchema=preprod";

    @TempDir
    Path tempDir;

    @Test
    void passwordWithSpacesAndQuotesReachesLibpqIntact() throws Exception {
        assumeTrue(postgresScannerAvailable(), "postgres_scanner extension not available");
        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(
                environment(UNREACHABLE, "yaci", HARD_PASSWORD), new AnalyticsStoreProperties());

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            SQLException e = assertThrows(SQLException.class, () -> helper.attachSourceDatabase(conn, "source_db"));
            assertConnectionFailureNotParseFailure(e);
        }
    }

    @Test
    void catalogConnectionStringIsQuotedToo() throws Exception {
        assumeTrue(postgresScannerAvailable(), "postgres_scanner extension not available");
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(tempDir.resolve("analytics").toString());
        properties.getDucklake().setCatalogType("postgresql");
        properties.getDucklake().setCatalogPassword(HARD_PASSWORD);
        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(
                environment(UNREACHABLE, "yaci", "other"), properties);

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            SQLException e = assertThrows(SQLException.class, () -> helper.attachDuckLakeCatalog(conn, false));
            assertConnectionFailureNotParseFailure(e);
        }
    }

    @Test
    void nullCredentialsAreOmittedInsteadOfSentAsNull() {
        DuckDbConnectionHelper.DatabaseCredentials creds = DuckDbConnectionHelper.parseCredentials(
                "jdbc:postgresql://db/yaci", null, null, "spring.datasource.url");
        String connInfo = DuckDbConnectionHelper.libpqConnInfo(creds, List.of("search_path=public"));

        assertFalse(connInfo.contains("user="), connInfo);
        assertFalse(connInfo.contains("password="), connInfo);
        assertFalse(connInfo.contains("null"), connInfo);
        assertEquals("dbname='yaci' host='db' port='5432' options='-csearch_path=public'", connInfo);
    }

    @Test
    void sslAndConnectionParametersOfTheUrlAreKept() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet notAttached = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(notAttached);

        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(environment(
                "jdbc:postgresql://pg1:5432,[::1]:5433/yaci?currentSchema=preprod&sslmode=verify-full"
                        + "&sslrootcert=/etc/ssl/root.crt&targetServerType=primary", "yaci", "pw"),
                new AnalyticsStoreProperties());
        helper.attachSourceDatabase(connection, "pg_live", 30);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(sql.capture());
        String attach = sql.getValue();
        assertTrue(attach.contains("dbname=''yaci''"), attach);
        assertTrue(attach.contains("host=''pg1,::1'' port=''5432,5433''"), attach);
        assertTrue(attach.contains("sslmode=''verify-full''"), attach);
        assertTrue(attach.contains("sslrootcert=''/etc/ssl/root.crt''"), attach);
        assertTrue(attach.contains("target_session_attrs=''primary''"), attach);
        assertTrue(attach.contains("-csearch_path=preprod -cstatement_timeout=30000"), attach);
    }

    @Test
    void exportSourceStatementTimeoutIsConfigurable() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet notAttached = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(notAttached);

        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(
                environment("jdbc:postgresql://db/yaci", "u", "pw"), properties);
        helper.attachSourceDatabase(connection, "source_db");
        properties.setSourceStatementTimeoutSeconds(600);
        helper.attachSourceDatabase(connection, "source_db2");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(statement, atLeastOnce()).execute(sql.capture());
        List<String> attaches = sql.getAllValues();
        assertFalse(attaches.get(0).contains("statement_timeout"), attaches.get(0));
        assertTrue(attaches.get(1).contains("-cstatement_timeout=600000"), attaches.get(1));
    }

    @Test
    void nonPostgresUrlOnlyFailsWhenPostgresIsNeeded() {
        DuckDbConnectionHelper helper = assertDoesNotThrow(() -> new DuckDbConnectionHelper(
                environment("jdbc:h2:mem:test;MODE=PostgreSQL", "sa", ""), new AnalyticsStoreProperties()));
        assertEquals("public", helper.getSourceCredentials().getSchema());

        SQLException e = assertThrows(SQLException.class,
                () -> helper.attachSourceDatabase(mock(Connection.class), "source_db"));
        assertTrue(e.getMessage().contains("spring.datasource.url"), e.getMessage());
        assertTrue(e.getMessage().contains("jdbc:postgresql"), e.getMessage());

        assertDoesNotThrow(() -> new DuckDbConnectionHelper(
                environment("jdbc:postgresql:yaci", "u", "p"), new AnalyticsStoreProperties()));
    }

    @Test
    void redactsWholePasswordsIncludingSpacesAndEscapedQuotes() {
        DuckDbConnectionHelper.registerSecret("se cret pw");
        assertEquals("dbname=x password=*** host=h",
                DuckDbConnectionHelper.redactSecrets("dbname=x password=se cret pw host=h"));
        assertEquals("password=*** host=h",
                DuckDbConnectionHelper.redactSecrets("password='se cret pw' host=h"));
        // Echoed SQL: the libpq string inside an SQL literal
        assertEquals("ATTACH 'dbname=''x'' password=*** host=''h''' AS a",
                DuckDbConnectionHelper.redactSecrets("ATTACH 'dbname=''x'' password=''lim pw\\''q'' host=''h''' AS a"));
        assertEquals("sslpassword=*** x", DuckDbConnectionHelper.redactSecrets("sslpassword='k p' x"));
        // A registered secret that is a prefix of another password does not stop redaction early
        DuckDbConnectionHelper.registerSecret("abcd");
        assertEquals("password=*** host=h", DuckDbConnectionHelper.redactSecrets("password=abcdefg host=h"));
        // A short common word as password does not garble unrelated text
        assertEquals("the password=*** of user", DuckDbConnectionHelper.redactSecrets("the password=x of user"));
    }

    private static void assertConnectionFailureNotParseFailure(SQLException e) {
        String message = e.getMessage();
        assertFalse(message.contains("missing \"=\""), message);
        assertFalse(message.contains("Parser Error"), message);
        assertFalse(message.contains("Syntax Error"), message);
        assertTrue(message.toLowerCase().contains("connect") || message.toLowerCase().contains("refused"), message);
        assertFalse(message.contains("d\"x"), "password leaked: " + message);
        assertFalse(message.contains("y z"), "password leaked: " + message);
    }

    private static boolean postgresScannerAvailable() {
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            DuckDbConnectionHelper.loadExtension(conn, "postgres_scanner", List.of());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Environment environment(String url, String user, String password) {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.datasource.url")).thenReturn(url);
        when(environment.getProperty("spring.datasource.username")).thenReturn(user);
        when(environment.getProperty("spring.datasource.password")).thenReturn(password);
        return environment;
    }
}
