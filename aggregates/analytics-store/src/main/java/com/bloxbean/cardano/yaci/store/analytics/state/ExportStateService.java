package com.bloxbean.cardano.yaci.store.analytics.state;

import com.bloxbean.cardano.yaci.store.analytics.writer.ExportResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
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

    private static final Table<?> EXPORT_STATE = DSL.table("analytics_export_state");
    private static final Field<String> TABLE_NAME = DSL.field("table_name", String.class);
    private static final Field<String> PARTITION_VALUE = DSL.field("partition_value", String.class);
    private static final Field<String> EXPORT_STATUS = DSL.field("export_status", String.class);
    private static final Field<Integer> RETRY_COUNT = DSL.field("retry_count", Integer.class);
    private static final Field<LocalDateTime> STARTED_AT = DSL.field("started_at", LocalDateTime.class);
    private static final Field<LocalDateTime> COMPLETED_AT = DSL.field("completed_at", LocalDateTime.class);
    private static final Field<LocalDateTime> CREATED_AT = DSL.field("created_at", LocalDateTime.class);
    private static final Field<LocalDateTime> UPDATED_AT = DSL.field("updated_at", LocalDateTime.class);
    private static final Field<Long> ROW_COUNT = DSL.field("row_count", Long.class);
    private static final Field<Long> FILE_SIZE_BYTES = DSL.field("file_size_bytes", Long.class);
    private static final Field<String> FILE_PATH = DSL.field("file_path", String.class);
    private static final Field<String> CHECKSUM_SHA256 = DSL.field("checksum_sha256", String.class);
    private static final Field<Integer> DURATION_SECONDS = DSL.field("duration_seconds", Integer.class);
    private static final Field<String> ERROR_MESSAGE = DSL.field("error_message", String.class);

    private final ExportStateRepository repository;
    private final DSLContext dsl;

    /**
     * Get current state for a partition
     */
    public ExportState getState(String tableName, String partitionValue) {
        return repository.findById(new ExportStateId(tableName, partitionValue))
            .orElse(null);
    }

    /**
     * Mark export as in-progress (prevents concurrent exports).
     * Uses jOOQ INSERT ... ON CONFLICT to atomically upsert, avoiding the
     * JPA @EmbeddedId merge() confusion that caused duplicate key errors.
     */
    @Transactional
    public ExportState markInProgress(String tableName, String partitionValue) {
        LocalDateTime now = LocalDateTime.now();

        dsl.insertInto(EXPORT_STATE)
            .set(TABLE_NAME, tableName)
            .set(PARTITION_VALUE, partitionValue)
            .set(EXPORT_STATUS, "IN_PROGRESS")
            .set(RETRY_COUNT, 0)
            .set(STARTED_AT, now)
            .set(CREATED_AT, now)
            .set(UPDATED_AT, now)
            .set(ROW_COUNT, (Long) null)
            .set(FILE_SIZE_BYTES, (Long) null)
            .set(FILE_PATH, (String) null)
            .set(CHECKSUM_SHA256, (String) null)
            .set(COMPLETED_AT, (LocalDateTime) null)
            .set(DURATION_SECONDS, (Integer) null)
            .set(ERROR_MESSAGE, (String) null)
            .onConflict(TABLE_NAME, PARTITION_VALUE)
            .doUpdate()
            .set(EXPORT_STATUS, DSL.val("IN_PROGRESS"))
            .set(RETRY_COUNT,
                DSL.when(EXPORT_STATUS.eq("FAILED"), RETRY_COUNT.plus(1))
                    .otherwise(RETRY_COUNT))
            .set(STARTED_AT, DSL.val(now))
            .set(UPDATED_AT, DSL.val(now))
            .setNull(ROW_COUNT)
            .setNull(FILE_SIZE_BYTES)
            .setNull(FILE_PATH)
            .setNull(CHECKSUM_SHA256)
            .setNull(COMPLETED_AT)
            .setNull(DURATION_SECONDS)
            .setNull(ERROR_MESSAGE)
            .execute();

        return repository.findById(new ExportStateId(tableName, partitionValue))
            .orElseThrow(() -> new IllegalStateException(
                "Export state not found after upsert for " + tableName + "/" + partitionValue));
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
