package com.bloxbean.cardano.yaci.store.plugin.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for listing plugins of a specific type with summary information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginListResponse {
    private String pluginType;
    private int totalPlugins;
    private long totalExecutions;
    private long totalErrors;
    private List<PluginSummary> plugins;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PluginSummary {
        private String name;
        private String language;
        private long executionCount;
        private long successCount;
        private long errorCount;
        private String lastExecutionTime;  // ISO 8601 UTC timestamp
        private Long lastDuration;
        private Long itemsProcessed;
    }
}
