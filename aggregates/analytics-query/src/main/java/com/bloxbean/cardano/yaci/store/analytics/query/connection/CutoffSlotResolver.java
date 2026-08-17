package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
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
@ConditionalOnExpression("${yaci.store.analytics.enabled:false} && "
        + "${yaci.store.analytics.query.enabled:false} && "
        + "${yaci.store.analytics.query.live-data-enabled:false}")
public class CutoffSlotResolver {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ExportStateService exportStateService;
    private final EraService eraService;
    private final TableExporterRegistry exporterRegistry;

    private final Map<String, Long> cutoffCache = new ConcurrentHashMap<>();

    public CutoffSlotResolver(ExportStateService exportStateService, EraService eraService,
                              TableExporterRegistry exporterRegistry) {
        this.exportStateService = exportStateService;
        this.eraService = eraService;
        this.exporterRegistry = exporterRegistry;
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

            if (!exporterRegistry.hasExporter(tableName)) {
                log.warn("No exporter metadata for '{}'; live federation is disabled for this table", tableName);
                return -1;
            }
            PartitionStrategy partitionStrategy = exporterRegistry.getExporter(tableName).getPartitionStrategy();

            return switch (partitionStrategy) {
                case DAILY -> computeDailyCutoff(tableName, completed);
                case EPOCH -> computeEpochCutoff(tableName, completed);
                default -> {
                    log.debug("Table '{}' has strategy '{}', not suitable for cutoff", tableName, partitionStrategy);
                    yield Long.MAX_VALUE; // All data from Parquet
                }
            };
        } catch (Exception e) {
            log.warn("Failed to compute cutoff for '{}' ({})", tableName, e.getClass().getSimpleName());
            return -1;
        }
    }

    private long computeDailyCutoff(String tableName, Set<String> completed) {
        // Partition values look like "date=2024-01-15"
        NavigableSet<LocalDate> dates = new TreeSet<>(completed.stream()
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
                .toList());
        if (dates.isEmpty()) return -1;

        LocalDate contiguousEnd = dates.first();
        for (LocalDate date : dates.tailSet(contiguousEnd, false)) {
            if (!date.equals(contiguousEnd.plusDays(1))) {
                log.warn("Export gap detected for '{}' after {}; cutoff will not cross the gap",
                        tableName, contiguousEnd);
                break;
            }
            contiguousEnd = date;
        }
        long cutoff = new PartitionValue.DatePartition(contiguousEnd)
                .toSlotRange(eraService).endSlot() - 1;
        if (cutoff > 0) {
            log.debug("Daily cutoff for '{}': slot {}", tableName, cutoff);
        }
        return cutoff;
    }

    private long computeEpochCutoff(String tableName, Set<String> completed) {
        // Partition values look like "epoch=450"
        NavigableSet<Integer> epochs = new TreeSet<>(completed.stream()
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
                .boxed()
                .toList());

        if (epochs.isEmpty()) {
            return -1;
        }

        int contiguousEnd = epochs.first();
        for (int epoch : epochs.tailSet(contiguousEnd, false)) {
            if (epoch != contiguousEnd + 1) {
                log.warn("Export gap detected for '{}' after epoch {}; cutoff will not cross the gap",
                        tableName, contiguousEnd);
                break;
            }
            contiguousEnd = epoch;
        }

        try {
            long cutoff = new PartitionValue.EpochPartition(contiguousEnd)
                    .toSlotRange(eraService).endSlot() - 1;
            log.debug("Epoch cutoff for '{}': epoch {} -> slot {}", tableName, contiguousEnd, cutoff);
            return cutoff;
        } catch (Exception e) {
            log.warn("Failed to convert epoch {} to slot for '{}': {}",
                    contiguousEnd, tableName, e.getClass().getSimpleName());
            return -1;
        }
    }
}
