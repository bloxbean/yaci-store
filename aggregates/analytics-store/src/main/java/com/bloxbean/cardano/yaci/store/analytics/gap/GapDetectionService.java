package com.bloxbean.cardano.yaci.store.analytics.gap;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.scheduler.ExportReadinessService;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportState;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.core.model.Era;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service for detecting gaps in export data.
 *
 * Identifies missing export dates between genesis and current blockchain sync point,
 * accounting for a configurable buffer period to avoid exporting incomplete data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class GapDetectionService {

    private final GenesisConfig genesisConfig;
    private final BlockStorageReader blockStorageReader;
    private final ExportStateService stateService;
    private final AnalyticsStoreProperties properties;
    private final EraService eraService;

    private Clock clock = Clock.systemDefaultZone();

    /**
     * The readiness check shared with the admin API. It depends on this service, so it is looked up
     * lazily; without it (unit tests) the exporter's own check is used.
     */
    private ObjectProvider<ExportReadinessService> readinessService;

    @Autowired(required = false)
    void setReadinessService(ObjectProvider<ExportReadinessService> readinessService) {
        this.readinessService = readinessService;
    }

    @PostConstruct
    void validateAndLogFinalitySettings() {
        int bufferDays = properties.getContinuousSync().getBufferDays();
        if (bufferDays < 1) {
            log.warn("yaci.store.analytics.continuous-sync.buffer-days={} is below the minimum of 1 (a day partition "
                    + "can only be exported once the day is complete) — using 1", bufferDays);
            properties.getContinuousSync().setBufferDays(1);
            bufferDays = 1;
        }
        int margin = properties.getContinuousSync().getFinalityMarginHours();
        if (margin < 0) {
            log.warn("yaci.store.analytics.continuous-sync.finality-margin-hours={} is negative (it would move the "
                    + "finalized tip into the future) — using 0", margin);
            properties.getContinuousSync().setFinalityMarginHours(0);
            margin = 0;
        }
        Duration derived = derivedFinalityWindow();
        if (derived.isZero()) {
            log.warn("Cannot derive the finality window from genesis (securityParam={}, activeSlotsCoeff={}, "
                    + "slotLength={}) and finality-window-hours is not set; only finality-margin-hours ({}h) and "
                    + "buffer-days ({}) keep exports behind the tip",
                    genesisConfig.getSecurityParam(), genesisConfig.getActiveSlotsCoeff(),
                    genesisConfig.slotDuration(Era.Shelley), margin, bufferDays);
        }
        int override = properties.getContinuousSync().getFinalityWindowHours();
        String source = override > 0
                ? "overridden by finality-window-hours=" + override
                : String.format("k=%d, activeSlotsCoeff=%s, slotLength=%ss", genesisConfig.getSecurityParam(),
                        genesisConfig.getActiveSlotsCoeff(), genesisConfig.slotDuration(Era.Shelley));
        log.info("Analytics export finality: window {}h ({}) + margin {}h, buffer-days={}",
                derived.toMinutes() / 60.0, source, margin, bufferDays);
    }

    /**
     * Find all missing export dates for a table.
     *
     * Checks from genesis to (latestSyncedDate - bufferDays).
     *
     * @param tableName The table to check for missing exports
     * @return List of dates that are missing exports, sorted oldest to newest
     */
    public List<LocalDate> findMissingExports(String tableName) {
        LocalDate startDate = getGenesisDate();
        LocalDate endDate = getExportEndDate();

        if (endDate.isBefore(startDate)) {
            log.debug("No exports needed yet - sync has not progressed past buffer period");
            return List.of();
        }

        // Get completed partitions from state table
        Set<String> completedPartitions = stateService.getCompletedPartitions(tableName);

        // Find gaps
        List<LocalDate> missing = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            String partitionValue = "date=" + current; // Hive-style format matching PartitionValue.DatePartition.toPathSegment()
            if (!completedPartitions.contains(partitionValue)) {
                missing.add(current);
            }
            current = current.plusDays(1);
        }

        log.debug("Gap detection for {}: {} missing exports from {} to {}",
            tableName, missing.size(), startDate, endDate);

        return missing;
    }

    /**
     * Get genesis date from network configuration.
     *
     * Converts the genesis start time (epoch seconds) to a LocalDate in UTC.
     *
     * @return Genesis date as LocalDate
     */
    public LocalDate getGenesisDate() {
        long startTimeEpochSeconds = genesisConfig.getStartTime();
        return Instant.ofEpochSecond(startTimeEpochSeconds)
            .atZone(ZoneOffset.UTC)
            .toLocalDate();
    }

    /**
     * Get the latest synced date from blockchain data.
     *
     * Reads the most recent block and extracts its blockTime.
     * If no blocks exist yet, returns the genesis date.
     *
     * @return Latest synced date as LocalDate
     */
    public LocalDate getLatestSyncedDate() {
        return getLatestSyncedBlockTime()
            .map(t -> t.atZone(ZoneOffset.UTC).toLocalDate())
            .orElseGet(() -> {
                log.warn("No blocks synced yet, using genesis date");
                return getGenesisDate();
            });
    }

    /**
     * Block time of the most recent synced block, if any.
     */
    public Optional<Instant> getLatestSyncedBlockTime() {
        return blockStorageReader.findRecentBlock()
            .map(Block::getBlockTime)
            .map(Instant::ofEpochSecond);
    }

    /**
     * Finality window applied before a day is considered exportable: the time Cardano needs
     * to produce {@code securityParam} (k) blocks — {@code k × slotLength / activeSlotsCoeff},
     * 12 h on mainnet and preprod — plus {@code continuous-sync.finality-margin-hours}.
     * {@code continuous-sync.finality-window-hours} replaces the derived part when set.
     * Cardano's rollback bound is k <em>blocks</em>; this window is the expected time for them,
 * so rows older than it are beyond the bound with overwhelming probability.
     *
     * @return finality window including the margin
     */
    public Duration getFinalityWindow() {
        int margin = Math.max(0, properties.getContinuousSync().getFinalityMarginHours());
        return derivedFinalityWindow().plusHours(margin);
    }

    private Duration derivedFinalityWindow() {
        int override = properties.getContinuousSync().getFinalityWindowHours();
        if (override > 0) {
            return Duration.ofHours(override);
        }
        int k = genesisConfig.getSecurityParam();
        double f = genesisConfig.getActiveSlotsCoeff();
        double slotLength = genesisConfig.slotDuration(Era.Shelley);
        if (k <= 0 || f <= 0 || slotLength <= 0) {
            // Reported once at startup (validateAndLogFinalitySettings); this method runs per gap check
            log.debug("Cannot derive the finality window from genesis (securityParam={}, activeSlotsCoeff={}, "
                    + "slotLength={}); only continuous-sync.finality-margin-hours and buffer-days apply",
                    k, f, slotLength);
            return Duration.ZERO;
        }
        return Duration.ofSeconds(Math.round(k * slotLength / f));
    }

    /**
     * Calculate the safe end date for exports.
     *
     * The latest synced block time is first moved back by the {@linkplain #getFinalityWindow()
     * finality window} ("finalized tip"), then {@code buffer-days} UTC days are subtracted. A day
     * partition is therefore exported only when all of its rows are older than the finality
     * window (with buffer-days=1: exported ~13 h after midnight UTC on mainnet). During the
     * initial sync the same rule keeps exports safely behind the sync point.
     *
     * If the buffer pushes the end date before genesis, return it as-is.
     *
     * @return Safe end date for exports as LocalDate
     */
    public LocalDate getExportEndDate() {
        int bufferDays = properties.getContinuousSync().getBufferDays();
        Instant latest = getLatestSyncedBlockTime().orElse(null);
        if (latest == null) {
            return getGenesisDate().minusDays(bufferDays);
        }
        Instant finalizedTip = latest.minus(getFinalityWindow());
        return finalizedTip.atZone(ZoneOffset.UTC).toLocalDate().minusDays(bufferDays);
    }

    /**
     * Find all missing epoch exports for a table.
     *
     * Checks from the first non-Byron epoch (Shelley start) to the current epoch (inclusive).
     * Not every missing epoch is ready to export; {@link #findGaps(TableExporter)} applies each
     * exporter's readiness check.
     * Byron epochs are skipped unless the table includes them.
     *
     * @param tableName The table to check for missing epoch exports
     * @return List of epoch numbers that are missing exports, sorted oldest to newest
     */
    public List<Integer> findMissingEpochExports(String tableName) {
        return findMissingEpochExports(tableName, false);
    }

    /**
     * @param includeByronEpochs start at epoch 0 for tables that hold rows for Byron epochs
     * @see com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter#includesByronEpochs()
     */
    public List<Integer> findMissingEpochExports(String tableName, boolean includeByronEpochs) {
        Optional<Integer> startEpochOpt = eraService.getFirstNonByronEpoch();
        if (startEpochOpt.isEmpty()) {
            log.debug("No non-Byron era found yet, skipping epoch gap detection for {}", tableName);
            return List.of();
        }

        int startEpoch = includeByronEpochs ? 0 : startEpochOpt.get();
        Optional<Integer> endEpochOpt = getCurrentEpoch();
        if (endEpochOpt.isEmpty()) {
            log.debug("No epoch available yet, skipping epoch gap detection for {}", tableName);
            return List.of();
        }

        int endEpoch = endEpochOpt.get();
        if (endEpoch < startEpoch) {
            log.debug("End epoch {} is before start epoch {}, no exports needed for {}", endEpoch, startEpoch, tableName);
            return List.of();
        }

        Set<String> completedPartitions = stateService.getCompletedPartitions(tableName);

        List<Integer> missing = new ArrayList<>();
        for (int epoch = startEpoch; epoch <= endEpoch; epoch++) {
            String partitionValue = "epoch=" + epoch;
            if (!completedPartitions.contains(partitionValue)) {
                missing.add(epoch);
            }
        }

        log.debug("Epoch gap detection for {}: {} missing exports from epoch {} to {}",
            tableName, missing.size(), startEpoch, endEpoch);

        return missing;
    }

    /**
     * Missing partitions of one table, split by what the scheduler can do with them.
     *
     * @param tableName  the table
     * @param due        ready and not waiting for a retry: export these now
     * @param backingOff exported before and failed; the next retry is not due yet
     * @param notReady   not exportable yet (for example waiting for an AdaPot job), or never
     *                   (AdaPot disabled); these are not gaps
     */
    public record TableGaps(String tableName, List<PartitionValue> due, List<PartitionValue> backingOff,
                            List<PartitionValue> notReady) {
        /** Partitions that could be exported but are not: {@code due} plus {@code backingOff}. */
        public int readyMissing() {
            return due.size() + backingOff.size();
        }
    }

    /**
     * Missing partitions of an exporter's table, split into due, backing off and not ready.
     *
     * <p>Daily partitions up to {@link #getExportEndDate()} and epoch partitions up to the current
     * epoch are candidates; each is then checked with {@link #isReady}. A partition whose last
     * export FAILED is retried only after {@link #retryDelay(int)} since that failure.</p>
     */
    public TableGaps findGaps(TableExporter exporter) {
        String tableName = exporter.getTableName();
        List<PartitionValue> missing = switch (exporter.getPartitionStrategy()) {
            case DAILY -> findMissingExports(tableName).stream().map(PartitionValue::ofDate).toList();
            case EPOCH -> findMissingEpochExports(tableName, exporter.includesByronEpochs()).stream()
                    .map(PartitionValue::ofEpoch).toList();
            default -> List.of();
        };
        if (missing.isEmpty()) {
            return new TableGaps(tableName, List.of(), List.of(), List.of());
        }

        Map<String, ExportState> failed = new HashMap<>();
        for (ExportState state : stateService.getFailedExports(tableName, Integer.MAX_VALUE)) {
            failed.put(state.getId().getPartitionValue(), state);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<PartitionValue> due = new ArrayList<>();
        List<PartitionValue> backingOff = new ArrayList<>();
        List<PartitionValue> notReady = new ArrayList<>();
        for (PartitionValue partition : missing) {
            ExportState lastFailure = failed.get(partition.toPathSegment());
            if (lastFailure != null && !isRetryDue(lastFailure, now)) {
                // It failed after passing the readiness check, so it was ready
                backingOff.add(partition);
            } else if (isReady(exporter, partition)) {
                due.add(partition);
            } else {
                notReady.add(partition);
            }
        }
        return new TableGaps(tableName, due, backingOff, notReady);
    }

    /**
     * Whether the partition can be exported now: {@link ExportReadinessService#checkExporter}, the
     * check the admin API and the export itself use (an unavailable table, an epoch that is not
     * finalized, the exporter's own {@link TableExporter#preExportValidation}). An exception means
     * "not ready".
     */
    boolean isReady(TableExporter exporter, PartitionValue partition) {
        ExportReadinessService readiness = readinessService == null ? null : readinessService.getIfAvailable();
        if (readiness != null) {
            // Same rules as the admin API: unavailable tables, finalized epochs, the exporter's own check
            return readiness.checkExporter(exporter, partition).isReady();
        }
        try {
            return exporter.preExportValidation(partition);
        } catch (RuntimeException e) {
            log.debug("Readiness check of {} {} failed ({}); treating it as not ready",
                    exporter.getTableName(), partition.toPathSegment(), e.getMessage());
            return false;
        }
    }

    /**
     * Wait before retrying a partition whose export failed: {@code retry-backoff-initial-minutes}
     * doubled for every earlier retry, at most {@code retry-backoff-max-minutes}.
     *
     * @param retryCount retries already made (the state's {@code retry_count})
     */
    public Duration retryDelay(int retryCount) {
        long initial = Math.max(1, properties.getContinuousSync().getRetryBackoffInitialMinutes());
        long max = Math.max(initial, properties.getContinuousSync().getRetryBackoffMaxMinutes());
        int doublings = Math.min(Math.max(0, retryCount), 40);
        long minutes = initial;
        for (int i = 0; i < doublings && minutes < max; i++) {
            minutes *= 2;
        }
        return Duration.ofMinutes(Math.min(minutes, max));
    }

    private boolean isRetryDue(ExportState failed, LocalDateTime now) {
        if (failed.getCompletedAt() == null) {
            return true;
        }
        int retryCount = failed.getRetryCount() != null ? failed.getRetryCount() : 0;
        return !now.isBefore(failed.getCompletedAt().plus(retryDelay(retryCount)));
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Get the current epoch number from the latest synced block.
     *
     * @return Optional containing the current epoch, or empty if no blocks exist
     */
    public Optional<Integer> getCurrentEpoch() {
        Block recentBlock = blockStorageReader.findRecentBlock().orElse(null);

        if (recentBlock == null) {
            log.warn("No blocks synced yet, cannot determine epoch scan upper bound");
            return Optional.empty();
        }

        return Optional.of(recentBlock.getEpochNumber());
    }
}
