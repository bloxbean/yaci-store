package com.bloxbean.cardano.yaci.store.plugin.polyglot.python;

import com.bloxbean.cardano.yaci.store.plugin.api.SchedulerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.config.SchedulerPluginDef;
import com.bloxbean.cardano.yaci.store.plugin.polyglot.BasePluginTest;
import com.bloxbean.cardano.yaci.store.plugin.scheduler.SchedulerVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for Python scheduler plugins
 */
public class PythonSchedulerPluginIntegrationTest extends BasePluginTest {
    
    private PythonPolyglotPluginFactory pythonFactory;
    
    @BeforeEach
    void setupPython() {
        // Setup Python factory using the base class setup
        pythonFactory = new PythonPolyglotPluginFactory(
            pluginContextUtil,
            pluginCacheService,
            variableProviderFactory,
            contextProvider,
            globalScriptContextRegistry
        );
    }
    
    @Test
    void testPythonSchedulerPluginExecution() {
        // Create a Python scheduler plugin definition
        SchedulerPluginDef schedulerDef = new SchedulerPluginDef();
        schedulerDef.setName("test-python-scheduler");
        schedulerDef.setLang("python");
        schedulerDef.setInlineScript("""
            # Access scheduler variables directly (no function wrapper needed)
            counter = state.get('pyCounter') or 0
            counter = counter + 1
            state.put('pyCounter', counter)
            state.put('lastPyExecution', executionTime)
            state.put('pyExecutionCount', executionCount)
            
            # Use custom variables if available
            if 'pythonMessage' in globals():
                state.put('pythonMessage', pythonMessage)
            
            # Store metrics - need to unwrap the dictionary before storing
            metrics = {
                'counter': counter,
                'execution_time': executionTime,
                'execution_count': executionCount
            }
            state.put('metrics', unwrapper.unwrap(metrics))
            """);
        schedulerDef.setExitOnError(false);
        
        // Set custom variables
        Map<String, Object> customVars = new HashMap<>();
        customVars.put("pythonMessage", "Hello from Python scheduler!");
        customVars.put("maxExecutions", 10);
        schedulerDef.setCustomVariables(customVars);
        
        // Create the plugin
        SchedulerPlugin<?> plugin = pythonFactory.createSchedulerPlugin(schedulerDef);
        
        // Verify plugin was created successfully
        assertThat(plugin).isNotNull();
        assertThat(plugin.getName()).isEqualTo("test-python-scheduler");
        
        // Manually execute the plugin with scheduler context
        Map<String, Object> schedulerVars = new HashMap<>();
        schedulerVars.put("executionTime", System.currentTimeMillis());
        schedulerVars.put("executionCount", 1L);
        schedulerVars.put("isManualTrigger", true);
        schedulerVars.put("pythonMessage", "Hello from Python scheduler!");
        
        // Set scheduler variables in context
        SchedulerVariableContext.setVariables(schedulerVars);
        
        try {
            // Execute plugin
            plugin.execute();
            
            // Verify state was updated
            var pluginState = pluginCacheService.forPlugin("test-python-scheduler");
            Integer counter = (Integer) pluginState.get("pyCounter");
            assertThat(counter).isEqualTo(1);
            
            Long lastExecution = (Long) pluginState.get("lastPyExecution");
            assertThat(lastExecution).isNotNull();
            
            String pythonMessage = (String) pluginState.get("pythonMessage");
            assertThat(pythonMessage).isEqualTo("Hello from Python scheduler!");
            
            // Check execution count was stored
            Long executionCount = (Long) pluginState.get("pyExecutionCount");
            assertThat(executionCount).isEqualTo(1L);
            
        } finally {
            // Clear scheduler context
            SchedulerVariableContext.clearVariables();
        }
    }
    
    @Test
    void testPythonSchedulerWithDataProcessing() {
        SchedulerPluginDef schedulerDef = new SchedulerPluginDef();
        schedulerDef.setName("test-python-data-scheduler");
        schedulerDef.setLang("python");
        schedulerDef.setInlineScript("""
            # Simulate data collection and processing (no function wrapper needed)
            data_points = state.get('dataPoints') or []
            
            # Add new data point
            new_point = {
                'id': executionCount,
                'timestamp': executionTime,
                'value': executionCount * 10,  # Some computed value
                'processed': False
            }
            data_points.append(new_point)
            
            # Process data points (batch processing simulation)
            processed_count = 0
            for point in data_points:
                if not point['processed']:
                    # Simulate processing
                    point['processed'] = True
                    point['processed_at'] = executionTime
                    point['result'] = point['value'] * 2  # Some transformation
                    processed_count += 1
                    
                    # Process max 3 items per execution
                    if processed_count >= 3:
                        break
            
            # Store results - need to unwrap complex objects
            state.put('dataPoints', unwrapper.unwrap(data_points))
            state.put('lastProcessedCount', processed_count)
            state.put('totalDataPoints', len(data_points))
            
            # Calculate statistics
            processed_points = [p for p in data_points if p['processed']]
            avg_value = sum(p['value'] for p in processed_points) / len(processed_points) if processed_points else 0
            
            stats = {
                'total_points': len(data_points),
                'processed_points': len(processed_points),
                'avg_value': avg_value,
                'last_execution': executionTime
            }
            # Store individual statistics instead of a complex object
            state.put('stats_total_points', len(data_points))
            state.put('stats_processed_points', len(processed_points))
            state.put('stats_avg_value', avg_value)
            state.put('stats_last_execution', executionTime)
            """);
        schedulerDef.setExitOnError(false);
        
        // Create plugin
        SchedulerPlugin<?> plugin = pythonFactory.createSchedulerPlugin(schedulerDef);
        
        // Set scheduler variables
        Map<String, Object> schedulerVars = new HashMap<>();
        schedulerVars.put("executionTime", System.currentTimeMillis());
        schedulerVars.put("executionCount", 2L);
        schedulerVars.put("isManualTrigger", false);
        
        SchedulerVariableContext.setVariables(schedulerVars);
        
        try {
            // Execute plugin
            plugin.execute();
            
            // Verify data processing worked
            var pluginState = pluginCacheService.forPlugin("test-python-data-scheduler");
            Integer totalDataPoints = (Integer) pluginState.get("totalDataPoints");
            assertThat(totalDataPoints).isEqualTo(1);  // Should have added one data point
            
            // Verify individual statistics were stored
            Integer totalPoints = (Integer) pluginState.get("stats_total_points");
            Integer processedPoints = (Integer) pluginState.get("stats_processed_points");
            assertThat(totalPoints).isEqualTo(1);
            assertThat(processedPoints).isEqualTo(1);
            
        } finally {
            SchedulerVariableContext.clearVariables();
        }
    }
}