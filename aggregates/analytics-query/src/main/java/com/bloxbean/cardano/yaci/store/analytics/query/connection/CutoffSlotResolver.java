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
 * <p>For each table, computes the contiguous range covered by completed exports as a
 * {@link Cutoff}. Data inside that range is served from the exported data; data before or
 * after it is served from PostgreSQL.</p>
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

    private final Map<String, Cutoff> cutoffCache = new ConcurrentHashMap<>();

    /**
     * The federation boundary of a table, expressed in both units so the view builder can
     * apply it to a slot column (DAILY tables) or an epoch column (EPOCH tables).
     *
     * @param slot       last slot covered by the exported data; -1 = nothing exported yet
     * @param epoch      last epoch covered by the exported data (EPOCH tables only); -1 = none
     * @param startSlot  first slot covered by the exported data
     * @param startEpoch first epoch covered by the exported data (EPOCH tables only)
     */
    public record Cutoff(long slot, long epoch, long startSlot, long startEpoch) {
        public static final Cutoff NONE = new Cutoff(-1, -1, 0, 0);
        /** Everything is in the exported data (non-partitioned tables). */
        public static final Cutoff ALL_EXPORTED = new Cutoff(
                Long.MAX_VALUE, Long.MAX_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);

        /** Compatibility constructor for a range with no lower bound. */
        public Cutoff(long slot, long epoch) {
            this(slot, epoch, Long.MIN_VALUE, Long.MIN_VALUE);
        }
    }

    public CutoffSlotResolver(ExportStateService exportStateService, EraService eraService,
                              TableExporterRegistry exporterRegistry) {
        this.exportStateService = exportStateService;
        this.eraService = eraService;
        this.exporterRegistry = exporterRegistry;
    }

    /**
     * Get the final slot of the contiguous exported range for a table.
     *
     * @param tableName the analytics table name
     * @return cutoff slot, or -1 if no exports exist (all data from PostgreSQL)
     */
    public long getCutoffSlot(String tableName) {
        return getCutoff(tableName).slot();
    }

    /**
     * Get the contiguous exported range for a table in both slot and epoch units.
     */
    public Cutoff getCutoff(String tableName) {
        return cutoffCache.computeIfAbsent(tableName, this::computeCutoff);
    }

    /**
     * Refresh all cached cutoffs. Call after new Parquet exports complete.
     */
    public void refresh() {
        Set<String> tables = Set.copyOf(cutoffCache.keySet());
        for (String table : tables) {
            Cutoff newCutoff = computeCutoff(table);
            Cutoff oldCutoff = cutoffCache.getOrDefault(table, Cutoff.NONE);
            cutoffCache.put(table, newCutoff);
            if (!newCutoff.equals(oldCutoff)) {
                log.info("Cutoff for '{}' advanced: slot {} -> {} (epoch {} -> {})", table,
                        oldCutoff.slot(), newCutoff.slot(), oldCutoff.epoch(), newCutoff.epoch());
            }
        }
    }

    /**
     * Clear the cache (forces recomputation on next access).
     */
    public void invalidate() {
        cutoffCache.clear();
    }

    private Cutoff computeCutoff(String tableName) {
        try {
            Set<String> completed = exportStateService.getCompletedPartitions(tableName);
            if (completed.isEmpty()) {
                log.debug("No completed exports for '{}', cutoff = -1 (all from PostgreSQL)", tableName);
                return Cutoff.NONE;
            }

            if (!exporterRegistry.hasExporter(tableName)) {
                log.warn("No exporter metadata for '{}'; live federation is disabled for this table", tableName);
                return Cutoff.NONE;
            }
            PartitionStrategy partitionStrategy = exporterRegistry.getExporter(tableName).getPartitionStrategy();

            return switch (partitionStrategy) {
                case DAILY -> computeDailyCutoff(tableName, completed);
                case EPOCH -> computeEpochCutoff(tableName, completed);
                default -> {
                    log.debug("Table '{}' has strategy '{}', not suitable for cutoff", tableName, partitionStrategy);
                    yield Cutoff.ALL_EXPORTED;
                }
            };
        } catch (Exception e) {
            log.warn("Failed to compute cutoff for '{}' ({})", tableName, e.getClass().getSimpleName());
            // During refresh, keep the last known-good range instead of routing the entire
            // table to PostgreSQL because of a transient export-state read failure.
            return cutoffCache.getOrDefault(tableName, Cutoff.NONE);
        }
    }

    private Cutoff computeDailyCutoff(String tableName, Set<String> completed) {
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
        if (dates.isEmpty()) return Cutoff.NONE;

        LocalDate contiguousStart = dates.first();
        LocalDate contiguousEnd = contiguousStart;
        for (LocalDate date : dates.tailSet(contiguousEnd, false)) {
            if (!date.equals(contiguousEnd.plusDays(1))) {
                log.warn("Export gap detected for '{}' after {}; cutoff will not cross the gap",
                        tableName, contiguousEnd);
                break;
            }
            contiguousEnd = date;
        }
        long startSlot = new PartitionValue.DatePartition(contiguousStart)
                .toSlotRange(eraService).startSlot();
        long cutoff = new PartitionValue.DatePartition(contiguousEnd)
                .toSlotRange(eraService).endSlot() - 1;
        if (cutoff > 0) {
            log.debug("Daily exported range for '{}': slots {} through {}", tableName, startSlot, cutoff);
        }
        return new Cutoff(cutoff, -1, startSlot, -1);
    }

    private Cutoff computeEpochCutoff(String tableName, Set<String> completed) {
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
            return Cutoff.NONE;
        }

        int contiguousStart = epochs.first();
        int contiguousEnd = contiguousStart;
        for (int epoch : epochs.tailSet(contiguousEnd, false)) {
            if (epoch != contiguousEnd + 1) {
                log.warn("Export gap detected for '{}' after epoch {}; cutoff will not cross the gap",
                        tableName, contiguousEnd);
                break;
            }
            contiguousEnd = epoch;
        }

        try {
            long startSlot = new PartitionValue.EpochPartition(contiguousStart)
                    .toSlotRange(eraService).startSlot();
            long cutoff = new PartitionValue.EpochPartition(contiguousEnd)
                    .toSlotRange(eraService).endSlot() - 1;
            log.debug("Epoch exported range for '{}': epochs {} through {}", tableName,
                    contiguousStart, contiguousEnd);
            return new Cutoff(cutoff, contiguousEnd, startSlot, contiguousStart);
        } catch (Exception e) {
            log.warn("Failed to convert epoch {} to slot for '{}': {}",
                    contiguousEnd, tableName, e.getClass().getSimpleName());
            return Cutoff.NONE;
        }
    }
}
