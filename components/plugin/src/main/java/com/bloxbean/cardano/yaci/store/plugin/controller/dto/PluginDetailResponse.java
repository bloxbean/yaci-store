package com.bloxbean.cardano.yaci.store.plugin.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for detailed plugin metrics and statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDetailResponse {
    private String name;
    private String type;
    private String language;
    private ExecutionMetrics executionMetrics;
    private ItemMetrics itemMetrics;
    private DurationMetrics durationMetrics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionMetrics {
        private long totalExecutions;
        private long successfulExecutions;
        private long failedExecutions;
        private double successRate;
        private String lastExecutionTime;  // ISO 8601 UTC timestamp
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemMetrics {
        private long totalItemsProcessed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DurationMetrics {
        private Long lastDuration;
        private Long minDuration;
        private Long maxDuration;
        private Double averageDuration;
    }
}
