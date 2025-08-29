package com.bloxbean.cardano.yaci.store.plugin.polyglot.js;

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
 * Integration test for JavaScript scheduler plugins
 */
public class JsSchedulerPluginIntegrationTest extends BasePluginTest {
    
    private JsPolyglotPluginFactory jsFactory;
    
    @BeforeEach
    void setupJs() {
        // Setup JS factory using the base class setup
        jsFactory = new JsPolyglotPluginFactory(
            pluginContextUtil,
            pluginCacheService,
            variableProviderFactory,
            contextProvider,
            globalScriptContextRegistry
        );
    }
    
    @Test
    void testJavaScriptSchedulerPluginExecution() {
        // Create a JavaScript scheduler plugin definition
        SchedulerPluginDef schedulerDef = new SchedulerPluginDef();
        schedulerDef.setName("test-js-scheduler");
        schedulerDef.setLang("js");
        schedulerDef.setInlineScript("""
            // Access scheduler variables directly (no function wrapper needed)
            var counter = state.get('jsCounter') || 0;
            counter = counter + 1;
            state.put('jsCounter', counter);
            state.put('lastJsExecution', executionTime);
            state.put('jsExecutionCount', executionCount);
            
            // Use custom variables if available
            if (typeof customMessage !== 'undefined') {
                state.put('customMessage', customMessage);
            }
            """);
        schedulerDef.setExitOnError(false);
        
        // Set custom variables
        Map<String, Object> customVars = new HashMap<>();
        customVars.put("customMessage", "Hello from JS scheduler!");
        schedulerDef.setCustomVariables(customVars);
        
        // Create the plugin
        SchedulerPlugin<?> plugin = jsFactory.createSchedulerPlugin(schedulerDef);
        
        // Verify plugin was created successfully
        assertThat(plugin).isNotNull();
        assertThat(plugin.getName()).isEqualTo("test-js-scheduler");
        
        // Manually execute the plugin with scheduler context
        Map<String, Object> schedulerVars = new HashMap<>();
        schedulerVars.put("executionTime", System.currentTimeMillis());
        schedulerVars.put("executionCount", 1L);
        schedulerVars.put("isManualTrigger", true);
        schedulerVars.put("customMessage", "Hello from JS scheduler!");
        
        // Set scheduler variables in context
        SchedulerVariableContext.setVariables(schedulerVars);
        
        try {
            // Execute plugin
            plugin.execute();
            
            // Verify state was updated
            var pluginState = pluginCacheService.forPlugin("test-js-scheduler");
            Integer counter = (Integer) pluginState.get("jsCounter");
            assertThat(counter).isEqualTo(1);
            
            Long lastExecution = (Long) pluginState.get("lastJsExecution");
            assertThat(lastExecution).isNotNull();
            
            String customMessage = (String) pluginState.get("customMessage");
            assertThat(customMessage).isEqualTo("Hello from JS scheduler!");
            
        } finally {
            // Clear scheduler context
            SchedulerVariableContext.clearVariables();
        }
    }
    
    @Test
    void testJavaScriptSchedulerWithComplexLogic() {
        SchedulerPluginDef schedulerDef = new SchedulerPluginDef();
        schedulerDef.setName("test-js-complex-scheduler");
        schedulerDef.setLang("js");
        schedulerDef.setInlineScript("""
            // Complex data processing example (no function wrapper needed)
            var data = state.get('processingQueue') || [];
            
            // Add new item to queue
            var newItem = {
                id: executionCount,
                timestamp: executionTime,
                processed: false
            };
            data.push(newItem);
            
            // Process items (simulate batch processing)
            var processed = 0;
            for (var i = 0; i < data.length; i++) {
                if (!data[i].processed) {
                    data[i].processed = true;
                    data[i].processedAt = executionTime;
                    processed++;
                    
                    // Only process 2 items per execution
                    if (processed >= 2) break;
                }
            }
            
            state.put('processingQueue', data);
            state.put('lastProcessedCount', processed);
            state.put('totalItems', data.length);
            
            // Store result object
            state.put('result', {
                processed: processed,
                queueSize: data.length,
                executionTime: executionTime
            });
            """);
        schedulerDef.setExitOnError(false);
        
        // Create plugin
        SchedulerPlugin<?> plugin = jsFactory.createSchedulerPlugin(schedulerDef);
        
        // Set scheduler variables
        Map<String, Object> schedulerVars = new HashMap<>();
        schedulerVars.put("executionTime", System.currentTimeMillis());
        schedulerVars.put("executionCount", 3L);
        schedulerVars.put("isManualTrigger", false);
        
        SchedulerVariableContext.setVariables(schedulerVars);
        
        try {
            // Execute plugin
            plugin.execute();
            
            // Verify complex processing worked
            var pluginState = pluginCacheService.forPlugin("test-js-complex-scheduler");
            Integer totalItems = (Integer) pluginState.get("totalItems");
            assertThat(totalItems).isEqualTo(1);  // Should have added one item
            
            Integer lastProcessedCount = (Integer) pluginState.get("lastProcessedCount");
            assertThat(lastProcessedCount).isEqualTo(1);  // Should have processed the one item
            
        } finally {
            SchedulerVariableContext.clearVariables();
        }
    }
}