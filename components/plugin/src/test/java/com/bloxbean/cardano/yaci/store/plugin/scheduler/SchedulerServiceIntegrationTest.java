package com.bloxbean.cardano.yaci.store.plugin.scheduler;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.plugin.api.SchedulerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.config.SchedulerPluginDef;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateConfig;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import com.bloxbean.cardano.yaci.store.plugin.impl.mvel.MvelStorePluginFactory;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for SchedulerService with actual scheduling
 */
public class SchedulerServiceIntegrationTest {

    private SchedulerService schedulerService;
    private PluginStateService pluginStateService;
    private MvelStorePluginFactory mvelFactory;
    private TaskScheduler taskScheduler;
    private StoreProperties storeProperties;

    @BeforeEach
    void setup() {
        // Setup task scheduler with virtual threads (matching production config)
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

        // Setup variable provider factory (can be null for basic test)
        VariableProviderFactory variableProviderFactory = new VariableProviderFactory(null);

        storeProperties = new StoreProperties();

        // Setup MVEL factory
        mvelFactory = new MvelStorePluginFactory(pluginStateService, variableProviderFactory);

        // Create scheduler service
        schedulerService = new SchedulerService(taskScheduler, storeProperties);
    }

    @Test
    void testSchedulerExecutesAtInterval() throws InterruptedException {
        // Create a scheduler plugin definition
        SchedulerPluginDef schedulerDef = new SchedulerPluginDef();
        schedulerDef.setName("test-interval-scheduler");
        schedulerDef.setLang("mvel");
        schedulerDef.setInlineScript("""
            // Increment counter on each execution
            counter = state.get('counter');
            if (counter == null) {
                counter = 0;
            }
            counter = counter + 1;
            state.put('counter', counter);
            state.put('lastExecution', executionTime);
            logger.info("Scheduler executed, counter: " + counter);
            """);
        schedulerDef.setExitOnError(false);

        // Set schedule for every 1 second
        SchedulerPluginDef.ScheduleConfig schedule = new SchedulerPluginDef.ScheduleConfig();
        schedule.setType(SchedulerPluginDef.ScheduleType.INTERVAL);
        schedule.setValue("1"); // 1 second interval
        schedulerDef.setSchedule(schedule);

        // Create the plugin
        SchedulerPlugin<?> plugin = mvelFactory.createSchedulerPlugin(schedulerDef);

        // Register with scheduler service
        schedulerService.registerScheduler(schedulerDef.getName(), plugin, schedulerDef);

        // Wait for scheduler to execute at least 3 times
        Thread.sleep(3500); // Wait 3.5 seconds

        // Check that the scheduler has executed multiple times
        var pluginState = pluginStateService.forPlugin("test-interval-scheduler");
        Integer counter = (Integer) pluginState.get("counter");
        assertThat(counter).isNotNull();
        assertThat(counter).isGreaterThanOrEqualTo(3);

        // Check last execution time was set
        Long lastExecution = (Long) pluginState.get("lastExecution");
        assertThat(lastExecution).isNotNull();
        assertThat(lastExecution).isGreaterThan(0);

        // Clean up
        schedulerService.cancelScheduler(schedulerDef.getName());
    }

    @Test
    void testSchedulerWithCustomVariables() throws InterruptedException {
        // Create a scheduler with custom variables
        SchedulerPluginDef schedulerDef = new SchedulerPluginDef();
        schedulerDef.setName("test-custom-vars-scheduler");
        schedulerDef.setLang("mvel");
        schedulerDef.setInlineScript("""
            // Access custom variables
            multiplier = customMultiplier;
            if (multiplier == null) {
                multiplier = 1;
            }
            
            value = state.get('value');
            if (value == null) {
                value = 1;
            }
            value = value * multiplier;
            state.put('value', value);
            state.put('customGreeting', customGreeting);
            logger.info("Value updated to: " + value);
            """);
        schedulerDef.setExitOnError(false);

        // Set custom variables
        Map<String, Object> customVars = new HashMap<>();
        customVars.put("customMultiplier", 2);
        customVars.put("customGreeting", "Hello from scheduler!");
        schedulerDef.setCustomVariables(customVars);

        // Set schedule
        SchedulerPluginDef.ScheduleConfig schedule = new SchedulerPluginDef.ScheduleConfig();
        schedule.setType(SchedulerPluginDef.ScheduleType.INTERVAL);
        schedule.setValue("1");
        schedulerDef.setSchedule(schedule);

        // Create and register plugin
        SchedulerPlugin<?> plugin = mvelFactory.createSchedulerPlugin(schedulerDef);
        schedulerService.registerScheduler(schedulerDef.getName(), plugin, schedulerDef);

        // Wait for execution
        Thread.sleep(2500); // Wait 2.5 seconds for at least 2 executions

        // Verify custom variables were used
        var pluginState = pluginStateService.forPlugin("test-custom-vars-scheduler");
        Integer value = (Integer) pluginState.get("value");
        assertThat(value).isNotNull();
        assertThat(value).isGreaterThanOrEqualTo(2); // Should be at least 2 after first execution

        String greeting = (String) pluginState.get("customGreeting");
        assertThat(greeting).isEqualTo("Hello from scheduler!");

        // Clean up
        schedulerService.cancelScheduler(schedulerDef.getName());
    }

//    @Test
    void testSchedulerCancellation() throws InterruptedException {
        // Create a scheduler
        SchedulerPluginDef schedulerDef = new SchedulerPluginDef();
        schedulerDef.setName("test-cancel-scheduler");
        schedulerDef.setLang("mvel");
        schedulerDef.setInlineScript("""
            executions = state.get('executions');
            if (executions == null) {
                executions = 0;
            }
            executions = executions + 1;
            state.put('executions', executions);
            """);
        schedulerDef.setExitOnError(false);

        SchedulerPluginDef.ScheduleConfig schedule = new SchedulerPluginDef.ScheduleConfig();
        schedule.setType(SchedulerPluginDef.ScheduleType.INTERVAL);
        schedule.setValue("1");
        schedulerDef.setSchedule(schedule);

        SchedulerPlugin<?> plugin = mvelFactory.createSchedulerPlugin(schedulerDef);
        schedulerService.registerScheduler(schedulerDef.getName(), plugin, schedulerDef);

        // Let it run for 2 seconds
        Thread.sleep(2000);

        // Cancel the scheduler
        schedulerService.cancelScheduler(schedulerDef.getName());

        // Get execution count at cancellation
        var pluginState = pluginStateService.forPlugin("test-cancel-scheduler");
        Integer executionsAtCancel = (Integer) pluginState.get("executions");

        // Wait another 2 seconds
        Thread.sleep(2000);

        // Verify execution count hasn't increased
        Integer executionsAfterWait = (Integer) pluginState.get("executions");
        assertThat(executionsAfterWait).isEqualTo(executionsAtCancel);

        // Verify status
        Map<String, SchedulerService.SchedulerExecutionInfo> statuses = schedulerService.getSchedulerStatuses();
        SchedulerService.SchedulerExecutionInfo info = statuses.get("test-cancel-scheduler");
        assertThat(info).isNotNull();
        assertThat(info.getStatus()).isEqualTo(SchedulerService.SchedulerStatus.CANCELLED);
    }

    @Test
    void testCronScheduler() throws InterruptedException {
        // Create scheduler with cron expression (every 2 seconds)
        SchedulerPluginDef schedulerDef = new SchedulerPluginDef();
        schedulerDef.setName("test-cron-scheduler");
        schedulerDef.setLang("mvel");
        schedulerDef.setInlineScript("""
            counter = state.get('counter');
            if (counter == null) {
                counter = 0;
            }
            counter = counter + 1;
            state.put('counter', counter);
            state.put('lastExecution', executionTime);
            logger.info("CRON scheduler executed, counter: " + counter);
            """);
        schedulerDef.setExitOnError(false);

        // Cron: every 2 seconds (*/2 * * * * ?)
        SchedulerPluginDef.ScheduleConfig schedule = new SchedulerPluginDef.ScheduleConfig();
        schedule.setType(SchedulerPluginDef.ScheduleType.CRON);
        schedule.setValue("*/2 * * * * ?");
        schedulerDef.setSchedule(schedule);

        SchedulerPlugin<?> plugin = mvelFactory.createSchedulerPlugin(schedulerDef);
        schedulerService.registerScheduler(schedulerDef.getName(), plugin, schedulerDef);

        // Wait for at least 2 executions (5 seconds should give us 2-3 executions)
        Thread.sleep(5000);

        var pluginState = pluginStateService.forPlugin("test-cron-scheduler");
        Integer counter = (Integer) pluginState.get("counter");
        assertThat(counter).isNotNull();
        assertThat(counter).isGreaterThanOrEqualTo(2);

        Long lastExecution = (Long) pluginState.get("lastExecution");
        assertThat(lastExecution).isNotNull();
        assertThat(lastExecution).isGreaterThan(0);

        // Clean up
        schedulerService.cancelScheduler(schedulerDef.getName());
    }

    @Test
    void testInvalidCronExpression() {
        SchedulerPluginDef schedulerDef = new SchedulerPluginDef();
        schedulerDef.setName("test-invalid-cron");
        schedulerDef.setLang("mvel");
        schedulerDef.setInlineScript("state.put('test', true);");
        schedulerDef.setExitOnError(false);

        SchedulerPluginDef.ScheduleConfig schedule = new SchedulerPluginDef.ScheduleConfig();
        schedule.setType(SchedulerPluginDef.ScheduleType.CRON);
        schedule.setValue("INVALID CRON");
        schedulerDef.setSchedule(schedule);

        SchedulerPlugin<?> plugin = mvelFactory.createSchedulerPlugin(schedulerDef);

        assertThatThrownBy(() ->
                schedulerService.registerScheduler(schedulerDef.getName(), plugin, schedulerDef))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid cron expression");
    }

    // TODO: Add error handling test - requires better MVEL error simulation
}
