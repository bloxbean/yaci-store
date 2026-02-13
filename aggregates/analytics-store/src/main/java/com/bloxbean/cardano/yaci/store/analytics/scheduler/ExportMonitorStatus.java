package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Health status information for the analytics export monitor.
 *
 * Reports the current state of all schedulers (daily export, epoch export,
 * continuous sync) and any stale in-progress exports detected by the export monitor.
 */
@Data
@Builder
public class ExportMonitorStatus {
    private boolean dailyExportRunning;
    private Instant lastDailyExportStart;
    private Instant lastDailyExportEnd;

    private boolean epochExportRunning;
    private Instant lastEpochExportStart;
    private Instant lastEpochExportEnd;

    private boolean continuousSyncRunning;
    private Instant lastSyncStart;
    private Instant lastSyncEnd;

    private int staleInProgressCount;
    private boolean healthy;
    private String unhealthyReason;
}