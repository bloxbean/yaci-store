package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Registry of the analytics tables that can currently be queried, and where their data lives.
 *
 * <ul>
 *   <li><b>Direct Parquet storage</b> ({@code storage.type=parquet}): scans
 *       {@code {export-path}/} for table directories and maps each table to a
 *       {@code read_parquet()} glob.</li>
 *   <li><b>DuckLake storage</b> ({@code storage.type=ducklake}): the table list and the exact
 *       set of committed Parquet data files come from the DuckLake catalog snapshot supplied
 *       by {@link ParquetReadConnectionProvider}; the export directory is never scanned, so
 *       in-flight or aborted export files are invisible.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class ParquetTableRegistry {

    private final AnalyticsStoreProperties properties;

    @Getter
    private volatile List<String> tableNames = Collections.emptyList();

    /** DuckLake only: table name → absolute paths of the committed data files. */
    private volatile Map<String, List<Path>> duckLakeFiles = Collections.emptyMap();

    @PostConstruct
    void init() {
        if (!isDuckLake()) {
            refresh();
        }
    }

    /** Re-scan direct-Parquet storage so tables created after startup become visible. */
    public synchronized void refresh() {
        if (isDuckLake()) return;

        Path exportDir = Paths.get(properties.getExportPath());
        if (!Files.isDirectory(exportDir)) {
            tableNames = Collections.emptyList();
            log.warn("Analytics export path not found: {}. No Parquet tables available.", exportDir);
            return;
        }

        List<String> discovered = new ArrayList<>();
        try (var dirs = Files.list(exportDir)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                String tableName = dir.getFileName().toString();
                if (hasParquetData(dir)) {
                    discovered.add(tableName);
                } else {
                    log.debug("Skipping empty table directory: {}", tableName);
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan analytics export path '{}' ({})",
                    exportDir, e.getClass().getSimpleName());
        }

        replaceTableNames(discovered);
        log.info("Discovered {} Parquet tables: {}", tableNames.size(), tableNames);
    }

    /** Replace discovery results with the transactionally visible DuckLake catalog tables. */
    public synchronized void replaceTableNames(Collection<String> tables) {
        List<String> discovered = new ArrayList<>(tables);
        Collections.sort(discovered);
        this.tableNames = List.copyOf(discovered);
    }

    /**
     * Replace the DuckLake snapshot: the committed data files per table. Tables without any
     * committed data file are not registered (there is nothing to build a view over yet).
     */
    public synchronized void replaceDuckLakeSnapshot(Map<String, ? extends Collection<Path>> filesByTable) {
        Map<String, List<Path>> snapshot = new TreeMap<>();
        for (Map.Entry<String, ? extends Collection<Path>> entry : filesByTable.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                log.debug("DuckLake table '{}' has no committed data files yet; skipping", entry.getKey());
                continue;
            }
            snapshot.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.duckLakeFiles = Collections.unmodifiableMap(snapshot);
        replaceTableNames(snapshot.keySet());
    }

    /**
     * Get the committed DuckLake data files for a table (absolute paths), or an empty list if
     * the table is unknown.
     */
    public List<Path> getDuckLakeFiles(String tableName) {
        if (!isDuckLake()) {
            throw new IllegalStateException("Only DuckLake storage tracks per-table data files");
        }
        return duckLakeFiles.getOrDefault(tableName, List.of());
    }

    /**
     * Get the read_parquet glob path for a specific table.
     */
    public String getParquetGlobPath(String tableName) {
        if (isDuckLake()) {
            throw new IllegalStateException("DuckLake tables must be read through the catalog snapshot");
        }
        return Paths.get(properties.getExportPath(), tableName, "**", "*.parquet")
                .toAbsolutePath()
                .normalize()
                .toString();
    }

    private boolean hasParquetData(Path tableDir) {
        try (var files = Files.find(tableDir, Integer.MAX_VALUE,
                (path, attrs) -> attrs.isRegularFile()
                        && path.getFileName().toString().endsWith(".parquet"))) {
            return files.findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean isDuckLake() {
        return "ducklake".equalsIgnoreCase(properties.getStorage().getType());
    }
}
