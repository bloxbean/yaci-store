package com.bloxbean.cardano.yaci.store.plugin.controller;

import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.controller.dto.PluginDetailResponse;
import com.bloxbean.cardano.yaci.store.plugin.controller.dto.PluginListResponse;
import com.bloxbean.cardano.yaci.store.plugin.controller.dto.PluginOverviewResponse;
import com.bloxbean.cardano.yaci.store.plugin.metrics.PluginExecutionMetrics;
import com.bloxbean.cardano.yaci.store.plugin.metrics.PluginMetricsCollector;
import com.bloxbean.cardano.yaci.store.plugin.scheduler.SchedulerService;
import com.bloxbean.cardano.yaci.store.plugin.scheduler.dto.SchedulerListResponse;
import com.bloxbean.cardano.yaci.store.plugin.scheduler.dto.SchedulerStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for monitoring all plugin types (read-only).
 * <p>
 * Provides read-only endpoints for viewing plugin status, statistics, and metrics
 * across all plugin types: FILTER, PRE_ACTION, POST_ACTION, EVENT_HANDLER, SCHEDULER, INIT.
 * <p>
 * Control operations are not supported in this release.
 */
@RestController("PluginController")
@RequestMapping("${apiPrefix}/admin/plugins")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "store.plugins.api-enabled",
        havingValue = "true",
        matchIfMissing = true)
@Tag(name = "Plugin API", description = "Read-only monitoring API for all plugin types")
public class PluginController {

    private final PluginMetricsCollector metricsCollector;
    private final SchedulerService schedulerService;

    // ========== Overview Endpoint ==========

    @GetMapping
    @Operation(description = "Get overview of all plugin types with summary statistics")
    public PluginOverviewResponse getOverview() {
        Map<String, PluginExecutionMetrics> allMetrics = metricsCollector.getAllMetrics();

        return PluginOverviewResponse.builder()
                .totalPlugins(allMetrics.size())
                .activePlugins((int) allMetrics.values().stream()
                        .filter(m -> m.getExecutionCount().get() > 0)
                        .count())
                .filters(buildPluginTypeStats(PluginType.FILTER))
                .preActions(buildPluginTypeStats(PluginType.PRE_ACTION))
                .postActions(buildPluginTypeStats(PluginType.POST_ACTION))
                .eventHandlers(buildPluginTypeStats(PluginType.EVENT_HANDLER))
                .schedulers(buildPluginTypeStats(PluginType.SCHEDULER))
                .initPlugins(buildPluginTypeStats(PluginType.INIT))
                .build();
    }

    // ========== Filter Endpoints ==========

    @GetMapping("/filters")
    @Operation(description = "List all filter plugins with execution metrics")
    public PluginListResponse listFilters() {
        return buildPluginListResponse(PluginType.FILTER);
    }

    @GetMapping("/filters/{name}")
    @Operation(description = "Get detailed metrics for a specific filter plugin")
    public ResponseEntity<PluginDetailResponse> getFilterDetail(@PathVariable String name) {
        return getPluginDetail(name, PluginType.FILTER);
    }

    // ========== PreAction Endpoints ==========

    @GetMapping("/pre-actions")
    @Operation(description = "List all pre-action plugins with execution metrics")
    public PluginListResponse listPreActions() {
        return buildPluginListResponse(PluginType.PRE_ACTION);
    }

    @GetMapping("/pre-actions/{name}")
    @Operation(description = "Get detailed metrics for a specific pre-action plugin")
    public ResponseEntity<PluginDetailResponse> getPreActionDetail(@PathVariable String name) {
        return getPluginDetail(name, PluginType.PRE_ACTION);
    }

    // ========== PostAction Endpoints ==========

    @GetMapping("/post-actions")
    @Operation(description = "List all post-action plugins with execution metrics")
    public PluginListResponse listPostActions() {
        return buildPluginListResponse(PluginType.POST_ACTION);
    }

    @GetMapping("/post-actions/{name}")
    @Operation(description = "Get detailed metrics for a specific post-action plugin")
    public ResponseEntity<PluginDetailResponse> getPostActionDetail(@PathVariable String name) {
        return getPluginDetail(name, PluginType.POST_ACTION);
    }

    // ========== EventHandler Endpoints ==========

    @GetMapping("/event-handlers")
    @Operation(description = "List all event handler plugins with execution metrics")
    public PluginListResponse listEventHandlers() {
        return buildPluginListResponse(PluginType.EVENT_HANDLER);
    }

    @GetMapping("/event-handlers/{name}")
    @Operation(description = "Get detailed metrics for a specific event handler plugin")
    public ResponseEntity<PluginDetailResponse> getEventHandlerDetail(@PathVariable String name) {
        return getPluginDetail(name, PluginType.EVENT_HANDLER);
    }

    // ========== Scheduler Endpoints (delegate to SchedulerService) ==========

    @GetMapping("/schedulers")
    @Operation(description = "List all scheduler plugins with status and statistics")
    public SchedulerListResponse listSchedulers() {
        Map<String, SchedulerService.SchedulerExecutionInfo> statuses = schedulerService.getSchedulerStatuses();

        int running = 0, failed = 0;
        for (var info : statuses.values()) {
            switch (info.getStatus()) {
                case RUNNING, SCHEDULED, COMPLETED -> running++;
                case FAILED -> failed++;
            }
        }

        var schedulers = statuses.values().stream()
                .map(info -> SchedulerListResponse.SchedulerSummary.builder()
                        .name(info.getName())
                        .status(info.getStatus().name())
                        .scheduleType(info.getScheduleType() != null ? info.getScheduleType().name() : "UNKNOWN")
                        .scheduleValue(info.getScheduleValue())
                        .totalExecutions(info.getExecutionCount())
                        .lastExecutionTime(info.getLastExecutionTime())
                        .build())
                .collect(Collectors.toList());

        return SchedulerListResponse.builder()
                .totalSchedulers(statuses.size())
                .runningSchedulers(running)
                .failedSchedulers(failed)
                .schedulers(schedulers)
                .build();
    }

    @GetMapping("/schedulers/{name}")
    @Operation(description = "Get detailed status and statistics for a specific scheduler")
    public ResponseEntity<SchedulerStatusResponse> getSchedulerStatus(@PathVariable String name) {
        var statuses = schedulerService.getSchedulerStatuses();
        var info = statuses.get(name);

        if (info == null) {
            return ResponseEntity.notFound().build();
        }

        var stats = SchedulerStatusResponse.SchedulerStatistics.builder()
                .totalExecutions(info.getExecutionCount())
                .successfulExecutions(info.getSuccessCount())
                .failedExecutions(info.getFailureCount())
                .lastExecutionTime(info.getLastExecutionTime())
                .lastExecutionDuration(info.getLastExecutionDuration())
                .averageExecutionDuration(info.getAverageExecutionDuration())
                .lastError(info.getLastError())
                .build();

        var response = SchedulerStatusResponse.builder()
                .name(info.getName())
                .status(info.getStatus())
                .scheduleType(info.getScheduleType())
                .scheduleValue(info.getScheduleValue())
                .statistics(stats)
                .build();

        return ResponseEntity.ok(response);
    }

    // ========== Helper Methods ==========

    private PluginOverviewResponse.PluginTypeStats buildPluginTypeStats(PluginType type) {
        Map<String, PluginExecutionMetrics> metrics = metricsCollector.getMetricsByType(type);

        long totalExecutions = metrics.values().stream()
                .mapToLong(m -> m.getExecutionCount().get())
                .sum();

        long totalErrors = metrics.values().stream()
                .mapToLong(m -> m.getErrorCount().get())
                .sum();

        // Only calculate items for types that process items in batches
        // FILTER, PRE_ACTION, POST_ACTION, EVENT_HANDLER process items
        // SCHEDULER and INIT do not process items - set to null
        Long totalItemsProcessed = null;

        if (type == PluginType.FILTER || type == PluginType.PRE_ACTION ||
            type == PluginType.POST_ACTION || type == PluginType.EVENT_HANDLER) {

            long processedSum = metrics.values().stream()
                    .mapToLong(PluginExecutionMetrics::getTotalItemsProcessed)
                    .sum();

            // Only set if there's actual data (avoid showing 0 as null would be cleaner)
            totalItemsProcessed = processedSum > 0 ? processedSum : null;
        }

        return PluginOverviewResponse.PluginTypeStats.builder()
                .count(metrics.size())
                .totalExecutions(totalExecutions)
                .totalErrors(totalErrors)
                .totalItemsProcessed(totalItemsProcessed)
                .build();
    }

    private PluginListResponse buildPluginListResponse(PluginType type) {
        Map<String, PluginExecutionMetrics> metrics = metricsCollector.getMetricsByType(type);

        long totalExecutions = metricsCollector.getTotalExecutionsByType(type);
        long totalErrors = metricsCollector.getTotalErrorsByType(type);

        var pluginSummaries = metrics.values().stream()
                .map(m -> PluginListResponse.PluginSummary.builder()
                        .name(m.getPluginName())
                        .language(m.getLanguage())
                        .executionCount(m.getExecutionCount().get())
                        .successCount(m.getSuccessCount().get())
                        .errorCount(m.getErrorCount().get())
                        .lastExecutionTime(m.getLastExecutionTimeMillis().get() > 0
                                ? Instant.ofEpochMilli(m.getLastExecutionTimeMillis().get()).toString()
                                : null)
                        .lastDuration(m.getExecutionCount().get() > 0
                                ? m.getLastDurationMillis().get() : null)
                        .itemsProcessed(m.getTotalItemsProcessed() > 0
                                ? m.getTotalItemsProcessed() : null)
                        .build())
                .collect(Collectors.toList());

        return PluginListResponse.builder()
                .pluginType(type.name())
                .totalPlugins(metrics.size())
                .totalExecutions(totalExecutions)
                .totalErrors(totalErrors)
                .plugins(pluginSummaries)
                .build();
    }

    private ResponseEntity<PluginDetailResponse> getPluginDetail(String name, PluginType expectedType) {
        return metricsCollector.getMetrics(name)
                .filter(m -> m.getPluginType() == expectedType)
                .map(m -> {
                    var executionMetrics = PluginDetailResponse.ExecutionMetrics.builder()
                            .totalExecutions(m.getExecutionCount().get())
                            .successfulExecutions(m.getSuccessCount().get())
                            .failedExecutions(m.getErrorCount().get())
                            .successRate(m.getSuccessRate())
                            .lastExecutionTime(m.getLastExecutionTimeMillis().get() > 0
                                    ? Instant.ofEpochMilli(m.getLastExecutionTimeMillis().get()).toString()
                                    : null)
                            .build();

                    var itemMetrics = PluginDetailResponse.ItemMetrics.builder()
                            .totalItemsProcessed(m.getTotalItemsProcessed())
                            .build();

                    var durationMetrics = PluginDetailResponse.DurationMetrics.builder()
                            .lastDuration(m.getExecutionCount().get() > 0
                                    ? m.getLastDurationMillis().get() : null)
                            .minDuration(m.getMinDuration() > 0 ? m.getMinDuration() : null)
                            .maxDuration(m.getMaxDuration() > 0 ? m.getMaxDuration() : null)
                            .averageDuration(m.getAverageDuration() > 0 ? m.getAverageDuration() : null)
                            .build();

                    var response = PluginDetailResponse.builder()
                            .name(m.getPluginName())
                            .type(m.getPluginType().name())
                            .language(m.getLanguage())
                            .executionMetrics(executionMetrics)
                            .itemMetrics(itemMetrics)
                            .durationMetrics(durationMetrics)
                            .build();

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
