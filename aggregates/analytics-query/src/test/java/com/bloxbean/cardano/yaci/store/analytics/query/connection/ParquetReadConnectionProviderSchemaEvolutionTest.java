package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.ducklake.DuckLakeCatalogSnapshotReader;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Views over Parquet files written before and after a column was added (the analytics writer
 * adds missing columns over time). read_parquet takes its schema from the first file unless
 * union_by_name is set, so both file orders must be covered.
 */
class ParquetReadConnectionProviderSchemaEvolutionTest {

    @TempDir
    Path exportDir;

    private AnalyticsStoreProperties properties;

    @BeforeEach
    void setUp() {
        properties = new AnalyticsStoreProperties();
        properties.setExportPath(exportDir.toString());
        properties.getStorage().setType("parquet");
        properties.getDuckdb().setThreads(2);
        properties.getDuckdb().getReader().setMaximumPoolSize(2);
    }

    @Test
    void oldFileFirstKeepsTheAddedColumn() throws Exception {
        // Paths sort old file first: the view used to take the old schema and drop 'fee'.
        writeParquet("tx_old_first/date=2024-01-01/data.parquet", "SELECT 1::BIGINT AS slot");
        writeParquet("tx_old_first/date=2024-01-02/data.parquet", "SELECT 2::BIGINT AS slot, 500::BIGINT AS fee");

        ParquetReadConnectionProvider provider = createProvider();

        assertEquals(2, queryLong(provider, "SELECT count(*) FROM tx_old_first"));
        assertEquals(500, queryLong(provider, "SELECT sum(fee) FROM tx_old_first"));
        assertEquals(1, queryLong(provider, "SELECT count(*) FROM tx_old_first WHERE fee IS NULL"));
    }

    @Test
    void newFileFirstDoesNotFailOnOlderFiles() throws Exception {
        // Paths sort new file first: scanning the old file used to fail with "schema mismatch".
        writeParquet("tx_new_first/a.parquet", "SELECT 2::BIGINT AS slot, 500::BIGINT AS fee");
        writeParquet("tx_new_first/b.parquet", "SELECT 1::BIGINT AS slot");

        ParquetReadConnectionProvider provider = createProvider();

        assertEquals(2, queryLong(provider, "SELECT count(*) FROM tx_new_first"));
        assertEquals(3, queryLong(provider, "SELECT sum(slot) FROM tx_new_first"));
        assertEquals(500, queryLong(provider, "SELECT sum(fee) FROM tx_new_first"));
    }

    @Test
    void filesWithOneSchemaDoNotUseUnionByName() throws Exception {
        writeParquet("same/a.parquet", "SELECT 1::BIGINT AS slot, 1::BIGINT AS fee");
        writeParquet("same/b.parquet", "SELECT 2::BIGINT AS fee, 2::BIGINT AS slot"); // order does not matter
        writeParquet("mixed/a.parquet", "SELECT 1::BIGINT AS slot");
        writeParquet("mixed/b.parquet", "SELECT 2::BIGINT AS slot, 'x' AS extra");
        writeParquet("retyped/a.parquet", "SELECT 1::INTEGER AS slot");
        writeParquet("retyped/b.parquet", "SELECT 2::BIGINT AS slot");

        ParquetReadConnectionProvider provider = createProvider();
        ParquetTableRegistry registry = new ParquetTableRegistry(properties);
        registry.init();

        assertFalse(provider.needsUnionByName("same", registry.listParquetFiles("same")));
        assertTrue(provider.needsUnionByName("mixed", registry.listParquetFiles("mixed")));
        assertTrue(provider.needsUnionByName("retyped", registry.listParquetFiles("retyped")));
        assertEquals(3, queryLong(provider, "SELECT sum(slot) FROM same"));
        assertEquals(3, queryLong(provider, "SELECT sum(slot) FROM retyped"));
    }

    @Test
    void refreshSwitchesToUnionByNameWhenANewColumnArrives() throws Exception {
        writeParquet("blk/date=2024-01-01/data.parquet", "SELECT 1::BIGINT AS slot");
        writeParquet("blk/date=2024-01-02/data.parquet", "SELECT 2::BIGINT AS slot");
        ParquetReadConnectionProvider provider = createProvider();
        assertEquals(3, queryLong(provider, "SELECT sum(slot) FROM blk"));

        writeParquet("blk/date=2024-01-03/data.parquet", "SELECT 3::BIGINT AS slot, 7::BIGINT AS size");
        provider.refreshViews();

        assertEquals(6, queryLong(provider, "SELECT sum(slot) FROM blk"));
        assertEquals(7, queryLong(provider, "SELECT sum(size) FROM blk"));

        // Footers of new files are read on the locked-down instance too (a failed read would
        // also answer true, so check a uniform table).
        writeParquet("uni/a.parquet", "SELECT 1::BIGINT AS slot");
        writeParquet("uni/b.parquet", "SELECT 2::BIGINT AS slot");
        ParquetTableRegistry registry = new ParquetTableRegistry(properties);
        assertFalse(provider.needsUnionByName("uni", registry.listParquetFiles("uni")));
    }

    private ParquetReadConnectionProvider createProvider() {
        ParquetTableRegistry registry = new ParquetTableRegistry(properties);
        registry.init();
        ParquetReadConnectionProvider provider = new ParquetReadConnectionProvider(
                properties, registry,
                objectProvider(DuckDbConnectionHelper.class, null),
                objectProvider(CutoffSlotResolver.class, null),
                objectProvider(TableExporterRegistry.class, null),
                objectProvider(DuckLakeCatalogSnapshotReader.class, null));
        provider.createViews();
        return provider;
    }

    private void writeParquet(String relativePath, String query) throws Exception {
        Path target = exportDir.resolve(relativePath);
        Files.createDirectories(target.getParent());
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement stmt = conn.createStatement()) {
            stmt.execute("COPY (" + query + ") TO '" + target.toString().replace("'", "''") + "' (FORMAT parquet)");
        }
    }

    private static long queryLong(ParquetReadConnectionProvider provider, String sql) throws SQLException {
        try (Connection conn = provider.getReadConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> objectProvider(Class<T> type, T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
