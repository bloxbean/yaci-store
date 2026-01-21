package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.LedgerStateStatusDto;
import com.bloxbean.cardano.yaci.store.adminui.dto.SyncStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service to retrieve ledger state status using a priority-based approach:
 * 1. First: Try Prometheus/Actuator endpoint (lightweight, no db pressure)
 * 2. Second: Fall back to direct database query
 * 3. Last: Return MODULE_NOT_AVAILABLE if neither works
 *
 * Results are cached for 30 seconds to reduce database/network load.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerStateStatusService {

    private static final long CACHE_DURATION_MS = 30000; // 30 seconds

    private final SyncStatusService syncStatusService;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Value("${management.endpoints.web.base-path:/actuator}")
    private String actuatorBasePath;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Autowired
    private Environment environment;

    @Value("${server.port:8080}")
    private int serverPort;

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

        // Try Prometheus metrics first (lightweight, no db pressure)
        LedgerStateStatusDto prometheusResult = tryGetFromPrometheus(currentEpoch);
        if (prometheusResult != null) {
            return updateCache(prometheusResult, adapotEnabled);
        }

        // Fall back to direct database query
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
                .build(), adapotEnabled);
    }

    private LedgerStateStatusDto updateCache(LedgerStateStatusDto status, boolean enabled) {
        status.setEnabled(enabled);
        this.cachedStatus = status;
        this.lastFetchTime = System.currentTimeMillis();
        return status;
    }

    private LedgerStateStatusDto tryGetFromPrometheus(int currentEpoch) {
        if (restTemplate == null) {
            log.debug("RestTemplate not available for Prometheus metrics");
            return null;
        }

        try {
            String prometheusUrl = "http://localhost:" + serverPort + actuatorBasePath + "/prometheus";
            String metrics = restTemplate.getForObject(prometheusUrl, String.class);

            if (metrics == null || !metrics.contains("yaci_store_adapot_job")) {
                return null;  // AdaPot metrics not available
            }

            int lastSuccessfulEpoch = parseMetricValue(metrics, "yaci_store_adapot_job_last_successful_epoch");
            int inProgressEpoch = parseMetricValue(metrics, "yaci_store_adapot_job_inprogress_epoch");
            int lastUnsuccessfulEpoch = parseMetricValue(metrics, "yaci_store_adapot_job_last_unsuccessful_epoch");

            boolean jobRunning = inProgressEpoch > 0;
            String status = jobRunning ? "STARTED" : (lastSuccessfulEpoch > 0 ? "COMPLETED" : "NOT_STARTED");
            String error = null;
            Integer errorEpoch = null;

            // If there's an unsuccessful epoch, fetch the actual error message from database
            if (lastUnsuccessfulEpoch > 0 && jdbcTemplate != null) {
                try {
                    error = jdbcTemplate.queryForObject(
                            "SELECT error_message FROM adapot_jobs WHERE epoch = ? AND type = 'REWARD_CALC'",
                            String.class,
                            lastUnsuccessfulEpoch);
                    if (error == null || error.isEmpty()) {
                        error = "Error in epoch " + lastUnsuccessfulEpoch;
                    }
                    errorEpoch = lastUnsuccessfulEpoch;
                    // Mark status as ERROR if there's an error in the most recent unsuccessful epoch
                    if (!jobRunning && lastUnsuccessfulEpoch > lastSuccessfulEpoch) {
                        status = "ERROR";
                    }
                } catch (Exception e) {
                    error = "Error in epoch " + lastUnsuccessfulEpoch;
                    errorEpoch = lastUnsuccessfulEpoch;
                }
            }

            return LedgerStateStatusDto.builder()
                    .currentEpoch(currentEpoch)
                    .lastProcessedEpoch(lastSuccessfulEpoch)
                    .jobRunning(jobRunning)
                    .lastJobStatus(status)
                    .lastJobError(error)
                    .lastErrorEpoch(errorEpoch)
                    .lastJobTimestamp(null)
                    .build();

        } catch (Exception e) {
            log.debug("Could not get ledger state from Prometheus: {}", e.getMessage());
            return null;
        }
    }

    private int parseMetricValue(String metrics, String metricName) {
        // Parse Prometheus format: metric_name{labels} value or metric_name value
        for (String line : metrics.split("\n")) {
            if (line.startsWith(metricName) && !line.startsWith("#")) {
                // Handle both: metric_name value and metric_name{labels} value
                String valuePart = line;
                int braceIndex = line.indexOf('}');
                if (braceIndex > 0) {
                    valuePart = line.substring(braceIndex + 1).trim();
                } else {
                    valuePart = line.substring(metricName.length()).trim();
                }

                try {
                    return (int) Double.parseDouble(valuePart);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
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

            return LedgerStateStatusDto.builder()
                    .currentEpoch(currentEpoch)
                    .lastProcessedEpoch(lastProcessedEpoch != null ? lastProcessedEpoch : 0)
                    .jobRunning(runningCount != null && runningCount > 0)
                    .lastJobStatus(lastJobStatus != null ? lastJobStatus : "NOT_STARTED")
                    .lastJobError(lastJobError)
                    .lastErrorEpoch(lastErrorEpoch)
                    .lastJobTimestamp(null)
                    .build();

        } catch (Exception e) {
            log.debug("Could not get ledger state from database: {}", e.getMessage());
            return null;
        }
    }
}
