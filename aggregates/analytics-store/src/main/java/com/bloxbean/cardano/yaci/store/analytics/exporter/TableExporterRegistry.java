package com.bloxbean.cardano.yaci.store.analytics.exporter;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for all table exporters with Spring auto-discovery.
 *
 * This service automatically discovers all {@link TableExporter} beans
 * and provides methods to query and access them by name or partition strategy.
 *
 * Auto-discovery Flow:
 * 1. Spring creates all @Service beans that implement TableExporter
 * 2. Spring injects them into registerExporters() method
 * 3. Registry validates uniqueness and stores them in a map
 *
 * Example usage:
 * <pre>
 * {@code
 * // Get all daily tables
 * List<String> dailyTables = registry.getTablesByStrategy(PartitionStrategy.DAILY);
 *
 * // Get exporter for specific table
 * TableExporter exporter = registry.getExporter("transactions");
 * exporter.exportForPartition(PartitionValue.ofDate(LocalDate.now()));
 *
 * // Check if table is enabled
 * if (registry.isEnabled("transactions")) {
 *     // Export...
 * }
 * }
 * </pre>
 */
@Service
@Slf4j
public class TableExporterRegistry {

    private final Map<String, TableExporter> exporters = new ConcurrentHashMap<>();
    private final AnalyticsStoreProperties properties;

    public TableExporterRegistry(AnalyticsStoreProperties properties) {
        this.properties = properties;
    }

    /**
     * Register all table exporters found by Spring auto-discovery.
     *
     * This method is called automatically by Spring after all TableExporter beans
     * are created. It validates that table names are unique and logs all registered exporters.
     *
     * @param exporterList List of all TableExporter beans discovered by Spring
     * @throws IllegalStateException if duplicate table names are found
     */
    @Autowired
    public void registerExporters(List<TableExporter> exporterList) {
        log.info("Starting table exporter registration...");

        for (TableExporter exporter : exporterList) {
            String tableName = exporter.getTableName();

            if (exporters.containsKey(tableName)) {
                throw new IllegalStateException(
                        "Duplicate table exporter for: " + tableName +
                        ". Each table must have exactly one exporter.");
            }

            exporters.put(tableName, exporter);
            log.info("Registered table exporter: {} ({})",
                    tableName, exporter.getPartitionStrategy());
        }

        log.info("Successfully registered {} table exporters: {}",
                exporters.size(),
                exporters.keySet().stream().sorted().collect(Collectors.joining(", ")));
    }

    /**
     * Get exporter for a specific table.
     *
     * @param tableName The table name
     * @return TableExporter instance
     * @throws IllegalArgumentException if no exporter exists for the table
     */
    public TableExporter getExporter(String tableName) {
        TableExporter exporter = exporters.get(tableName);
        if (exporter == null) {
            throw new IllegalArgumentException(
                    "No exporter found for table: " + tableName +
                    ". Available tables: " + String.join(", ", exporters.keySet()));
        }
        return exporter;
    }

    /**
     * Get all registered table names.
     *
     * @return List of table names (unsorted)
     */
    public List<String> getAllTables() {
        return new ArrayList<>(exporters.keySet());
    }

    /**
     * Get all tables using a specific partition strategy.
     *
     * @param strategy Partition strategy (DAILY, EPOCH, MONTHLY)
     * @return Sorted list of table names using this strategy
     */
    public List<String> getTablesByStrategy(PartitionStrategy strategy) {
        return exporters.values().stream()
                .filter(e -> e.getPartitionStrategy() == strategy)
                .map(TableExporter::getTableName)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Check if a table is enabled in configuration.
     *
     * Tables are enabled if:
     * - yaci.store.analytics.enabled-tables is empty (all enabled), OR
     * - yaci.store.analytics.enabled-tables contains the table name
     *
     * @param tableName The table name to check
     * @return true if table is enabled
     */
    public boolean isEnabled(String tableName) {
        Set<String> enabledTables = properties.getEnabledTables();
        // Empty set means all tables are enabled
        return enabledTables.isEmpty() || enabledTables.contains(tableName);
    }

    /**
     * Get all enabled table names.
     *
     * @return List of enabled table names
     */
    public List<String> getEnabledTables() {
        return getAllTables().stream()
                .filter(this::isEnabled)
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Get enabled tables for a specific partition strategy.
     *
     * @param strategy Partition strategy
     * @return List of enabled table names using this strategy
     */
    public List<String> getEnabledTablesByStrategy(PartitionStrategy strategy) {
        return getTablesByStrategy(strategy).stream()
                .filter(this::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * Check if a table exporter exists.
     *
     * @param tableName The table name
     * @return true if exporter exists
     */
    public boolean hasExporter(String tableName) {
        return exporters.containsKey(tableName);
    }

    /**
     * Get total number of registered exporters.
     *
     * @return Count of registered exporters
     */
    public int getExporterCount() {
        return exporters.size();
    }
}
