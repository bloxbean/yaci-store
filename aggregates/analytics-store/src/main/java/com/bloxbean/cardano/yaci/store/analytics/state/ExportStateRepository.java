package com.bloxbean.cardano.yaci.store.analytics.state;

import org.springframework.data.jpa.repository.JpaRepository;
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
           "AND e.startedAt < :threshold")
    List<ExportState> findStaleInProgressExports(@Param("threshold") LocalDateTime threshold);
}
