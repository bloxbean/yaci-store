package com.bloxbean.cardano.yaci.store.analytics.admin;

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

    private final ExportStateRepository repository;

    /**
     * Reset export state to allow re-export.
     *
     * Deletes the state record for the specified partition, allowing it to be exported again.
     *
     * @param tableName The table name
     * @param partitionValue The partition value (e.g., "2024-10-31")
     */
    @Transactional
    public void resetExportState(String tableName, String partitionValue) {
        ExportStateId id = new ExportStateId(tableName, partitionValue);
        repository.findById(id).ifPresent(state -> {
            log.warn("Resetting export state for {}/{}", tableName, partitionValue);
            repository.delete(state);
        });
    }

    /**
     * Reset all exports in a date range.
     *
     * Useful for bulk re-export scenarios.
     *
     * @param tableName The table name
     * @param startDate Start date (inclusive, yyyy-MM-dd format)
     * @param endDate End date (inclusive, yyyy-MM-dd format)
     * @return Number of states reset
     */
    @Transactional
    public int resetDateRange(String tableName, String startDate, String endDate) {
        // Find all states in range
        List<ExportState> states = repository.findAll().stream()
            .filter(s -> s.getId().getTableName().equals(tableName))
            .filter(s -> {
                String partition = s.getId().getPartitionValue();
                return partition.compareTo(startDate) >= 0 && partition.compareTo(endDate) <= 0;
            })
            .toList();

        repository.deleteAll(states);
        log.warn("Reset {} export states for {} from {} to {}",
            states.size(), tableName, startDate, endDate);

        return states.size();
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
        List<ExportState> allStates = repository.findAll().stream()
            .filter(s -> s.getId().getTableName().equals(tableName))
            .toList();

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
