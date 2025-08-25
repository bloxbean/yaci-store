package com.bloxbean.cardano.yaci.store.plugin.impl.mvel;

import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import com.bloxbean.cardano.yaci.store.plugin.api.SchedulerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.config.ScriptDef;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateConfig;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MvelSchedulerPluginBasicTest {
    static PluginStateConfig pluginStateConfig;
    static PluginStateService pluginStateService;

    @BeforeAll
    static void setup() {
        pluginStateConfig = new PluginStateConfig();
        pluginStateService = new PluginStateService(pluginStateConfig.globalState(),
                pluginStateConfig.pluginStates());
    }

    @Test
    void createSchedulerPlugin_withInlineScript() {
        MvelStorePluginFactory factory = new MvelStorePluginFactory(pluginStateService, null);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test-scheduler");
        pluginDef.setLang("mvel");
        pluginDef.setInlineScript("state.put('inlineTest', true)");

        SchedulerPlugin<Object> schedulerPlugin = factory.createSchedulerPlugin(pluginDef);

        assertThat(schedulerPlugin).isNotNull();
        assertThat(schedulerPlugin.getName()).isEqualTo("test-scheduler");
        assertThat(schedulerPlugin.getPluginType()).isEqualTo(PluginType.SCHEDULER);
    }

    @Test
    void createSchedulerPlugin_withScriptFile() throws IOException {
        MvelStorePluginFactory factory = new MvelStorePluginFactory(pluginStateService, null);

        String scriptContent = """
                def executeScheduler() {
                    state.put('scriptTest', true);
                }
                """;

        Path tempScriptFile = Files.createTempFile("scheduler_script", ".mvel");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test-scheduler");
        pluginDef.setLang("mvel");

        ScriptDef scriptDef = new ScriptDef();
        scriptDef.setFile(tempScriptFile.toFile().getAbsolutePath());
        scriptDef.setFunction("executeScheduler");
        pluginDef.setScript(scriptDef);

        SchedulerPlugin<Object> schedulerPlugin = factory.createSchedulerPlugin(pluginDef);

        assertThat(schedulerPlugin).isNotNull();
        assertThat(schedulerPlugin.getName()).isEqualTo("test-scheduler");
        assertThat(schedulerPlugin.getPluginType()).isEqualTo(PluginType.SCHEDULER);

        // Clean up
        Files.deleteIfExists(tempScriptFile);
    }

    @Test
    void createSchedulerPlugin_withExpression_throwsException() {
        MvelStorePluginFactory factory = new MvelStorePluginFactory(pluginStateService, null);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test-scheduler");
        pluginDef.setLang("mvel");
        pluginDef.setExpression("state.put('created', true)");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> factory.createSchedulerPlugin(pluginDef));

        assertThat(exception.getMessage()).contains("Expression is not supported for scheduler plugin");
    }

    @Test
    void createSchedulerPlugin_throwsExceptionWhenNoScript() {
        MvelStorePluginFactory factory = new MvelStorePluginFactory(pluginStateService, null);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test-scheduler");
        pluginDef.setLang("mvel");
        // No expression, inlineScript, or script set

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> factory.createSchedulerPlugin(pluginDef));

        assertThat(exception.getMessage()).contains("No inline-script or script found in scheduler definition");
    }

    @Test
    void executeSchedulerPlugin_withInlineScript() {
        MvelStorePluginFactory factory = new MvelStorePluginFactory(pluginStateService, null);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("test-scheduler");
        pluginDef.setLang("mvel");
        pluginDef.setInlineScript("""
                // Basic test that only uses state
                counter = state.get('counter');
                if (counter == null) {
                    counter = 0;
                }
                counter = counter + 1;
                state.put('counter', counter);
                """);

        SchedulerPlugin<Object> schedulerPlugin = factory.createSchedulerPlugin(pluginDef);

        // Execute multiple times to test counter
        schedulerPlugin.execute();
        schedulerPlugin.execute();
        schedulerPlugin.execute();

        // Verify state was updated correctly
        var pluginState = pluginStateService.forPlugin("test-scheduler");
        assertThat(pluginState.get("counter")).isEqualTo(3);
    }

    @Test
    void executeSchedulerPlugin_withScriptFileAndFunction() throws IOException {
        MvelStorePluginFactory factory = new MvelStorePluginFactory(pluginStateService, null);

        String scriptContent = """
                def processTask() {
                    // Basic test that only uses state
                    tasks = state.get('tasks');
                    if (tasks == null) {
                        tasks = [];
                    }
                    newTask = ['executed': true];
                    tasks.add(newTask);
                    state.put('tasks', tasks);
                    state.put('lastProcessed', true);
                }
                """;

        Path tempScriptFile = Files.createTempFile("scheduler_script", ".mvel");
        Files.writeString(tempScriptFile, scriptContent);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("task-scheduler");
        pluginDef.setLang("mvel");

        ScriptDef scriptDef = new ScriptDef();
        scriptDef.setFile(tempScriptFile.toFile().getAbsolutePath());
        scriptDef.setFunction("processTask");
        pluginDef.setScript(scriptDef);

        SchedulerPlugin<Object> schedulerPlugin = factory.createSchedulerPlugin(pluginDef);

        // Execute scheduler
        schedulerPlugin.execute();

        // Verify state
        var pluginState = pluginStateService.forPlugin("task-scheduler");
        assertThat(pluginState.get("lastProcessed")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        var tasks = (java.util.List<java.util.Map<String, Object>>) pluginState.get("tasks");
        assertThat(tasks).hasSize(1);
        assertThat(tasks.get(0).get("executed")).isEqualTo(true);

        // Clean up
        Files.deleteIfExists(tempScriptFile);
    }

    @Test
    void schedulerPlugin_implementsCorrectInterface() {
        MvelStorePluginFactory factory = new MvelStorePluginFactory(pluginStateService, null);

        PluginDef pluginDef = new PluginDef();
        pluginDef.setName("interface-test");
        pluginDef.setLang("mvel");
        pluginDef.setInlineScript("state.put('interfaceTest', true)");

        SchedulerPlugin<Object> schedulerPlugin = factory.createSchedulerPlugin(pluginDef);

        // Verify it implements the SchedulerPlugin interface correctly
        assertThat(schedulerPlugin).isInstanceOf(SchedulerPlugin.class);
        assertThat(schedulerPlugin.getPluginType()).isEqualTo(PluginType.SCHEDULER);

        // Should be able to call execute() without parameters
        schedulerPlugin.execute();

        // Verify it executed
        var pluginState = pluginStateService.forPlugin("interface-test");
        assertThat(pluginState.get("interfaceTest")).isEqualTo(true);
    }
}
