package com.bloxbean.cardano.yaci.store.analytics.gap;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
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
            String partitionValue = current.toString(); // yyyy-MM-dd format
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
     * @return Safe end date for exports as LocalDate
     */
    public LocalDate getExportEndDate() {
        LocalDate latestSynced = getLatestSyncedDate();
        int bufferDays = properties.getContinuousSync().getBufferDays();
        return latestSynced.minusDays(bufferDays);
    }
}
