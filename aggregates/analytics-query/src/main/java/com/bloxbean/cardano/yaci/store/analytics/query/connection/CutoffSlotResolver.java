package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.query.model.TableMetadata;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves the boundary slot between historical Parquet data and live PostgreSQL data.
 *
 * <p>For each table, computes the maximum slot covered by completed Parquet exports.
 * Data at or below this slot is served from Parquet; data above it from PostgreSQL.</p>
 *
 * <p>The boundary is derived from {@link ExportStateService#getCompletedPartitions(String)},
 * which tracks which date/epoch partitions have been successfully exported to Parquet.</p>
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.query", name = "enabled", havingValue = "true")
public class CutoffSlotResolver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ExportStateService exportStateService;
    private final EraService eraService;

    private final Map<String, Long> cutoffCache = new ConcurrentHashMap<>();

    public CutoffSlotResolver(ExportStateService exportStateService, EraService eraService) {
        this.exportStateService = exportStateService;
        this.eraService = eraService;
    }

    /**
     * Get the cutoff slot for a table. Data at slot &lt;= cutoff is in Parquet.
     *
     * @param tableName the analytics table name
     * @return cutoff slot, or -1 if no exports exist (all data from PostgreSQL)
     */
    public long getCutoffSlot(String tableName) {
        return cutoffCache.computeIfAbsent(tableName, this::computeCutoffSlot);
    }

    /**
     * Refresh all cached cutoff slots. Call after new Parquet exports complete.
     */
    public void refresh() {
        Set<String> tables = Set.copyOf(cutoffCache.keySet());
        for (String table : tables) {
            long newCutoff = computeCutoffSlot(table);
            long oldCutoff = cutoffCache.getOrDefault(table, -1L);
            cutoffCache.put(table, newCutoff);
            if (newCutoff != oldCutoff) {
                log.info("Cutoff slot for '{}' advanced: {} -> {}", table, oldCutoff, newCutoff);
            }
        }
    }

    /**
     * Clear the cache (forces recomputation on next access).
     */
    public void invalidate() {
        cutoffCache.clear();
    }

    private long computeCutoffSlot(String tableName) {
        try {
            Set<String> completed = exportStateService.getCompletedPartitions(tableName);
            if (completed.isEmpty()) {
                log.debug("No completed exports for '{}', cutoff = -1 (all from PostgreSQL)", tableName);
                return -1;
            }

            TableMetadata meta = TableMetadata.forTable(tableName);
            String partitionStrategy = (meta != null) ? meta.partitionStrategy() : "DAILY";

            return switch (partitionStrategy) {
                case "DAILY" -> computeDailyCutoff(tableName, completed);
                case "EPOCH" -> computeEpochCutoff(tableName, completed);
                default -> {
                    log.debug("Table '{}' has strategy '{}', not suitable for cutoff", tableName, partitionStrategy);
                    yield Long.MAX_VALUE; // All data from Parquet
                }
            };
        } catch (Exception e) {
            log.warn("Failed to compute cutoff for '{}': {}", tableName, e.getMessage());
            return -1;
        }
    }

    private long computeDailyCutoff(String tableName, Set<String> completed) {
        // Partition values look like "date=2024-01-15"
        OptionalLong maxSlot = completed.stream()
                .filter(p -> p.startsWith("date="))
                .map(p -> p.substring("date=".length()))
                .map(dateStr -> {
                    try {
                        return LocalDate.parse(dateStr, DATE_FORMATTER);
                    } catch (Exception e) {
                        log.warn("Invalid date partition '{}' for table '{}'", dateStr, tableName);
                        return null;
                    }
                })
                .filter(d -> d != null)
                .map(date -> new PartitionValue.DatePartition(date).toSlotRange(eraService))
                .mapToLong(range -> range.endSlot() - 1) // endSlot is exclusive, so cutoff = endSlot - 1
                .max();

        long cutoff = maxSlot.orElse(-1);
        if (cutoff > 0) {
            log.debug("Daily cutoff for '{}': slot {}", tableName, cutoff);
        }
        return cutoff;
    }

    private long computeEpochCutoff(String tableName, Set<String> completed) {
        // Partition values look like "epoch=450"
        OptionalInt maxEpoch = completed.stream()
                .filter(p -> p.startsWith("epoch="))
                .map(p -> p.substring("epoch=".length()))
                .mapToInt(epochStr -> {
                    try {
                        return Integer.parseInt(epochStr);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid epoch partition '{}' for table '{}'", epochStr, tableName);
                        return -1;
                    }
                })
                .filter(e -> e >= 0)
                .max();

        if (maxEpoch.isEmpty()) {
            return -1;
        }

        try {
            long cutoff = new PartitionValue.EpochPartition(maxEpoch.getAsInt())
                    .toSlotRange(eraService).endSlot() - 1;
            log.debug("Epoch cutoff for '{}': epoch {} -> slot {}", tableName, maxEpoch.getAsInt(), cutoff);
            return cutoff;
        } catch (Exception e) {
            log.warn("Failed to convert epoch {} to slot for '{}': {}",
                    maxEpoch.getAsInt(), tableName, e.getMessage());
            return -1;
        }
    }
}
