package com.bloxbean.cardano.yaci.store.mcp.server.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service responsible for refreshing all MCP-related materialized views.
 *
 * Manages scheduled refresh operations for:
 * 1. token_holder_summary_mv - Token holder statistics (every 3 hours)
 * 2. address_credential_mapping - Address to credential mapping (daily)
 * 3. address_token_diversity_mv - Address portfolio diversity stats (every 7 hours)
 *
 * Refresh Strategy:
 * - Uses REFRESH MATERIALIZED VIEW CONCURRENTLY for zero downtime
 * - Scheduled via cron expressions (NOT run on application startup)
 * - Independent schedules for each view based on update frequency needs
 *
 * Important: @Scheduled with cron does NOT run on application startup.
 * Refreshes only occur at the specified cron times, so multiple server
 * restarts will NOT trigger multiple refreshes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@EnableScheduling
@ConditionalOnProperty(
    name = "store.mcp-server.aggregation.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class McpMaterializedViewRefreshService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Refresh the token_holder_summary_mv materialized view.
     *
     * Schedule: Every 3 hours at minute 0 (12:00, 3:00, 6:00, 9:00, etc.)
     * Duration: ~8 minutes for full refresh on mainnet
     * Data staleness: Up to 3 hours
     *
     * This view enables fast token holder queries:
     * - tokens-with-min-holders: 54+ seconds to &lt;100ms (546x faster)
     * - token-holder-stats-by-policy: 2+ hours to &lt;100ms (72,000x faster)
     *
     * Configuration:
     * - Enable/disable: store.mcp-server.aggregation.token-holder-summary.enabled
     * - Cron schedule: store.mcp-server.aggregation.token-holder-summary.refresh-cron
     *
     * Note: First refresh after database restart may fail if view doesn't exist.
     * View must be created manually via sql/token-holder-summary-mv.sql
     */
    @Scheduled(cron = "${store.mcp-server.aggregation.token-holder-summary.refresh-cron:0 0 */3 * * *}")
    @ConditionalOnProperty(
        name = "store.mcp-server.aggregation.token-holder-summary.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public void refreshTokenHolderSummary() {
        log.info("🔄 Starting token_holder_summary_mv refresh...");
        long startTime = System.currentTimeMillis();

        try {
            // Use CONCURRENTLY to allow reads during refresh
            // Note: Requires a unique index on the MV (created in SQL file)
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY token_holder_summary_mv");

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ token_holder_summary_mv refresh completed in {} ms ({} minutes)",
                    duration, duration / 60000.0);

            // Log row count for monitoring
            Long rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM token_holder_summary_mv", Long.class);
            log.info("   Materialized view contains {} unique tokens", rowCount);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ token_holder_summary_mv refresh failed after {} ms: {}",
                    duration, e.getMessage(), e);

            // Don't throw - next scheduled refresh will retry
            // This prevents the scheduler from stopping on errors
        }
    }

    /**
     * Refresh the address_credential_mapping materialized view.
     *
     * Schedule: Daily at 2:00 AM
     * Duration: Varies based on UTXO count (~1-5 minutes typical)
     * Data staleness: Up to 24 hours
     *
     * This view enables fast payment credential lookups for:
     * - Contract TVL estimation
     * - Franken address aggregation
     * - Payment credential to address mapping
     *
     * Configuration:
     * - Enable/disable: store.mcp-server.aggregation.address-credential-mapping.enabled
     * - Cron schedule: store.mcp-server.aggregation.address-credential-mapping.refresh-cron
     *
     * Note: First refresh after database restart may fail if view doesn't exist.
     * View must be created manually via sql/address-credential-mapping.sql
     */
    @Scheduled(cron = "${store.mcp-server.aggregation.address-credential-mapping.refresh-cron:0 0 2 * * *}")
    @ConditionalOnProperty(
        name = "store.mcp-server.aggregation.address-credential-mapping.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public void refreshAddressCredentialMapping() {
        log.info("🔄 Starting address_credential_mapping refresh...");
        long startTime = System.currentTimeMillis();

        try {
            // Use CONCURRENTLY to allow reads during refresh
            // Note: Requires a unique index on the MV (created in SQL file)
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY address_credential_mapping");

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ address_credential_mapping refresh completed in {} ms ({} minutes)",
                    duration, duration / 60000.0);

            // Log row count for monitoring
            Long rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM address_credential_mapping", Long.class);
            log.info("   Materialized view contains {} unique addresses", rowCount);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ address_credential_mapping refresh failed after {} ms: {}",
                    duration, e.getMessage(), e);

            // Don't throw - next scheduled refresh will retry
            // This prevents the scheduler from stopping on errors
        }
    }

    /**
     * Refresh the address_token_diversity_mv materialized view.
     *
     * Schedule: Every 12 hours at minute 0 (12:00 AM, 12:00 PM)
     * Duration: ~2-3 minutes for full refresh on mainnet
     * Data staleness: Up to 12 hours
     *
     * This view enables fast address diversity queries:
     * - address-token-diversity: 109 seconds to &lt;1s (198x faster)
     *
     * Configuration:
     * - Enable/disable: store.mcp-server.aggregation.address-token-diversity.enabled
     * - Cron schedule: store.mcp-server.aggregation.address-token-diversity.refresh-cron
     *
     * Note: First refresh after database restart may fail if view doesn't exist.
     * View must be created manually via sql/address-token-diversity-mv.sql
     */
    @Scheduled(cron = "${store.mcp-server.aggregation.address-token-diversity.refresh-cron:0 0 */12 * * *}")
    @ConditionalOnProperty(
        name = "store.mcp-server.aggregation.address-token-diversity.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public void refreshAddressTokenDiversity() {
        log.info("🔄 Starting address_token_diversity_mv refresh...");
        long startTime = System.currentTimeMillis();

        try {
            // Use CONCURRENTLY to allow reads during refresh
            // Note: Requires a unique index on the MV (created in SQL file)
            jdbcTemplate.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY address_token_diversity_mv");

            long duration = System.currentTimeMillis() - startTime;
            log.info("✅ address_token_diversity_mv refresh completed in {} ms ({} minutes)",
                    duration, duration / 60000.0);

            // Log row count for monitoring
            Long rowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM address_token_diversity_mv", Long.class);
            log.info("   Materialized view contains {} unique addresses", rowCount);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("❌ address_token_diversity_mv refresh failed after {} ms: {}",
                    duration, e.getMessage(), e);

            // Don't throw - next scheduled refresh will retry
            // This prevents the scheduler from stopping on errors
        }
    }

    /**
     * Manual refresh trigger for token holder summary view.
     * Can be called programmatically or via admin endpoints if needed.
     */
    public void triggerTokenHolderSummaryRefresh() {
        log.info("Manual refresh triggered for token_holder_summary_mv");
        refreshTokenHolderSummary();
    }

    /**
     * Manual refresh trigger for address credential mapping view.
     * Can be called programmatically or via admin endpoints if needed.
     */
    public void triggerAddressCredentialMappingRefresh() {
        log.info("Manual refresh triggered for address_credential_mapping");
        refreshAddressCredentialMapping();
    }

    /**
     * Manual refresh trigger for address token diversity view.
     * Can be called programmatically or via admin endpoints if needed.
     */
    public void triggerAddressTokenDiversityRefresh() {
        log.info("Manual refresh triggered for address_token_diversity_mv");
        refreshAddressTokenDiversity();
    }

    /**
     * Trigger refresh of all materialized views.
     * Useful for manual full refresh operations.
     */
    public void triggerAllRefreshes() {
        log.info("Manual refresh triggered for ALL materialized views");
        refreshTokenHolderSummary();
        refreshAddressCredentialMapping();
        refreshAddressTokenDiversity();
    }
}
