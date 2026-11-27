package com.bloxbean.cardano.yaci.store.analytics.writer;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.ducklake.DuckLakeWriterLock;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The source PostgreSQL connection dies in the middle of an export (its backend is terminated
 * while the INSERT reads from it, as when PostgreSQL is stopped or restarted). That export
 * fails with the PostgreSQL error, the partition keeps its rows, and later exports on the same
 * writer replace partitions atomically again.
 *
 * <p>Runs only when {@code YACI_ANALYTICS_TEST_PG_HOST}, {@code _PORT}, {@code _DB},
 * {@code _USER} and {@code _PASSWORD} point at a throwaway PostgreSQL whose user may create
 * schemas, e.g.
 * {@code docker run -d -e POSTGRES_PASSWORD=pw -p 127.0.0.1:47731:5432 postgres:16-alpine}.</p>
 */
@EnabledIfEnvironmentVariable(named = "YACI_ANALYTICS_TEST_PG_HOST", matches = ".+")
class DuckLakeWriterSourceFailureIT {

    private static final String HOST = System.getenv("YACI_ANALYTICS_TEST_PG_HOST");
    private static final String PORT = System.getenv().getOrDefault("YACI_ANALYTICS_TEST_PG_PORT", "5432");
    private static final String DB = System.getenv().getOrDefault("YACI_ANALYTICS_TEST_PG_DB", "postgres");
    private static final String USER = System.getenv().getOrDefault("YACI_ANALYTICS_TEST_PG_USER", "postgres");
    private static final String PASSWORD = System.getenv().getOrDefault("YACI_ANALYTICS_TEST_PG_PASSWORD", "");

    @TempDir
    Path tempDir;

    private final String schema = "writer_fault_" + UUID.randomUUID().toString().substring(0, 8);
    private HikariDataSource dataSource;
    private DuckLakeWriterService writer;
    private String exportPath;

    @BeforeEach
    void setUp() throws Exception {
        admin("CREATE SCHEMA " + schema);
        admin("CREATE TABLE " + schema + ".src AS SELECT g::bigint AS id FROM generate_series(1, 20000) g");
        // A source that takes ~1 minute to read, so the export is still reading when it is killed
        admin("CREATE FUNCTION " + schema + ".slow_id(x bigint) RETURNS bigint LANGUAGE plpgsql VOLATILE AS "
                + "$f$ BEGIN PERFORM pg_sleep(0.002); RETURN x; END $f$");
        admin("CREATE VIEW " + schema + ".slow_src AS SELECT " + schema + ".slow_id(id) AS id FROM " + schema + ".src");

        exportPath = tempDir.resolve("analytics").toString();
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(exportPath);
        properties.getStorage().setType("ducklake");
        properties.getDucklake().setCatalogType("duckdb");
        properties.getDucklake().setCatalogPath(tempDir.resolve("analytics/ducklake.catalog.db").toString());

        dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:duckdb:");
        dataSource.setDriverClassName("org.duckdb.DuckDBDriver");
        dataSource.setMaximumPoolSize(1);
        dataSource.setMinimumIdle(1);

        DuckDbConnectionHelper helper = new DuckDbConnectionHelper(environment(), properties);
        writer = new DuckLakeWriterService(dataSource, properties, helper, new DuckLakeWriterLock());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (dataSource != null) {
            dataSource.close();
        }
        admin("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
    }

    @Test
    void aSourceKilledMidExportFailsOnlyThatExport() throws Exception {
        String partition = path("reward", "epoch=7");
        assertThat(writer.export(rows("src", 100), partition, "block_time").getRowCount()).isEqualTo(100);

        CompletableFuture<ExportResult> killed = CompletableFuture.supplyAsync(
                () -> writer.export(rows("slow_src", 20000), partition, "block_time"));
        awaitSourceQuery();
        admin("SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE pid <> pg_backend_pid() "
                + "AND query LIKE '%" + schema + "%slow_src%'");

        assertThatThrownBy(() -> killed.get(60, TimeUnit.SECONDS))
                .hasMessageContaining("terminating connection")
                .hasMessageNotContaining("no transaction is active");
        assertThat(writer.verifyExport(partition, 100)).isTrue();

        // The same writer replaces the partition again, in one snapshot
        assertThat(writer.export(rows("src", 50), partition, "block_time").getRowCount()).isEqualTo(50);
        assertThat(writer.verifyExport(partition, 50)).isTrue();

        // A failing re-export leaves the partition as it was
        assertThatThrownBy(() -> writer.export(
                "SELECT id, 7 AS epoch FROM source_db." + schema + ".src WHERE error('source failed') IS NULL",
                partition, "block_time"))
                .hasMessageContaining("source failed");
        assertThat(writer.verifyExport(partition, 50)).isTrue();
    }

    private String rows(String table, int count) {
        return "SELECT id, 7 AS epoch FROM source_db." + schema + "." + table + " WHERE id <= " + count;
    }

    private void awaitSourceQuery() throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            if (!"0".equals(adminQuery("SELECT count(*)::text FROM pg_stat_activity WHERE pid <> pg_backend_pid() "
                    + "AND state = 'active' AND query LIKE '%" + schema + "%slow_src%'"))) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("the export never started reading the slow source");
    }

    private String path(String table, String partition) {
        return Path.of(exportPath, table, partition, "data.parquet").toString();
    }

    private Environment environment() {
        Environment env = mock(Environment.class);
        when(env.getProperty("spring.datasource.url"))
                .thenReturn("jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB + "?currentSchema=" + schema);
        when(env.getProperty("spring.datasource.username")).thenReturn(USER);
        when(env.getProperty("spring.datasource.password")).thenReturn(PASSWORD);
        return env;
    }

    private static String libpq() {
        return "dbname='" + DB + "' host='" + HOST + "' port='" + PORT + "' user='" + USER + "' password='"
                + PASSWORD.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private static void admin(String sql) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:"); Statement st = c.createStatement()) {
            st.execute("INSTALL postgres");
            st.execute("LOAD postgres");
            st.execute("ATTACH '" + libpq().replace("'", "''") + "' AS adm (TYPE POSTGRES)");
            st.execute("CALL postgres_execute('adm', '" + sql.replace("'", "''") + "')");
        }
    }

    private static String adminQuery(String sql) throws SQLException {
        try (Connection c = DriverManager.getConnection("jdbc:duckdb:"); Statement st = c.createStatement()) {
            st.execute("INSTALL postgres");
            st.execute("LOAD postgres");
            st.execute("ATTACH '" + libpq().replace("'", "''") + "' AS adm (TYPE POSTGRES)");
            try (ResultSet rs = st.executeQuery("SELECT * FROM postgres_query('adm', '" + sql.replace("'", "''") + "')")) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }
}
