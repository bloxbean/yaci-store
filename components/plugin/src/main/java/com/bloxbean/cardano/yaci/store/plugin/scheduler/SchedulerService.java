package com.bloxbean.cardano.yaci.store.plugin.scheduler;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.plugin.api.SchedulerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.config.SchedulerPluginDef;
import jakarta.annotation.PreDestroy;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Service responsible for managing and executing scheduler plugins.
 * Supports INTERVAL and CRON scheduling.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final TaskScheduler taskScheduler;
    private final StoreProperties storeProperties;

    // Track scheduled tasks
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, SchedulerPlugin<?>> schedulerPlugins = new ConcurrentHashMap<>();
    private final Map<String, SchedulerExecutionInfo> executionInfo = new ConcurrentHashMap<>();

    /**
     * Register a scheduler plugin with the service
     */
    public void registerScheduler(String name, SchedulerPlugin<?> plugin, PluginDef pluginDef) {
        if (!(pluginDef instanceof SchedulerPluginDef)) {
            log.warn("Plugin definition is not a SchedulerPluginDef, skipping: {}", name);
            return;
        }

        SchedulerPluginDef schedulerDef = (SchedulerPluginDef) pluginDef;
        schedulerPlugins.put(name, plugin);

        try {
            schedulePlugin(name, plugin, schedulerDef);
            log.info("Scheduled plugin: {} with schedule: {}", name, schedulerDef.getSchedule());
        } catch (IllegalArgumentException e) {
            // Re-throw validation errors (e.g., invalid cron expression)
            log.error("Failed to schedule plugin due to invalid configuration: {}", name, e);
            throw e;
        } catch (Exception e) {
            log.error("Failed to schedule plugin: {}", name, e);
            handleSchedulerError(name, e, plugin.getPluginDef().getExitOnError());
        }
    }

    /**
     * Schedule a plugin based on its configuration
     */
    private void schedulePlugin(String pluginName, SchedulerPlugin<?> plugin, SchedulerPluginDef pluginDef) {
        SchedulerPluginDef.ScheduleConfig schedule = pluginDef.getSchedule();
        if (schedule == null) {
            log.warn("No schedule configuration found for plugin: {}", pluginName);
            return;
        }

        Runnable task = createSchedulerTask(pluginName, plugin, pluginDef);

        ScheduledFuture<?> future;
        switch (schedule.getType()) {
            case INTERVAL:
                long intervalSeconds = Long.parseLong(schedule.getValue());
                future = taskScheduler.scheduleAtFixedRate(task, Duration.ofSeconds(intervalSeconds));
                log.info("Scheduled {} with interval of {} seconds", pluginName, intervalSeconds);
                break;

            case CRON:
                String cronExpression = schedule.getValue();
                try {
                    CronTrigger cronTrigger = new CronTrigger(cronExpression);
                    future = taskScheduler.schedule(task, cronTrigger);
                    log.info("Scheduled {} with cron expression: {}", pluginName, cronExpression);
                } catch (IllegalArgumentException e) {
                    log.error("Invalid cron expression '{}' for plugin: {}", cronExpression, pluginName, e);
                    throw new IllegalArgumentException("Invalid cron expression: " + cronExpression, e);
                }
                break;

            default:
                throw new IllegalArgumentException("Unsupported schedule type: " + schedule.getType());
        }

        scheduledTasks.put(pluginName, future);
        SchedulerExecutionInfo info = new SchedulerExecutionInfo(pluginName);
        info.setScheduleType(schedule.getType());
        info.setScheduleValue(schedule.getValue());
        executionInfo.put(pluginName, info);
    }

    /**
     * Create a runnable task for the scheduler
     */
    private Runnable createSchedulerTask(String pluginName, SchedulerPlugin<?> plugin, SchedulerPluginDef pluginDef) {
        return () -> {
            try {
                executeScheduler(pluginName, plugin, pluginDef);
            } catch (Exception e) {
                log.error("Error executing scheduler plugin: {}", pluginName, e);
                handleSchedulerError(pluginName, e, pluginDef.getExitOnError());
            }
        };
    }

    /**
     * Execute a scheduler plugin
     */
    private void executeScheduler(String pluginName, SchedulerPlugin<?> plugin, SchedulerPluginDef pluginDef) {
        long startTime = System.currentTimeMillis();

        try {
            // Update execution info
            SchedulerExecutionInfo info = executionInfo.get(pluginName);
            if (info != null) {
                info.incrementExecutionCount();
                info.setLastExecutionTime(startTime);
                info.setStatus(SchedulerStatus.RUNNING);
            }

            // Prepare variables for injection
            Map<String, Object> variables = prepareSchedulerVariables(pluginName, pluginDef, startTime);

            // Set variables in thread-local context for the plugin to access
            injectVariables(variables);

            try {
                // Execute plugin (no parameters - variables are injected)
                plugin.execute();

                long duration = System.currentTimeMillis() - startTime;

                if (info != null) {
                    info.setStatus(SchedulerStatus.COMPLETED);
                    info.setLastExecutionDuration(duration);
                    info.updateAverageExecutionDuration(duration);
                    info.incrementSuccessCount();
                }

                log.debug("Successfully executed scheduler plugin: {} in {}ms", pluginName, duration);

            } finally {
                // Clear injected variables
                clearInjectedVariables();
            }

        } catch (Exception e) {
            SchedulerExecutionInfo info = executionInfo.get(pluginName);
            if (info != null) {
                info.setStatus(SchedulerStatus.FAILED);
                info.setLastError(e.getMessage());
                info.incrementFailureCount();
            }
            throw e;
        }
    }

    /**
     * Prepare variables for scheduler execution
     */
    private Map<String, Object> prepareSchedulerVariables(String pluginName, SchedulerPluginDef pluginDef, long executionTime) {
        Map<String, Object> variables = new HashMap<>();

        // Scheduler-specific variables
        variables.put("executionTime", executionTime);

        // TODO: Add current block info when blockchain integration is available
        variables.put("currentBlock", 0L);  // Placeholder
        variables.put("currentBlockHash", "");  // Placeholder

        // Execution history
        SchedulerExecutionInfo info = executionInfo.get(pluginName);
        if (info != null) {
            variables.put("executionCount", info.getExecutionCount());
            variables.put("lastExecutionTime", info.getLastExecutionTime());
        }

        // Custom variables from configuration
        if (pluginDef.getCustomVariables() != null) {
            variables.putAll(pluginDef.getCustomVariables());
        }

        return variables;
    }

    /**
     * Inject variables into thread-local context
     */
    private void injectVariables(Map<String, Object> variables) {
        // This will be properly implemented with variable provider factory
        // For MVP, we'll use a simple approach
        SchedulerVariableContext.setVariables(variables);
    }

    /**
     * Clear injected variables from thread-local context
     */
    private void clearInjectedVariables() {
        SchedulerVariableContext.clearVariables();
    }

    /**
     * Handle scheduler execution errors
     */
    private void handleSchedulerError(String pluginName, Exception error, Boolean exitOnError) {
        log.error("Scheduler plugin {} failed: {}", pluginName, error.getMessage());

        // Update error info
        SchedulerExecutionInfo info = executionInfo.get(pluginName);
        if (info != null) {
            info.setLastError(error.getMessage());
            info.setStatus(SchedulerStatus.FAILED);
        }

        boolean shouldExitOnError = exitOnError != null
                ? exitOnError
                : storeProperties.isPluginExitOnError();

        if (shouldExitOnError) {
            // Cancel the scheduler
            cancelScheduler(pluginName);
            log.warn("Scheduler plugin {} has been cancelled due to exitOnError=true", pluginName);
        }
    }

    /**
     * Cancel a scheduled task and remove it from tracking
     * (executionInfo is preserved for historical purposes)
     */
    public void cancelScheduler(String pluginName) {
        ScheduledFuture<?> future = scheduledTasks.get(pluginName);
        if (future != null) {
            future.cancel(false);
            scheduledTasks.remove(pluginName);
            // Remove from plugins map to prevent memory leak
            schedulerPlugins.remove(pluginName);

            // Keep executionInfo for historical purposes but mark as CANCELLED
            SchedulerExecutionInfo info = executionInfo.get(pluginName);
            if (info != null) {
                info.setStatus(SchedulerStatus.CANCELLED);
            }

            log.info("Cancelled scheduler: {} (removed from active schedulers)", pluginName);
        }
    }

    /**
     * Get scheduler status information
     */
    public Map<String, SchedulerExecutionInfo> getSchedulerStatuses() {
        return new HashMap<>(executionInfo);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down scheduler service, cancelling all scheduled tasks");
        for (String pluginName : scheduledTasks.keySet()) {
            cancelScheduler(pluginName);
        }
    }


    /**
     * Information about scheduler execution
     */
    @Data
    public static class SchedulerExecutionInfo {
        private final String name;
        private long executionCount = 0;
        private long successCount = 0;
        private long failureCount = 0;
        private Long lastExecutionTime;
        private Long lastExecutionDuration;
        private Double averageExecutionDuration;
        private String lastError;
        private SchedulerStatus status = SchedulerStatus.SCHEDULED;
        private SchedulerPluginDef.ScheduleType scheduleType;
        private String scheduleValue;

        public SchedulerExecutionInfo(String name) {
            this.name = name;
        }

        public synchronized void incrementExecutionCount() {
            this.executionCount++;
        }

        public synchronized void incrementSuccessCount() {
            this.successCount++;
        }

        public synchronized void incrementFailureCount() {
            this.failureCount++;
        }

        public synchronized void updateAverageExecutionDuration(long duration) {
            if (averageExecutionDuration == null) {
                averageExecutionDuration = (double) duration;
            } else {
                // Rolling average: (old_avg * (n-1) + new_value) / n
                averageExecutionDuration = (averageExecutionDuration * (executionCount - 1) + duration) / executionCount;
            }
        }
    }

    /**
     * Scheduler status enum
     */
    public enum SchedulerStatus {
        SCHEDULED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }
}
