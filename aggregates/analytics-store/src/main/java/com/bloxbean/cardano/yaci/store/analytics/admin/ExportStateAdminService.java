package com.bloxbean.cardano.yaci.store.analytics.admin;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportState;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateId;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateRepository;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStatus;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Admin service for managing export state and statistics.
 *
 * Provides operations for resetting export state and retrieving aggregate statistics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics", name = "enabled", havingValue = "true")
public class ExportStateAdminService {

    private static final String DATE_PREFIX = "date=";
    private static final String EPOCH_PREFIX = "epoch=";

    private final ExportStateRepository repository;

    /**
     * Reset export state to allow re-export.
     *
     * Deletes the state record for the specified partition, allowing it to be exported again.
     * The exported data is not touched; the next export of the partition replaces it.
     *
     * @param tableName The table name
     * @param partitionValue The partition: {@code 2024-10-31}, {@code date=2024-10-31}, {@code 450}
     *                       or {@code epoch=450}
     * @return true if a state record was deleted, false if there was none
     * @throws IllegalArgumentException if the partition is neither a date nor an epoch
     */
    @Transactional
    public boolean resetExportState(String tableName, String partitionValue) {
        String key = PartitionValue.parse(partitionValue).toPathSegment();
        ExportStateId id = new ExportStateId(tableName, key);
        return repository.findById(id).map(state -> {
            log.warn("Resetting export state for {}/{} (was {})", tableName, key, state.getStatus());
            repository.delete(state);
            return true;
        }).orElse(false);
    }

    /**
     * Reset all daily exports of a table between two days (inclusive).
     *
     * Useful for bulk re-export scenarios. Reads only the table's daily state rows.
     *
     * @param tableName The table name
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Number of states reset
     */
    @Transactional
    public int resetDateRange(String tableName, LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate " + endDate + " is before startDate " + startDate);
        }
        List<ExportState> states = repository.findByTableNameAndPartitionPrefix(tableName, DATE_PREFIX).stream()
            .filter(s -> {
                LocalDate day = parseDay(s.getId().getPartitionValue());
                return day != null && !day.isBefore(startDate) && !day.isAfter(endDate);
            })
            .toList();

        repository.deleteAll(states);
        log.warn("Reset {} export states for {} from {} to {}",
            states.size(), tableName, startDate, endDate);

        return states.size();
    }

    /**
     * Reset the export state of every epoch from {@code startEpoch} to {@code endEpoch} (inclusive)
     * of an EPOCH table, so they are exported again.
     *
     * @return Number of states reset
     */
    @Transactional
    public int resetEpochRange(String tableName, int startEpoch, int endEpoch) {
        if (endEpoch < startEpoch) {
            throw new IllegalArgumentException("endEpoch " + endEpoch + " is before startEpoch " + startEpoch);
        }
        List<ExportState> states = repository.findByTableNameAndPartitionPrefix(tableName, EPOCH_PREFIX).stream()
            .filter(s -> {
                Integer epoch = parseEpoch(s.getId().getPartitionValue());
                return epoch != null && epoch >= startEpoch && epoch <= endEpoch;
            })
            .toList();

        repository.deleteAll(states);
        log.warn("Reset {} export states for {} from epoch {} to {}", states.size(), tableName, startEpoch, endEpoch);
        return states.size();
    }

    private static Integer parseEpoch(String key) {
        try {
            return ((PartitionValue.EpochPartition) PartitionValue.parse(key)).epoch();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static LocalDate parseDay(String key) {
        try {
            return ((PartitionValue.DatePartition) PartitionValue.parse(key)).date();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Get aggregate export statistics.
     *
     * Provides overall metrics for monitoring and reporting.
     *
     * @param tableName The table name
     * @return ExportStatistics with aggregate information
     */
    public ExportStatistics getStatistics(String tableName) {
        List<ExportState> allStates = repository.findByTableName(tableName);

        long completed = allStates.stream().filter(s -> s.getStatus() == ExportStatus.COMPLETED).count();
        long failed = allStates.stream().filter(s -> s.getStatus() == ExportStatus.FAILED).count();
        long inProgress = allStates.stream().filter(s -> s.getStatus() == ExportStatus.IN_PROGRESS).count();

        long totalRows = allStates.stream()
            .filter(s -> s.getRowCount() != null)
            .mapToLong(ExportState::getRowCount)
            .sum();

        long totalBytes = allStates.stream()
            .filter(s -> s.getFileSizeBytes() != null)
            .mapToLong(ExportState::getFileSizeBytes)
            .sum();

        return ExportStatistics.builder()
            .tableName(tableName)
            .totalPartitions(allStates.size())
            .completedCount(completed)
            .failedCount(failed)
            .inProgressCount(inProgress)
            .totalRowsExported(totalRows)
            .totalBytesExported(totalBytes)
            .build();
    }

    /**
     * Export statistics aggregate data.
     */
    @Data
    @Builder
    public static class ExportStatistics {
        private String tableName;
        private long totalPartitions;
        private long completedCount;
        private long failedCount;
        private long inProgressCount;
        private long totalRowsExported;
        private long totalBytesExported;
    }
}
