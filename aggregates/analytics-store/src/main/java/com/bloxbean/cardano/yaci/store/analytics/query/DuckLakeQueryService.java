package com.bloxbean.cardano.yaci.store.analytics.query;

import com.bloxbean.cardano.yaci.store.analytics.helper.DuckDbConnectionHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * Query service for reading analytical data from DuckLake.
 *
 * Uses the reader DataSource with multiple concurrent connections for optimal query throughput.
 * All operations are read-only - no data modifications are performed.
 *
 * Example queries:
 * - Total transaction count across all partitions
 * - Transaction count by date range
 * - Aggregate statistics on exported data
 */
@Service
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
public class DuckLakeQueryService {

    private final DataSource duckDbReaderDataSource;
    private final DuckDbConnectionHelper connectionHelper;

    public DuckLakeQueryService(@Qualifier("duckDbReaderDataSource") DataSource duckDbReaderDataSource,
                                DuckDbConnectionHelper connectionHelper) {
        this.duckDbReaderDataSource = duckDbReaderDataSource;
        this.connectionHelper = connectionHelper;
        log.info("Initialized DuckLakeQueryService with reader DataSource");
    }

    /**
     * Get total transaction count across all partitions in DuckLake.
     *
     * @return Total number of transactions
     */
    public long getTotalTransactionCount() {
        String query = "SELECT COUNT(*) as total FROM transactions";
        return executeCountQuery(query);
    }

    /**
     * Get transaction count for a specific date range.
     *
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return Number of transactions in date range
     */
    public long getTransactionCountByDateRange(LocalDate startDate, LocalDate endDate) {
        // DuckLake stores transactions in date-partitioned format: date=2024-01-15
        // Query across multiple partitions using date filter
        String query = String.format(
                "SELECT COUNT(*) as total FROM transactions WHERE date >= '%s' AND date <= '%s'",
                startDate, endDate
        );
        return executeCountQuery(query);
    }

    /**
     * Get transaction count for a specific date.
     *
     * @param date Target date
     * @return Number of transactions on that date
     */
    public long getTransactionCountByDate(LocalDate date) {
        String query = String.format(
                "SELECT COUNT(*) as total FROM transactions WHERE date = '%s'",
                date
        );
        return executeCountQuery(query);
    }

    /**
     * Get total address balance record count across all partitions.
     *
     * @return Total number of address balance records
     */
    public long getTotalAddressBalanceCount() {
        String query = "SELECT COUNT(*) as total FROM address_balance";
        return executeCountQuery(query);
    }

    /**
     * Get statistics about transaction outputs.
     *
     * @return TransactionOutputStats with aggregate metrics
     */
    public TransactionOutputStats getTransactionOutputStats() {
        String query = """
            SELECT
                COUNT(*) as total_outputs,
                SUM(lovelace) as total_lovelace,
                AVG(lovelace) as avg_lovelace
            FROM transaction_outputs
        """;

        try (Connection conn = duckDbReaderDataSource.getConnection()) {
            // Prepare connection for DuckLake queries in READ_ONLY mode (no source attachment needed)
            connectionHelper.prepareConnectionForDuckLake(conn, false, true);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    return new TransactionOutputStats(
                            rs.getLong("total_outputs"),
                            rs.getLong("total_lovelace"),
                            rs.getDouble("avg_lovelace")
                    );
                }
            }
        } catch (SQLException e) {
            log.error("Failed to get transaction output stats: {}", e.getMessage(), e);
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }

        return new TransactionOutputStats(0, 0, 0.0);
    }

    /**
     * Execute a COUNT query and return the result.
     *
     * @param query SQL query with COUNT(*) as total
     * @return Count result
     */
    private long executeCountQuery(String query) {
        try (Connection conn = duckDbReaderDataSource.getConnection()) {
            // Prepare connection for DuckLake queries in READ_ONLY mode (no source attachment needed)
            connectionHelper.prepareConnectionForDuckLake(conn, false, true);

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    return rs.getLong("total");
                }
            }
        } catch (SQLException e) {
            log.error("Failed to execute query: {} - {}", query, e.getMessage(), e);
            throw new RuntimeException("Query failed: " + e.getMessage(), e);
        }

        return 0;
    }

    /**
     * Statistics about transaction outputs.
     *
     * @param totalOutputs Total number of outputs
     * @param totalLovelace Total lovelace amount
     * @param avgLovelace Average lovelace per output
     */
    public record TransactionOutputStats(
            long totalOutputs,
            long totalLovelace,
            double avgLovelace
    ) {}
}
