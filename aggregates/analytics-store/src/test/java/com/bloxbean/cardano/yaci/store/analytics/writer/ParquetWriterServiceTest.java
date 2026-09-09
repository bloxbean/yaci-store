package com.bloxbean.cardano.yaci.store.analytics.writer;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Parquet storage mode ({@code storage.type=parquet}) writer path on the real DuckDB engine:
 * the writer pool (in-memory DuckDB, pool size 1) as configured by
 * {@code DuckDbDataSourceConfig}, the shared {@link DuckDbConnectionHelper}, and
 * {@link ParquetWriterService}'s {@code COPY ... TO tmp -> atomic rename} flow.
 *
 * <p>The PostgreSQL source is replaced by an in-memory DuckDB database attached under the
 * same {@code source_db} alias, so the writer takes its "already attached" branch; the
 * PostgreSQL attach statement itself is covered by {@code DuckDbConnectionHelperTest}.</p>
 */
class ParquetWriterServiceTest {

    @TempDir
    Path exportDir;

    private HikariDataSource writerDataSource;
    private ParquetWriterService writer;

    @BeforeEach
    void setUp() throws Exception {
        writerDataSource = new HikariDataSource();
        writerDataSource.setJdbcUrl("jdbc:duckdb:");
        writerDataSource.setDriverClassName("org.duckdb.DuckDBDriver");
        writerDataSource.setMaximumPoolSize(1);
        writerDataSource.setMinimumIdle(1);
        writerDataSource.setConnectionTimeout(2000);

        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(exportDir.toString());
        properties.getStorage().setType("parquet");
        properties.getParquetExport().setCodec("ZSTD");
        properties.getParquetExport().setCompressionLevel(3);

        // Stand-in for the PostgreSQL source: attach an in-memory database as "source_db" on
        // the single pooled connection and seed it with block-like rows.
        try (Connection conn = writerDataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("ATTACH ':memory:' AS source_db");
            stmt.execute("CREATE TABLE source_db.main.block AS "
                    + "SELECT range AS slot, range * 10 AS number, to_timestamp(1700000000 + range) AS block_time FROM range(100)");
        }

        writer = new ParquetWriterService(writerDataSource, properties, new DuckDbConnectionHelper(environment(), properties));
    }

    @AfterEach
    void tearDown() {
        if (writerDataSource != null) writerDataSource.close();
    }

    @Test
    void exportsQueryResultAtomicallyToTheHivePartitionPath() throws Exception {
        Path output = exportDir.resolve("block/date=2024-01-15/data.parquet");
        // An orphaned .tmp from a previous crashed run must be cleaned up, not left behind
        Files.createDirectories(output.getParent());
        Files.writeString(output.getParent().resolve("stale.parquet.tmp"), "junk");

        ExportResult result = writer.export(
                "SELECT * FROM source_db.main.block WHERE slot < 40", output.toString(), "block_time");

        assertEquals(output.toString(), result.getFilePath());
        assertEquals(40, result.getRowCount());
        assertTrue(result.getFileSize() > 0);
        assertTrue(Files.isRegularFile(output));
        try (Stream<Path> files = Files.list(output.getParent())) {
            assertTrue(files.noneMatch(p -> p.toString().endsWith(".tmp")), "no temp files may remain");
        }
        assertEquals(40, readParquetCount(output));

        // Re-export of the same partition replaces the file in place (rename over existing)
        ExportResult second = writer.export(
                "SELECT * FROM source_db.main.block WHERE slot < 10", output.toString(), "block_time");
        assertEquals(10, second.getRowCount());
        assertEquals(10, readParquetCount(output));
    }

    @Test
    void failedExportLeavesNoPartialOutput() throws Exception {
        Path output = exportDir.resolve("block/date=2024-01-16/data.parquet");

        RuntimeException failure = assertThrows(RuntimeException.class, () -> writer.export(
                "SELECT * FROM source_db.main.no_such_table", output.toString(), "block_time"));
        assertTrue(failure.getMessage().startsWith("Export to Parquet failed"), failure.getMessage());
        assertFalse(Files.exists(output), "no output file for a failed export");
    }

    private static long readParquetCount(Path parquet) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT count(*) FROM read_parquet('"
                     + parquet.toString().replace("'", "''") + "')")) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static Environment environment() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.datasource.url"))
                .thenReturn("jdbc:postgresql://localhost:5432/yaci?currentSchema=main");
        when(environment.getProperty("spring.datasource.username")).thenReturn("user");
        when(environment.getProperty("spring.datasource.password")).thenReturn("password");
        return environment;
    }
}
