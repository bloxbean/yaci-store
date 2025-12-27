package com.bloxbean.cardano.yaci.store.plugin.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for listing schedulers with summary information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerListResponse {
    private int totalSchedulers;
    private int runningSchedulers;
    private int failedSchedulers;
    private List<SchedulerSummary> schedulers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchedulerSummary {
        private String name;
        private String status;
        private String scheduleType;
        private String scheduleValue;
        private long totalExecutions;
        private Long lastExecutionTime;
    }
}
