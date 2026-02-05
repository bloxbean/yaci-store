package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.LedgerStateStatusDto;
import com.bloxbean.cardano.yaci.store.adminui.dto.SyncStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Service to retrieve ledger state status by querying the database directly.
 * Results are cached for 30 seconds to reduce database load.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerStateStatusService {

    private static final long CACHE_DURATION_MS = 30000; // 30 seconds

    private final SyncStatusService syncStatusService;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    // Caching fields
    private volatile LedgerStateStatusDto cachedStatus;
    private volatile long lastFetchTime = 0;

    public LedgerStateStatusDto getLedgerStateStatus() {
        // Return cached result if still valid
        if (cachedStatus != null && System.currentTimeMillis() - lastFetchTime < CACHE_DURATION_MS) {
            return cachedStatus;
        }

        // Check if AdaPot module is enabled via configuration
        boolean adapotEnabled = Boolean.parseBoolean(
                environment.getProperty("store.adapot.enabled", "false")
        );

        SyncStatusDto syncStatus = syncStatusService.getSyncStatus();
        int currentEpoch = syncStatus.getEpoch();

        // Query database directly
        LedgerStateStatusDto dbResult = tryGetFromDatabase(currentEpoch);
        if (dbResult != null) {
            return updateCache(dbResult, adapotEnabled);
        }

        // Module not available
        return updateCache(LedgerStateStatusDto.builder()
                .currentEpoch(currentEpoch)
                .lastProcessedEpoch(0)
                .jobRunning(false)
                .lastJobStatus("MODULE_NOT_AVAILABLE")
                .lastJobError(null)
                .lastJobTimestamp(null)
                .treasury(null)
                .reserves(null)
                .build(), adapotEnabled);
    }

    private LedgerStateStatusDto updateCache(LedgerStateStatusDto status, boolean enabled) {
        status.setEnabled(enabled);
        this.cachedStatus = status;
        this.lastFetchTime = System.currentTimeMillis();
        return status;
    }

    private LedgerStateStatusDto tryGetFromDatabase(int currentEpoch) {
        if (jdbcTemplate == null) {
            return null;
        }

        try {
            // Check if adapot_jobs table exists
            Integer tableCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = 'adapot_jobs'",
                    Integer.class);

            if (tableCount == null || tableCount == 0) {
                return null;
            }

            // Get latest completed job epoch
            Integer lastProcessedEpoch = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(MAX(epoch), 0) FROM adapot_jobs WHERE status = 'COMPLETED' AND type = 'REWARD_CALC'",
                    Integer.class);

            // Check for running jobs (STARTED without error_message means actually running)
            Integer runningCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM adapot_jobs WHERE status = 'STARTED' AND type = 'REWARD_CALC' AND (error_message IS NULL OR error_message = '')",
                    Integer.class);

            // Get last job status and error message
            String lastJobStatus = "NOT_STARTED";
            String lastJobError = null;
            Integer lastErrorEpoch = null;
            try {
                var result = jdbcTemplate.queryForMap(
                        "SELECT epoch, status, error_message FROM adapot_jobs WHERE type = 'REWARD_CALC' ORDER BY epoch DESC LIMIT 1");
                lastJobStatus = (String) result.get("status");
                lastJobError = (String) result.get("error_message");

                // If job is STARTED with an error_message, it's actually an error state
                if ("STARTED".equals(lastJobStatus) && lastJobError != null && !lastJobError.isEmpty()) {
                    lastJobStatus = "ERROR";
                }

                // Set error epoch if there's an error
                if (lastJobError != null && !lastJobError.isEmpty()) {
                    lastErrorEpoch = ((Number) result.get("epoch")).intValue();
                }
            } catch (Exception e) {
                // No jobs found
            }

            // Query treasury and reserves from adapot table if we have a completed epoch
            String treasury = null;
            String reserves = null;
            if (lastProcessedEpoch != null && lastProcessedEpoch > 0 && (lastJobError == null || lastJobError.isEmpty())) {
                try {
                    var adapotResult = jdbcTemplate.queryForMap(
                            "SELECT treasury, reserves FROM adapot WHERE epoch = ?",
                            lastProcessedEpoch);
                    Object treasuryObj = adapotResult.get("treasury");
                    Object reservesObj = adapotResult.get("reserves");

                    if (treasuryObj != null) {
                        treasury = treasuryObj.toString();
                    }
                    if (reservesObj != null) {
                        reserves = reservesObj.toString();
                    }
                } catch (Exception e) {
                    log.debug("Could not fetch treasury/reserves for epoch {}: {}", lastProcessedEpoch, e.getMessage());
                }
            }

            return LedgerStateStatusDto.builder()
                    .currentEpoch(currentEpoch)
                    .lastProcessedEpoch(lastProcessedEpoch != null ? lastProcessedEpoch : 0)
                    .jobRunning(runningCount != null && runningCount > 0)
                    .lastJobStatus(lastJobStatus != null ? lastJobStatus : "NOT_STARTED")
                    .lastJobError(lastJobError)
                    .lastErrorEpoch(lastErrorEpoch)
                    .lastJobTimestamp(null)
                    .treasury(treasury)
                    .reserves(reserves)
                    .build();

        } catch (Exception e) {
            log.debug("Could not get ledger state from database: {}", e.getMessage());
            return null;
        }
    }
}
