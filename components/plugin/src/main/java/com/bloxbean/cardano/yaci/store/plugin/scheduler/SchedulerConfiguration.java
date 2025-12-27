package com.bloxbean.cardano.yaci.store.plugin.scheduler;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

/**
 * Configuration for scheduler plugin support.
 * Creates the TaskScheduler bean needed for scheduling plugins.
 * Uses virtual threads for better scalability and resource utilization.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "store.plugins.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class SchedulerConfiguration {

    private final StoreProperties storeProperties;

    @Bean(name = "pluginTaskScheduler")
    public TaskScheduler taskScheduler() {
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setThreadNamePrefix("plugin-scheduler-");
        scheduler.setVirtualThreads(true);  // Use virtual threads for I/O-bound scheduler tasks

        int timeoutSeconds = storeProperties.getSchedulerTaskTerminationTimeoutSeconds();
        scheduler.setTaskTerminationTimeout(timeoutSeconds * 1000L);  // Convert to milliseconds

        return scheduler;
    }
}
