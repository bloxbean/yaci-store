package com.bloxbean.cardano.yaci.store.plugin.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for overview of all plugin types with summary statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginOverviewResponse {
    private int totalPlugins;
    private int activePlugins;
    private PluginTypeStats filters;
    private PluginTypeStats preActions;
    private PluginTypeStats postActions;
    private PluginTypeStats eventHandlers;
    private PluginTypeStats schedulers;
    private PluginTypeStats initPlugins;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PluginTypeStats {
        private int count;
        private long totalExecutions;
        private long totalErrors;
        private Long totalItemsProcessed;   // Nullable - omitted for plugin types that don't process items
    }
}
