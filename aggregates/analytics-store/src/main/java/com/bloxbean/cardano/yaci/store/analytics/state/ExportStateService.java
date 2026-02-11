package com.bloxbean.cardano.yaci.store.analytics.state;

import com.bloxbean.cardano.yaci.store.analytics.writer.ExportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportStateService {

    private final ExportStateRepository repository;

    /**
     * Get current state for a partition
     */
    public ExportState getState(String tableName, String partitionValue) {
        return repository.findById(new ExportStateId(tableName, partitionValue))
            .orElse(null);
    }

    /**
     * Mark export as in-progress (prevents concurrent exports)
     */
    @Transactional
    public ExportState markInProgress(String tableName, String partitionValue) {
        ExportState state = repository.findById(new ExportStateId(tableName, partitionValue))
            .orElse(new ExportState(tableName, partitionValue));

        // Only increment retry count if previous attempt failed
        if (state.getStatus() == ExportStatus.FAILED) {
            state.setRetryCount(state.getRetryCount() + 1);
        }

        state.setStatus(ExportStatus.IN_PROGRESS);
        state.setStartedAt(LocalDateTime.now());

        // Clear stale fields from previous attempts to prevent misleading metrics
        state.setRowCount(null);
        state.setFileSizeBytes(null);
        state.setFilePath(null);
        state.setChecksumSha256(null);
        state.setCompletedAt(null);
        state.setDurationSeconds(null);
        state.setErrorMessage(null);

        return repository.save(state);
    }

    /**
     * Mark export as completed
     */
    @Transactional
    public void markCompleted(ExportState state, ExportResult result, String checksum) {
        state.setStatus(ExportStatus.COMPLETED);
        state.setCompletedAt(LocalDateTime.now());
        state.setRowCount(result.getRowCount());
        state.setFileSizeBytes(result.getFileSize());
        state.setFilePath(result.getFilePath());
        state.setChecksumSha256(checksum);
        state.setErrorMessage(null);

        Duration duration = Duration.between(state.getStartedAt(), state.getCompletedAt());
        state.setDurationSeconds((int) duration.getSeconds());

        repository.save(state);

        log.info("✅ Export completed: {}/{} ({} rows, {} bytes, {} seconds)",
            state.getId().getTableName(),
            state.getId().getPartitionValue(),
            result.getRowCount(),
            result.getFileSize(),
            state.getDurationSeconds());
    }

    /**
     * Mark export as failed
     */
    @Transactional
    public void markFailed(ExportState state, String errorMessage) {
        state.setStatus(ExportStatus.FAILED);
        state.setCompletedAt(LocalDateTime.now());

        // Calculate duration for SLA monitoring
        if (state.getStartedAt() != null) {
            Duration duration = Duration.between(state.getStartedAt(), state.getCompletedAt());
            state.setDurationSeconds((int) duration.getSeconds());
        }

        // Truncate error message if too long
        if (errorMessage != null && errorMessage.length() > 1000) {
            state.setErrorMessage(errorMessage.substring(0, 1000) + "... (truncated)");
        } else {
            state.setErrorMessage(errorMessage);
        }

        repository.save(state);

        log.error("❌ Export failed: {}/{} (duration: {} seconds)",
            state.getId().getTableName(),
            state.getId().getPartitionValue(),
            state.getDurationSeconds());
    }

    /**
     * Get all completed partitions (for gap detection)
     */
    public Set<String> getCompletedPartitions(String tableName) {
        return new HashSet<>(repository.findCompletedPartitions(tableName));
    }

    /**
     * Find failed exports for retry
     */
    public List<ExportState> getFailedExports(String tableName, int maxRetries) {
        return repository.findFailedExports(tableName, maxRetries);
    }

    /**
     * Find in-progress exports (for monitoring stale jobs)
     */
    public List<ExportState> getInProgressExports(String tableName) {
        return repository.findInProgressExports(tableName);
    }

    /**
     * Reset stale IN_PROGRESS exports to FAILED.
     *
     * Finds exports that have been IN_PROGRESS for longer than the specified timeout
     * and marks them as FAILED. This handles cases where the application crashed or
     * was forcibly stopped during an export, leaving the state stuck.
     *
     * @param staleTimeoutMinutes exports older than this many minutes are considered stale
     * @return number of stale exports that were reset
     */
    @Transactional
    public int resetStaleInProgressExports(int staleTimeoutMinutes) {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(staleTimeoutMinutes);
        List<ExportState> staleExports = repository.findStaleInProgressExports(cutoff);

        for (ExportState state : staleExports) {
            state.setStatus(ExportStatus.FAILED);
            state.setErrorMessage("Reset: stale IN_PROGRESS (exceeded " + staleTimeoutMinutes + " min timeout)");
            state.setCompletedAt(LocalDateTime.now());
            if (state.getStartedAt() != null) {
                Duration duration = Duration.between(state.getStartedAt(), state.getCompletedAt());
                state.setDurationSeconds((int) duration.getSeconds());
            }
            repository.save(state);
            log.warn("Reset stale IN_PROGRESS export: {}/{} (started at: {})",
                    state.getId().getTableName(), state.getId().getPartitionValue(), state.getStartedAt());
        }

        return staleExports.size();
    }

    /**
     * Reset all IN_PROGRESS exports to FAILED.
     *
     * Used during application shutdown to ensure no exports are left in an
     * inconsistent IN_PROGRESS state.
     *
     * @return number of exports that were reset
     */
    @Transactional
    public int resetAllInProgressExports() {
        List<ExportState> inProgressExports = repository.findAllInProgressExports();

        for (ExportState state : inProgressExports) {
            state.setStatus(ExportStatus.FAILED);
            state.setErrorMessage("Reset: application shutdown while export was in progress");
            state.setCompletedAt(LocalDateTime.now());
            if (state.getStartedAt() != null) {
                Duration duration = Duration.between(state.getStartedAt(), state.getCompletedAt());
                state.setDurationSeconds((int) duration.getSeconds());
            }
            repository.save(state);
            log.warn("Reset IN_PROGRESS export on shutdown: {}/{}",
                    state.getId().getTableName(), state.getId().getPartitionValue());
        }

        return inProgressExports.size();
    }
}
