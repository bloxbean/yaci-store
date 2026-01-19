package com.bloxbean.cardano.yaci.store.adminui.service;

import com.bloxbean.cardano.yaci.store.adminui.dto.LedgerStateStatusDto;
import com.bloxbean.cardano.yaci.store.adminui.dto.SyncStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service to retrieve ledger state status using a priority-based approach:
 * 1. First: Try Prometheus/Actuator endpoint (lightweight, no db pressure)
 * 2. Second: Fall back to direct database query
 * 3. Last: Return MODULE_NOT_AVAILABLE if neither works
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerStateStatusService {

    private final SyncStatusService syncStatusService;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Value("${management.endpoints.web.base-path:/actuator}")
    private String actuatorBasePath;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    @Value("${server.port:8080}")
    private int serverPort;

    public LedgerStateStatusDto getLedgerStateStatus() {
        SyncStatusDto syncStatus = syncStatusService.getSyncStatus();
        int currentEpoch = syncStatus.getEpoch();

        // Try Prometheus metrics first (lightweight, no db pressure)
        LedgerStateStatusDto prometheusResult = tryGetFromPrometheus(currentEpoch);
        if (prometheusResult != null) {
            return prometheusResult;
        }

        // Fall back to direct database query
        LedgerStateStatusDto dbResult = tryGetFromDatabase(currentEpoch);
        if (dbResult != null) {
            return dbResult;
        }

        // Module not available
        return LedgerStateStatusDto.builder()
                .currentEpoch(currentEpoch)
                .lastProcessedEpoch(0)
                .jobRunning(false)
                .lastJobStatus("MODULE_NOT_AVAILABLE")
                .lastJobError(null)
                .lastJobTimestamp(null)
                .build();
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
            String error = lastUnsuccessfulEpoch > 0 ? "Error in epoch " + lastUnsuccessfulEpoch : null;

            return LedgerStateStatusDto.builder()
                    .currentEpoch(currentEpoch)
                    .lastProcessedEpoch(lastSuccessfulEpoch)
                    .jobRunning(jobRunning)
                    .lastJobStatus(status)
                    .lastJobError(error)
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

            // Check for running jobs
            Integer runningCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM adapot_jobs WHERE status = 'STARTED' AND type = 'REWARD_CALC'",
                    Integer.class);

            // Get last job status - handle empty result
            String lastJobStatus;
            try {
                lastJobStatus = jdbcTemplate.queryForObject(
                        "SELECT status FROM adapot_jobs WHERE type = 'REWARD_CALC' ORDER BY epoch DESC LIMIT 1",
                        String.class);
            } catch (Exception e) {
                lastJobStatus = "NOT_STARTED";
            }

            return LedgerStateStatusDto.builder()
                    .currentEpoch(currentEpoch)
                    .lastProcessedEpoch(lastProcessedEpoch != null ? lastProcessedEpoch : 0)
                    .jobRunning(runningCount != null && runningCount > 0)
                    .lastJobStatus(lastJobStatus != null ? lastJobStatus : "NOT_STARTED")
                    .lastJobError(null)
                    .lastJobTimestamp(null)
                    .build();

        } catch (Exception e) {
            log.debug("Could not get ledger state from database: {}", e.getMessage());
            return null;
        }
    }
}
