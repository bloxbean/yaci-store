package com.bloxbean.cardano.yaci.store.analytics.gap;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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

        log.info("Gap detection for {}: {} missing exports from {} to {}",
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
        Block recentBlock = blockStorageReader.findRecentBlock()
            .orElse(null);

        if (recentBlock == null || recentBlock.getBlockTime() == null) {
            log.warn("No blocks synced yet, using genesis date");
            return getGenesisDate();
        }

        long blockTimeEpochSeconds = recentBlock.getBlockTime();
        return Instant.ofEpochSecond(blockTimeEpochSeconds)
            .atZone(ZoneOffset.UTC)
            .toLocalDate();
    }

    /**
     * Calculate the safe end date for exports (latest sync - buffer).
     *
     * This ensures we don't export data for dates that are still being populated,
     * providing a safety buffer during initial blockchain synchronization.
     *
     * The returned date is clamped to never be before genesis, even if the buffer
     * period extends before the blockchain's start date.
     *
     * @return Safe end date for exports as LocalDate (>= genesis date)
     */
    public LocalDate getExportEndDate() {
        LocalDate latestSynced = getLatestSyncedDate();
        int bufferDays = properties.getContinuousSync().getBufferDays();
        LocalDate calculatedEndDate = latestSynced.minusDays(bufferDays);

        // Ensure export end date is never before genesis
        LocalDate genesisDate = getGenesisDate();
        if (calculatedEndDate.isBefore(genesisDate)) {
            log.debug("Export end date {} is before genesis {}, clamping to genesis",
                calculatedEndDate, genesisDate);
            return genesisDate;
        }

        return calculatedEndDate;
    }

    /**
     * Find all missing epoch exports for a table.
     *
     * Checks from the first non-Byron epoch (Shelley start) to the latest completed epoch.
     * Epoch-based tables only contain Shelley+ data, so Byron epochs are skipped.
     *
     * @param tableName The table to check for missing epoch exports
     * @return List of epoch numbers that are missing exports, sorted oldest to newest
     */
    public List<Integer> findMissingEpochExports(String tableName) {
        Optional<Integer> startEpochOpt = eraService.getFirstNonByronEpoch();
        if (startEpochOpt.isEmpty()) {
            log.debug("No non-Byron era found yet, skipping epoch gap detection for {}", tableName);
            return List.of();
        }

        int startEpoch = startEpochOpt.get();
        Optional<Integer> endEpochOpt = getLatestCompletedEpoch();
        if (endEpochOpt.isEmpty()) {
            log.debug("No completed epoch available yet, skipping epoch gap detection for {}", tableName);
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

        log.info("Epoch gap detection for {}: {} missing exports from epoch {} to {}",
            tableName, missing.size(), startEpoch, endEpoch);

        return missing;
    }

    /**
     * Get the latest fully completed epoch number.
     *
     * Reads the current epoch from the latest block and returns currentEpoch - 1,
     * since the current epoch is still in progress.
     *
     * @return Optional containing the latest completed epoch, or empty if no blocks exist
     */
    public Optional<Integer> getLatestCompletedEpoch() {
        Block recentBlock = blockStorageReader.findRecentBlock().orElse(null);

        if (recentBlock == null) {
            log.warn("No blocks synced yet, cannot determine latest completed epoch");
            return Optional.empty();
        }

        int currentEpoch = recentBlock.getEpochNumber();
        return Optional.of(currentEpoch - 1);
    }
}
