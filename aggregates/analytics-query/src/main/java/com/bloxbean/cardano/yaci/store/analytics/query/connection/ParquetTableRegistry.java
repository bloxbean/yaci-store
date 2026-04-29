package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Discovers available Parquet tables from the analytics export path.
 *
 * <p>Scans {@code {export-path}/main/} for subdirectories, each representing
 * a table exported by analytics-store. Maps table names to their Parquet glob paths
 * for use in DuckDB {@code read_parquet()} calls.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class ParquetTableRegistry {

    private final AnalyticsStoreProperties properties;

    @Getter
    private List<String> tableNames = Collections.emptyList();

    @PostConstruct
    void init() {
        Path mainDir = Paths.get(properties.getExportPath(), "main");
        if (!Files.isDirectory(mainDir)) {
            log.warn("Analytics export path not found: {}. No Parquet tables available.", mainDir);
            return;
        }

        List<String> discovered = new ArrayList<>();
        try (DirectoryStream<Path> dirs = Files.newDirectoryStream(mainDir, Files::isDirectory)) {
            for (Path dir : dirs) {
                String tableName = dir.getFileName().toString();
                // Check that it has at least one partition or parquet file
                if (hasParquetData(dir)) {
                    discovered.add(tableName);
                } else {
                    log.debug("Skipping empty table directory: {}", tableName);
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan analytics export path: {}", mainDir, e);
        }

        Collections.sort(discovered);
        this.tableNames = Collections.unmodifiableList(discovered);
        log.info("Discovered {} Parquet tables: {}", tableNames.size(), tableNames);
    }

    /**
     * Get the read_parquet glob path for a specific table.
     */
    public String getParquetGlobPath(String tableName) {
        return Paths.get(properties.getExportPath(), "main", tableName, "**", "*.parquet")
                .toString();
    }

    private boolean hasParquetData(Path tableDir) {
        try (DirectoryStream<Path> children = Files.newDirectoryStream(tableDir)) {
            return children.iterator().hasNext();
        } catch (IOException e) {
            return false;
        }
    }
}
