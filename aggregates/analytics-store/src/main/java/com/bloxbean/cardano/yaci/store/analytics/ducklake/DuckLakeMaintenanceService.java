package com.bloxbean.cardano.yaci.store.analytics.ducklake;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Deletes the data files that no current snapshot of the DuckLake catalog references any more.
 *
 * <p>A partition replace (re-export, repair) retires the partition's old data files, but
 * DuckLake keeps them, and every snapshot, until snapshots are expired and old files are
 * cleaned up. Without this task they stay on disk forever.</p>
 *
 * <p>Each run, under the writer lock:</p>
 * <ol>
 *   <li>{@code ducklake_expire_snapshots(older_than => now() - retention)} expires the snapshots
 *       older than the retention (never the latest one). The files only they referenced are
 *       scheduled for deletion, with the time of scheduling.</li>
 *   <li>{@code ducklake_cleanup_old_files(older_than => now() - retention)} deletes the files that
 *       were scheduled more than the retention ago (never {@code cleanup_all}).</li>
 * </ol>
 *
 * <p>So a file is deleted at least {@code retention} after it stopped being part of the latest
 * snapshot. The analytics query layer builds its views from the file list of the latest
 * snapshot and refreshes them every 5 minutes; with the default 7-day retention a view keeps
 * every file it references unless its refresh fails for a week. Orphaned files (never
 * committed, for example from an export that crashed) are not touched: the export path may
 * also hold files DuckLake never tracked, such as Parquet-mode exports.</p>
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
public class DuckLakeMaintenanceService {

    private final DataSource writerDataSource;
    private final DuckDbConnectionHelper connectionHelper;
    private final DuckLakeWriterLock writerLock;
    private final AnalyticsStoreProperties properties;

    public DuckLakeMaintenanceService(@Qualifier("duckDbWriterDataSource") DataSource writerDataSource,
                                      DuckDbConnectionHelper connectionHelper,
                                      DuckLakeWriterLock writerLock,
                                      AnalyticsStoreProperties properties) {
        this.writerDataSource = writerDataSource;
        this.connectionHelper = connectionHelper;
        this.writerLock = writerLock;
        this.properties = properties;
    }

    /**
     * @param expiredSnapshots snapshots expired by this run
     * @param deletedFiles     data and delete files removed from disk by this run
     */
    public record Result(long expiredSnapshots, long deletedFiles) {
    }

    @Scheduled(fixedDelayString = "${yaci.store.analytics.ducklake.maintenance.interval-minutes:1440}",
            initialDelayString = "${yaci.store.analytics.ducklake.maintenance.initial-delay-minutes:60}",
            timeUnit = TimeUnit.MINUTES)
    public void scheduledMaintenance() {
        if (!properties.getDucklake().getMaintenance().isEnabled()) {
            return;
        }
        try {
            run();
        } catch (SQLException e) {
            log.warn("DuckLake maintenance failed ({}); will retry at the next interval",
                    DuckDbConnectionHelper.redactSecrets(e.getMessage()));
        } catch (RuntimeException e) {
            log.warn("DuckLake maintenance failed ({}); will retry at the next interval",
                    DuckDbConnectionHelper.redactSecrets(String.valueOf(e.getMessage())));
        }
    }

    /** Runs maintenance with the configured retention (at least one hour). */
    public Result run() throws SQLException {
        int hours = Math.max(1, properties.getDucklake().getMaintenance().getRetentionHours());
        return run(Duration.ofHours(hours));
    }

    /**
     * Expires the snapshots older than {@code retention} and deletes the files scheduled for
     * deletion more than {@code retention} ago.
     */
    public Result run(Duration retention) throws SQLException {
        long seconds = Math.max(0, retention.toSeconds());
        try (DuckLakeWriterLock.Guard ignored = writerLock.acquire();
             Connection conn = writerDataSource.getConnection()) {
            connectionHelper.prepareConnectionForDuckLake(conn, false, false);
            long expired = countRows(conn, String.format(
                    "CALL ducklake_expire_snapshots('ducklake_catalog', older_than => now() - INTERVAL %d SECOND);",
                    seconds));
            long deleted = countRows(conn, String.format(
                    "CALL ducklake_cleanup_old_files('ducklake_catalog', older_than => now() - INTERVAL %d SECOND);",
                    seconds));
            if (expired > 0 || deleted > 0) {
                log.info("DuckLake maintenance: expired {} snapshots and deleted {} unreferenced files "
                        + "(retention {}h)", expired, deleted, retention.toHours());
            } else {
                log.debug("DuckLake maintenance: nothing to expire or delete (retention {}h)", retention.toHours());
            }
            return new Result(expired, deleted);
        } catch (SQLException e) {
            throw DuckDbConnectionHelper.sanitize(e);
        }
    }

    private static long countRows(Connection conn, String sql) throws SQLException {
        long rows = 0;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rows++;
            }
        }
        return rows;
    }
}
