package com.bloxbean.cardano.yaci.store.plugin.scheduler.dto;

import com.bloxbean.cardano.yaci.store.plugin.api.config.SchedulerPluginDef.ScheduleType;
import com.bloxbean.cardano.yaci.store.plugin.scheduler.SchedulerService.SchedulerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for scheduler status with detailed statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerStatusResponse {
    private String name;
    private SchedulerStatus status;
    private ScheduleType scheduleType;
    private String scheduleValue;
    private SchedulerStatistics statistics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchedulerStatistics {
        private long totalExecutions;
        private long successfulExecutions;
        private long failedExecutions;
        private Long lastExecutionTime;
        private Long lastExecutionDuration;
        private Double averageExecutionDuration;
        private String lastError;
    }
}
