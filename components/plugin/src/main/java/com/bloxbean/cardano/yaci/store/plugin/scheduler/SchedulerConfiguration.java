package com.bloxbean.cardano.yaci.store.plugin.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for scheduler plugin support.
 * Creates the TaskScheduler bean needed for scheduling plugins.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "store.plugins.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerConfiguration {
    
    @Bean(name = "pluginTaskScheduler")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);  // Adjust based on expected concurrent schedulers
        scheduler.setThreadNamePrefix("plugin-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        return scheduler;
    }
}