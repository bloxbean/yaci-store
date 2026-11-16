package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.gap.GapDetectionService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

/**
 * The one readiness check for every export, whoever asks for it: the continuous-sync scheduler,
 * the admin export endpoints and the partition repair all go through
 * {@link UniversalExportService} or call this service before exporting.
 *
 * <p>A partition that is exported before its data is final is stored as COMPLETED with partial
 * data, and a COMPLETED partition is never exported again. So:</p>
 * <ul>
 *   <li><b>Daily partitions</b> are ready when the day is on or before
 *   {@link GapDetectionService#getExportEndDate()}: the finalized tip (latest block time minus the
 *   finality window) minus {@code continuous-sync.buffer-days}. This is the bound the scheduler
 *   has always used.</li>
 *   <li><b>Epoch partitions</b> are never ready for an epoch that has not started. Tables that
 *   {@linkplain TableExporter#requiresFinalizedEpoch() require a finalized epoch} are ready once the
 *   epoch's last slot is older than the finalized tip. Every table must also pass its exporter's
 *   own check ({@link TableExporter#preExportValidation}), such as the AdaPot job.</li>
 *   <li>An exporter that is {@linkplain TableExporter#unavailableReason() unavailable} is never ready.</li>
 * </ul>
 * Errors in a check mean "not ready, try later", never "export anyway".
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class ExportReadinessService {

    private final GapDetectionService gapDetectionService;
    private final EraService eraService;
    private final AnalyticsStoreProperties properties;

    /**
     * Full check of one table's partition: the exporter, the partition's finality and the
     * exporter's own prerequisites.
     *
     * @throws IllegalArgumentException if the partition type does not match the table's strategy,
     *                                  or the epoch is negative
     */
    public PartitionReadiness check(TableExporter exporter, PartitionValue partition) {
        validatePartitionType(exporter, partition);
        Optional<String> unavailable = exporter.unavailableReason();
        if (unavailable.isPresent()) {
            return unavailable(exporter, unavailable.get());
        }
        PartitionReadiness finality = checkFinal(partition);
        if (!finality.isReady()) {
            return finality;
        }
        return checkExporter(exporter, partition);
    }

    /**
     * Finality of the partition alone, independent of any table: a day must be on or before the
     * export end date, an epoch must have started.
     */
    public PartitionReadiness checkFinal(PartitionValue partition) {
        try {
            if (partition instanceof PartitionValue.DatePartition day) {
                LocalDate exportEndDate = gapDetectionService.getExportEndDate();
                if (day.date().isAfter(exportEndDate)) {
                    return PartitionReadiness.notFinal(String.format(
                            "%s is not final yet: the latest day that can be exported is %s (the finalized tip "
                                    + "minus buffer-days=%d)", partition.toPathSegment(), exportEndDate,
                            properties.getContinuousSync().getBufferDays()));
                }
                return PartitionReadiness.READY;
            }
            int epoch = ((PartitionValue.EpochPartition) partition).epoch();
            if (epoch < 0) {
                throw new IllegalArgumentException("Epoch must not be negative: " + epoch);
            }
            Optional<Integer> currentEpoch = gapDetectionService.getCurrentEpoch();
            if (currentEpoch.isEmpty()) {
                return PartitionReadiness.notFinal("No blocks are synced yet");
            }
            if (epoch > currentEpoch.get()) {
                return PartitionReadiness.notFinal(String.format("%s has not started yet (the current epoch is %d)",
                        partition.toPathSegment(), currentEpoch.get()));
            }
            return PartitionReadiness.READY;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not check whether {} is final, treating it as not final: {}",
                    partition.toPathSegment(), e.getMessage());
            return PartitionReadiness.notFinal("Could not check whether " + partition.toPathSegment()
                    + " is final: " + e.getMessage());
        }
    }

    /**
     * The table-specific part of {@link #check}, for callers that already checked
     * {@link #checkFinal} for the partition (one partition, many tables).
     */
    public PartitionReadiness checkExporter(TableExporter exporter, PartitionValue partition) {
        Optional<String> unavailable = exporter.unavailableReason();
        if (unavailable.isPresent()) {
            return unavailable(exporter, unavailable.get());
        }
        try {
            if (partition instanceof PartitionValue.EpochPartition epoch && exporter.requiresFinalizedEpoch()
                    && !isEpochFinalized(epoch)) {
                return PartitionReadiness.notFinal(String.format(
                        "%s of %s is not final yet: the epoch has not ended, or its last slot is within the "
                                + "finality window of the tip", partition.toPathSegment(), exporter.getTableName()));
            }
            if (!exporter.preExportValidation(partition)) {
                return PartitionReadiness.dependencyPending(String.format(
                        "%s of %s is not ready yet: the data it is exported from is not complete (for example its "
                                + "AdaPot job has not finished)", partition.toPathSegment(), exporter.getTableName()));
            }
            return PartitionReadiness.READY;
        } catch (Exception e) {
            log.warn("Readiness check of {} {} failed, treating it as not ready: {}",
                    exporter.getTableName(), partition.toPathSegment(), e.getMessage());
            return PartitionReadiness.dependencyPending(String.format("Could not check whether %s of %s is ready: %s",
                    partition.toPathSegment(), exporter.getTableName(), e.getMessage()));
        }
    }

    /** The latest day that can be exported (finalized tip minus buffer-days). */
    public LocalDate getExportEndDate() {
        return gapDetectionService.getExportEndDate();
    }

    /** The UTC day of the network's genesis; no day before it holds data. */
    public LocalDate getGenesisDate() {
        return gapDetectionService.getGenesisDate();
    }

    /** The epoch of the latest synced block, if any. */
    public Optional<Integer> getCurrentEpoch() {
        return gapDetectionService.getCurrentEpoch();
    }

    private boolean isEpochFinalized(PartitionValue.EpochPartition epoch) {
        Optional<Instant> latest = gapDetectionService.getLatestSyncedBlockTime();
        if (latest.isEmpty()) {
            return false;
        }
        Instant finalizedTip = latest.get().minus(gapDetectionService.getFinalityWindow());
        long firstNonFinalSlot = eraService.slotAtOrAfterTime(finalizedTip.getEpochSecond());
        // The epoch's slots are [start, end); all of them must start before the finalized tip
        return epoch.toSlotRange(eraService).endSlot() <= firstNonFinalSlot;
    }

    private static void validatePartitionType(TableExporter exporter, PartitionValue partition) {
        PartitionStrategy expected = partition instanceof PartitionValue.DatePartition
                ? PartitionStrategy.DAILY : PartitionStrategy.EPOCH;
        if (exporter.getPartitionStrategy() != expected) {
            throw new IllegalArgumentException(String.format("Table %s is partitioned %s, not %s (%s)",
                    exporter.getTableName(), exporter.getPartitionStrategy(), expected, partition.toPathSegment()));
        }
    }

    private static PartitionReadiness unavailable(TableExporter exporter, String reason) {
        return PartitionReadiness.unavailable("Table " + exporter.getTableName() + " cannot be exported: " + reason);
    }
}
