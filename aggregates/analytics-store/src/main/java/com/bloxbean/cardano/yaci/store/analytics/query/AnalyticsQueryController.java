package com.bloxbean.cardano.yaci.store.analytics.query;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Query endpoints for reading analytical data from DuckLake.
 *
 * Enabled when DuckLake storage is configured.
 * Uses the reader DataSource with multiple concurrent connections for optimal throughput.
 *
 * Provides read-only access to exported data:
 * - Transaction counts and statistics
 * - Address balance aggregates
 * - Output statistics
 */
@RestController
@RequestMapping("/api/v1/analytics/query")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "yaci.store.analytics.storage", name = "type", havingValue = "ducklake")
public class AnalyticsQueryController {

    private final DuckLakeQueryService queryService;

    /**
     * Get total transaction count across all partitions.
     *
     * Example: GET /api/v1/analytics/query/stats/transaction-count
     *
     * @return Total transaction count
     */
    @GetMapping("/stats/transaction-count")
    public ResponseEntity<CountResult> getTotalTransactionCount() {
        log.debug("Fetching total transaction count");
        long count = queryService.getTotalTransactionCount();
        return ResponseEntity.ok(new CountResult("transactions", count, "Total across all partitions"));
    }

    /**
     * Get transaction count for a specific date.
     *
     * Example: GET /api/v1/analytics/query/stats/transaction-count/date/2024-01-15
     *
     * @param date Target date (yyyy-MM-dd)
     * @return Transaction count for that date
     */
    @GetMapping("/stats/transaction-count/date/{date}")
    public ResponseEntity<CountResult> getTransactionCountByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("Fetching transaction count for date: {}", date);
        long count = queryService.getTransactionCountByDate(date);
        return ResponseEntity.ok(new CountResult("transactions", count, "Date: " + date));
    }

    /**
     * Get transaction count for a date range.
     *
     * Example: GET /api/v1/analytics/query/stats/transaction-count/range?startDate=2024-01-01&amp;endDate=2024-01-31
     *
     * @param startDate Start date (inclusive, yyyy-MM-dd)
     * @param endDate End date (inclusive, yyyy-MM-dd)
     * @return Transaction count for date range
     */
    @GetMapping("/stats/transaction-count/range")
    public ResponseEntity<CountResult> getTransactionCountByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.debug("Fetching transaction count for range: {} to {}", startDate, endDate);
        long count = queryService.getTransactionCountByDateRange(startDate, endDate);
        return ResponseEntity.ok(new CountResult("transactions", count,
                String.format("Range: %s to %s", startDate, endDate)));
    }

    /**
     * Get total address balance record count.
     *
     * Example: GET /api/v1/analytics/query/stats/address-balance-count
     *
     * @return Total address balance count
     */
    @GetMapping("/stats/address-balance-count")
    public ResponseEntity<CountResult> getTotalAddressBalanceCount() {
        log.debug("Fetching total address balance count");
        long count = queryService.getTotalAddressBalanceCount();
        return ResponseEntity.ok(new CountResult("address_balance", count, "Total across all partitions"));
    }

    /**
     * Get transaction output statistics.
     *
     * Example: GET /api/v1/analytics/query/stats/output-stats
     *
     * @return Aggregate statistics about transaction outputs
     */
    @GetMapping("/stats/output-stats")
    public ResponseEntity<OutputStatsResult> getTransactionOutputStats() {
        log.debug("Fetching transaction output statistics");
        var stats = queryService.getTransactionOutputStats();
        return ResponseEntity.ok(new OutputStatsResult(
                stats.totalOutputs(),
                stats.totalLovelace(),
                stats.avgLovelace()
        ));
    }

    // DTOs

    @Data
    public static class CountResult {
        private final String table;
        private final long count;
        private final String description;
    }

    @Data
    public static class OutputStatsResult {
        private final long totalOutputs;
        private final long totalLovelace;
        private final double avgLovelace;
    }
}
