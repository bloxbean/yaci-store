package com.bloxbean.cardano.yaci.store.plugin.scheduler;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.plugin.api.SchedulerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.config.SchedulerPluginDef;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateConfig;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import com.bloxbean.cardano.yaci.store.plugin.impl.mvel.MvelStorePluginFactory;
import com.bloxbean.cardano.yaci.store.plugin.metrics.PluginMetricsCollector;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for scheduler admin read-only monitoring API
 */
public class SchedulerAdminIntegrationTest {

    private SchedulerService schedulerService;
    private PluginStateService pluginStateService;
    private MvelStorePluginFactory mvelFactory;
    private TaskScheduler taskScheduler;
    private StoreProperties storeProperties;
    private PluginMetricsCollector metricsCollector;

    @BeforeEach
    void setup() {
        // Setup task scheduler with virtual threads
        SimpleAsyncTaskScheduler scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setThreadNamePrefix("test-scheduler-");
        scheduler.setVirtualThreads(true);
        taskScheduler = scheduler;

        // Setup plugin state service
        PluginStateConfig pluginStateConfig = new PluginStateConfig();
        pluginStateService = new PluginStateService(
                pluginStateConfig.globalState(),
                pluginStateConfig.pluginStates()
        );

        // Setup variable provider factory
        VariableProviderFactory variableProviderFactory = new VariableProviderFactory(null);

        storeProperties = new StoreProperties();

        // Setup MVEL factory
        mvelFactory = new MvelStorePluginFactory(pluginStateService, variableProviderFactory);

        // Setup metrics collector (no MeterRegistry for tests)
        metricsCollector = new PluginMetricsCollector(storeProperties, null);

        // Create scheduler service
        schedulerService = new SchedulerService(taskScheduler, storeProperties, metricsCollector);
    }

    @Test
    void testExecutionMetrics() throws InterruptedException {
        // Create a scheduler
        SchedulerPluginDef schedulerDef = new SchedulerPluginDef();
        schedulerDef.setName("test-metrics");
        schedulerDef.setLang("mvel");
        schedulerDef.setInlineScript("""
                counter = state.get('counter');
                if (counter == null) counter = 0;
                counter = counter + 1;
                state.put('counter', counter);

                // Simulate some work
                Thread.sleep(50);
                """);
        schedulerDef.setExitOnError(false);

        SchedulerPluginDef.ScheduleConfig schedule = new SchedulerPluginDef.ScheduleConfig();
        schedule.setType(SchedulerPluginDef.ScheduleType.INTERVAL);
        schedule.setValue("1");
        schedulerDef.setSchedule(schedule);

        SchedulerPlugin<?> plugin = mvelFactory.createSchedulerPlugin(schedulerDef);
        schedulerService.registerScheduler(schedulerDef.getName(), plugin, schedulerDef);

        // Wait for several executions
        Thread.sleep(3500);

        // Get execution info
        Map<String, SchedulerService.SchedulerExecutionInfo> statuses = schedulerService.getSchedulerStatuses();
        SchedulerService.SchedulerExecutionInfo info = statuses.get("test-metrics");

        // Verify metrics
        assertThat(info).isNotNull();
        assertThat(info.getName()).isEqualTo("test-metrics");
        assertThat(info.getExecutionCount()).isGreaterThanOrEqualTo(3);
        assertThat(info.getSuccessCount()).isGreaterThanOrEqualTo(3);
        assertThat(info.getFailureCount()).isEqualTo(0);
        assertThat(info.getLastExecutionTime()).isNotNull();
        assertThat(info.getLastExecutionDuration()).isGreaterThan(40); // At least 50ms sleep
        assertThat(info.getAverageExecutionDuration()).isGreaterThan(40);
        assertThat(info.getScheduleType()).isEqualTo(SchedulerPluginDef.ScheduleType.INTERVAL);
        assertThat(info.getScheduleValue()).isEqualTo("1");

        // Clean up
        schedulerService.cancelScheduler(schedulerDef.getName());
    }
}
