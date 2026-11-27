package com.bloxbean.cardano.yaci.store.analytics.helper;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.ducklake.DuckDbCatalogPreInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;

/**
 * DuckDB-file DuckLake catalog: paths that need SQL escaping, DATA_PATH spelled differently,
 * a catalog of another directory, a catalog file locked by another process, and catalog-type
 * validation. Skipped when the DuckLake extension is not available.
 */
class DuckDbFileCatalogAttachTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void requireDuckLake() {
        assumeTrue(duckLakeAvailable(), "DuckLake extension not available");
    }

    @Test
    void quoteInThePathsDoesNotBreakStartup() throws Exception {
        Path base = Files.createDirectories(tempDir.resolve("it's here"));
        AnalyticsStoreProperties properties = properties(base.resolve("analytics"), base.resolve("cat'alog.db"));

        new DuckDbCatalogPreInitializer(properties).afterPropertiesSet();
        assertTrue(Files.isRegularFile(base.resolve("cat'alog.db")));

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            helper(properties).prepareConnectionForDuckLake(conn, false, false);
            exec(conn, "CREATE TABLE t AS SELECT 1 AS a");
        }
    }

    @Test
    void sameDirectoryWrittenDifferentlyUsesTheRecordedPath() throws Exception {
        Path data = Files.createDirectories(tempDir.resolve("data/analytics"));
        Path catalog = tempDir.resolve("catalog.db");
        createCatalog(properties(data, catalog));

        // Same directory: through a symbolic link, and with redundant segments
        Path link = Files.createSymbolicLink(tempDir.resolve("link"), tempDir.resolve("data"));
        for (String spelling : List.of(link.resolve("analytics").toString(), tempDir + "/data/./x/../analytics/")) {
            try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
                helper(properties(Path.of(spelling), catalog)).prepareConnectionForDuckLake(conn, false, false);
                assertEquals(1, single(conn, "SELECT count(*) FROM t"));
                assertEquals(data.toString() + "/", singleString(conn,
                        "SELECT value FROM ducklake_options('ducklake_catalog') WHERE option_name = 'data_path'"));
            }
        }
    }

    @Test
    void catalogOfAnotherDirectoryIsRefusedWithAClearMessage() throws Exception {
        Path data = Files.createDirectories(tempDir.resolve("a"));
        Path catalog = tempDir.resolve("catalog.db");
        createCatalog(properties(data, catalog));

        Path other = Files.createDirectories(tempDir.resolve("b"));
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            SQLException e = assertThrows(SQLException.class,
                    () -> helper(properties(other, catalog)).attachDuckLakeCatalog(conn, false));
            assertTrue(e.getMessage().contains("export-path"), e.getMessage());
            assertTrue(e.getMessage().contains(data.toString()), e.getMessage());
            assertEquals(0, single(conn, "SELECT count(*) FROM duckdb_databases() WHERE database_name = 'ducklake_catalog'"));
        }
    }

    @Test
    void lockedCatalogFailsWithAnActionableMessageAfterTheWait() throws Exception {
        Path data = Files.createDirectories(tempDir.resolve("analytics"));
        Path catalog = tempDir.resolve("catalog.db");
        AnalyticsStoreProperties properties = properties(data, catalog);
        createCatalog(properties);
        properties.getDucklake().setCatalogLockWaitSeconds(1);

        Process holder = holdFile(catalog, 30);
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            long start = System.nanoTime();
            SQLException e = assertThrows(SQLException.class, () -> helper(properties).attachDuckLakeCatalog(conn, false));
            long waited = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            assertTrue(e.getMessage().contains("locked by another process"), e.getMessage());
            assertTrue(e.getMessage().contains("duckdb CLI"), e.getMessage());
            assertTrue(waited >= 900, "waited " + waited + " ms");
        } finally {
            holder.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void lockReleasedWhileWaitingIsPickedUp() throws Exception {
        Path data = Files.createDirectories(tempDir.resolve("analytics"));
        Path catalog = tempDir.resolve("catalog.db");
        AnalyticsStoreProperties properties = properties(data, catalog);
        createCatalog(properties);
        properties.getDucklake().setCatalogLockWaitSeconds(30);

        Process holder = holdFile(catalog, 2);
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            assertDoesNotThrow(() -> helper(properties).prepareConnectionForDuckLake(conn, false, false));
            assertEquals(1, single(conn, "SELECT count(*) FROM t"));
        } finally {
            holder.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void unknownCatalogTypeIsRejected() {
        assertEquals(DuckDbConnectionHelper.CatalogType.DUCKDB, DuckDbConnectionHelper.CatalogType.of(" DuckDB "));
        assertEquals(DuckDbConnectionHelper.CatalogType.POSTGRESQL, DuckDbConnectionHelper.CatalogType.of("PostgreSQL"));

        AnalyticsStoreProperties properties = properties(tempDir.resolve("a"), tempDir.resolve("c.db"));
        properties.getDucklake().setCatalogType("postgres");
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new DuckDbCatalogPreInitializer(properties).afterPropertiesSet());
        assertTrue(e.getMessage().contains("'postgres'"), e.getMessage());
        assertTrue(e.getMessage().contains("'postgresql'") && e.getMessage().contains("'duckdb'"), e.getMessage());
        assertThrows(IllegalStateException.class,
                () -> helper(properties).attachDuckLakeCatalog(mock(Connection.class), false));
    }

    // ---------------------------------------------------------------------

    private void createCatalog(AnalyticsStoreProperties properties) throws Exception {
        new DuckDbCatalogPreInitializer(properties).afterPropertiesSet();
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            helper(properties).prepareConnectionForDuckLake(conn, false, false);
            exec(conn, "CREATE TABLE t AS SELECT 1 AS a");
        }
    }

    /** Another process that opens the catalog file read-only, like {@code duckdb -readonly}. */
    private static Process holdFile(Path file, int seconds) throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"),
                CatalogFileHolder.class.getName(), file.toString(), Integer.toString(seconds))
                .redirectErrorStream(true)
                .start();
        BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
        String line = out.readLine();
        assumeTrue("READY".equals(line), "could not start a process holding the catalog: " + line);
        return process;
    }

    /** Child-process entry point: holds a read-only DuckDB connection to a file. */
    public static final class CatalogFileHolder {
        public static void main(String[] args) throws Exception {
            Properties config = new Properties();
            config.setProperty("duckdb.read_only", "true");
            try (Connection ignored = DriverManager.getConnection("jdbc:duckdb:" + args[0], config)) {
                System.out.println("READY");
                System.out.flush();
                Thread.sleep(Long.parseLong(args[1]) * 1000L);
            }
        }
    }

    private static AnalyticsStoreProperties properties(Path exportPath, Path catalogPath) {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(exportPath.toString());
        properties.getStorage().setType("ducklake");
        properties.getDucklake().setCatalogType("duckdb");
        properties.getDucklake().setCatalogPath(catalogPath.toString());
        properties.getDuckdb().setTempDirectory(exportPath.resolveSibling("spill").toString());
        return properties;
    }

    private static DuckDbConnectionHelper helper(AnalyticsStoreProperties properties) {
        return new DuckDbConnectionHelper(mock(Environment.class), properties);
    }

    private static void exec(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        }
    }

    private static long single(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static String singleString(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static boolean duckLakeAvailable() {
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            DuckDbConnectionHelper.loadExtension(conn, "ducklake", List.of());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
