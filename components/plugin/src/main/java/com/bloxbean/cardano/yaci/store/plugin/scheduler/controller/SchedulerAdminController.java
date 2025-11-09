package com.bloxbean.cardano.yaci.store.plugin.scheduler.controller;

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

import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for monitoring scheduler plugins (read-only).
 * <p>
 * Provides read-only endpoints for viewing scheduler status and statistics.
 * Control operations (pause, resume, cancel, trigger) are not supported in this release.
 */
@RestController("SchedulerAdminController")
@RequestMapping("${apiPrefix}/admin/schedulers")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "store.admin.api-enabled", havingValue = "true")
@Tag(name = "Scheduler Admin API", description = "Read-only monitoring API for scheduler plugins")
public class SchedulerAdminController {

    private final SchedulerService schedulerService;

    @GetMapping
    @Operation(description = "List all registered schedulers with status and statistics")
    public SchedulerListResponse listSchedulers() {
        Map<String, SchedulerService.SchedulerExecutionInfo> statuses = schedulerService.getSchedulerStatuses();

        int running = 0, failed = 0, cancelled = 0;
        for (var info : statuses.values()) {
            switch (info.getStatus()) {
                case RUNNING, SCHEDULED, COMPLETED -> running++;
                case FAILED -> failed++;
                case CANCELLED -> cancelled++;
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

    @GetMapping("/{name}")
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
}
