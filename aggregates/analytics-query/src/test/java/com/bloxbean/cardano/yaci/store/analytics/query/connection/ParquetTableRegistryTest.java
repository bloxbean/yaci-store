package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParquetTableRegistryTest {

    @TempDir
    Path exportDirectory;

    @Test
    void scansDirectParquetRootAndFindsTablesAddedAfterStartup() throws Exception {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(exportDirectory.toString());
        properties.getStorage().setType("parquet");
        writeParquetMarker("block");

        ParquetTableRegistry registry = new ParquetTableRegistry(properties);
        registry.init();
        assertEquals(List.of("block"), registry.getTableNames());
        assertTrue(registry.getParquetGlobPath("block").contains("/block/"));

        writeParquetMarker("transaction");
        registry.refresh();
        assertEquals(List.of("block", "transaction"), registry.getTableNames());
    }

    @Test
    void duckLakeNamesComeFromCatalogRatherThanRawDirectoryScan() throws Exception {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(exportDirectory.toString());
        properties.getStorage().setType("ducklake");
        writeParquetMarker("uncommitted_file");

        ParquetTableRegistry registry = new ParquetTableRegistry(properties);
        registry.init();
        assertTrue(registry.getTableNames().isEmpty());

        registry.replaceTableNames(List.of("transaction", "block"));
        assertEquals(List.of("block", "transaction"), registry.getTableNames());
    }

    @Test
    void duckLakeSnapshotRegistersOnlyTablesWithCommittedFiles() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(exportDirectory.toString());
        properties.getStorage().setType("ducklake");
        ParquetTableRegistry registry = new ParquetTableRegistry(properties);

        Path blockFile = exportDirectory.resolve("main/block/date=2026-01-01/ducklake-1.parquet");
        registry.replaceDuckLakeSnapshot(Map.of(
                "transaction", List.of(),
                "block", List.of(blockFile)));

        assertEquals(List.of("block"), registry.getTableNames());
        assertEquals(List.of(blockFile), registry.getDuckLakeFiles("block"));
        assertTrue(registry.getDuckLakeFiles("transaction").isEmpty());
        assertThrows(IllegalStateException.class, () -> registry.getParquetGlobPath("block"));
    }

    @Test
    void duckLakeSnapshotKeepsEmptyTablesWhenCatalogColumnsAreKnown() {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.setExportPath(exportDirectory.toString());
        properties.getStorage().setType("ducklake");
        ParquetTableRegistry registry = new ParquetTableRegistry(properties);

        registry.replaceDuckLakeSnapshot(
                Map.of("instant_reward", List.of()),
                Map.of("instant_reward", List.of(
                        new ParquetTableRegistry.TableColumn("epoch", "INTEGER"),
                        new ParquetTableRegistry.TableColumn("amount", "DECIMAL(38,0)"))));

        assertEquals(List.of("instant_reward"), registry.getTableNames());
        assertTrue(registry.getDuckLakeFiles("instant_reward").isEmpty());
        assertEquals(2, registry.getDuckLakeColumns("instant_reward").size());
    }

    private void writeParquetMarker(String table) throws Exception {
        Path partition = Files.createDirectories(exportDirectory.resolve(table).resolve("date=2026-01-01"));
        Files.write(partition.resolve("data.parquet"), new byte[]{1});
    }
}
