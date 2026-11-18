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
           "AND e.startedAt < :cutoff " +
           "ORDER BY e.startedAt")
    List<ExportState> findStaleInProgressExports(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT e FROM ExportState e " +
           "WHERE e.status = 'IN_PROGRESS'")
    List<ExportState> findAllInProgressExports();

    @Query("SELECT e FROM ExportState e WHERE e.id.tableName = :tableName")
    List<ExportState> findByTableName(@Param("tableName") String tableName);

    /** State rows of a table whose partition key starts with the given prefix (e.g. {@code date=}). */
    @Query("SELECT e FROM ExportState e " +
           "WHERE e.id.tableName = :tableName AND e.id.partitionValue LIKE CONCAT(:prefix, '%')")
    List<ExportState> findByTableNameAndPartitionPrefix(@Param("tableName") String tableName,
                                                        @Param("prefix") String prefix);

    /** Table name, partition value and row count of every COMPLETED partition that holds rows. */
    @Query("SELECT e.id.tableName, e.id.partitionValue, e.rowCount FROM ExportState e " +
           "WHERE e.status = 'COMPLETED' AND e.rowCount > 0")
    List<Object[]> findCompletedPartitionsWithRows();

    /** Table name and partition value of every COMPLETED partition stored with no rows. */
    @Query("SELECT e.id.tableName, e.id.partitionValue FROM ExportState e " +
           "WHERE e.status = 'COMPLETED' AND (e.rowCount = 0 OR e.rowCount IS NULL)")
    List<Object[]> findCompletedEmptyPartitions();

}
