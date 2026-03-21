package com.bloxbean.cardano.yaci.store.analytics.state;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExportStateRepository extends JpaRepository<ExportState, ExportStateId> {

    @Query("SELECT e.id.partitionValue FROM ExportState e " +
           "WHERE e.id.tableName = :tableName AND e.status = 'COMPLETED'")
    List<String> findCompletedPartitions(@Param("tableName") String tableName);

    @Query("SELECT e FROM ExportState e " +
           "WHERE e.id.tableName = :tableName " +
           "AND e.status = 'FAILED' " +
           "AND e.retryCount < :maxRetries " +
           "ORDER BY e.id.partitionValue")
    List<ExportState> findFailedExports(
        @Param("tableName") String tableName,
        @Param("maxRetries") int maxRetries
    );

    @Query("SELECT e FROM ExportState e " +
           "WHERE e.id.tableName = :tableName " +
           "AND e.status = 'IN_PROGRESS' " +
           "ORDER BY e.startedAt DESC")
    List<ExportState> findInProgressExports(@Param("tableName") String tableName);

    @Query("SELECT e FROM ExportState e " +
           "WHERE e.status = 'IN_PROGRESS' " +
           "AND e.startedAt < :cutoff " +
           "ORDER BY e.startedAt")
    List<ExportState> findStaleInProgressExports(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT e FROM ExportState e " +
           "WHERE e.status = 'IN_PROGRESS'")
    List<ExportState> findAllInProgressExports();

    /**
     * Atomically insert or update export state to IN_PROGRESS.
     */
    @Modifying
    @Query(value = """
        INSERT INTO analytics_export_state
            (table_name, partition_value, export_status, retry_count, started_at, created_at, updated_at,
             row_count, file_size_bytes, file_path, checksum_sha256, completed_at, duration_seconds, error_message)
        VALUES (:tableName, :partitionValue, 'IN_PROGRESS', 0, :now, :now, :now,
                NULL, NULL, NULL, NULL, NULL, NULL, NULL)
        ON CONFLICT (table_name, partition_value) DO UPDATE SET
            export_status = 'IN_PROGRESS',
            retry_count = CASE WHEN analytics_export_state.export_status = 'FAILED'
                          THEN analytics_export_state.retry_count + 1
                          ELSE analytics_export_state.retry_count END,
            started_at = :now,
            updated_at = :now,
            row_count = NULL,
            file_size_bytes = NULL,
            file_path = NULL,
            checksum_sha256 = NULL,
            completed_at = NULL,
            duration_seconds = NULL,
            error_message = NULL
        """, nativeQuery = true)
    void upsertInProgress(@Param("tableName") String tableName,
                           @Param("partitionValue") String partitionValue,
                           @Param("now") LocalDateTime now);
}
