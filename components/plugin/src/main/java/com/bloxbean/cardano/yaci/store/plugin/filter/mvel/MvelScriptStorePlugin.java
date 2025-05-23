package com.bloxbean.cardano.yaci.store.plugin.filter.mvel;

import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.EventHandlerPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PostActionPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PreActionPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.FilterPlugin;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MvelScriptStorePlugin<T> implements FilterPlugin<T>, PreActionPlugin<T>, PostActionPlugin<T>, EventHandlerPlugin<T> {
    private final String name;
    private final Serializable compiledExpr;
    private final String functionName;
    private final PluginContextUtil pluginContextUtil;

    public MvelScriptStorePlugin(String name, PluginDef.Script script, PluginContextUtil pluginContextUtil) {
        this.name = name;
        String file = script.getFile();
        this.functionName = script.getFunction();
        this.pluginContextUtil = pluginContextUtil;

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Script file cannot be null or empty " + script);
        }

        String code;
        try {
            code = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(file)));
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to load script from file: " + file, e);
        }

        this.compiledExpr = MVEL.compileExpression(code);
        log.info("Created MVEL plugin {} with script file:\n{}", name, file);

    }

    public MvelScriptStorePlugin(String name, String inlineScript, String function, PluginContextUtil pluginContextUtil) {
        this.name = name;
        this.functionName = function;
        this.pluginContextUtil = pluginContextUtil;

        if (inlineScript == null || inlineScript.isEmpty()) {
            throw new IllegalArgumentException("Inline Script cannot be null or empty");
        }

        this.compiledExpr = MVEL.compileExpression(inlineScript);

        log.info("Created MVEL plugin {} with inline script:\n{}", name, inlineScript);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<T> filter(Collection<T> items) {
        if(log.isTraceEnabled())
            log.trace("Filtering {} items with MVEL filter {}", items.size(), name);

        if (items == null || items.isEmpty()) {
            return items;
        }

        Map<String,Object> vars = new HashMap<>();
        vars.put("items", items);
        vars.put("util", pluginContextUtil);

        // 2) wrap it in a VariableResolverFactory
        MapVariableResolverFactory vrf = new MapVariableResolverFactory(vars);

        return (Collection<T>) MVEL.executeExpression(compiledExpr, null, vrf);
    }

    @Override
    public void preAction(Collection<T> items) {
        if (log.isTraceEnabled())
            log.trace("PreAction {} items with MVEL pre-action plugin {}", items.size(), name);

        Map<String,Object> vars = new HashMap<>();
        vars.put("items", items);
        vars.put("util", pluginContextUtil);

        MapVariableResolverFactory vrf = new MapVariableResolverFactory(vars);

        MVEL.executeExpression(compiledExpr, null, vrf);

        if (functionName != null) {
            String invokeExpr = functionName + "(items)";
            MVEL.executeExpression(
                    MVEL.compileExpression(invokeExpr), null, vrf);
        }

    }

    @Override
    public void postAction(Collection<T> items) {
        if (log.isTraceEnabled())
            log.trace("PostAction {} items with MVEL post-action plugin {}", items.size(), name);

        Map<String,Object> vars = new HashMap<>();
        vars.put("items", items);
        vars.put("util", pluginContextUtil);

        MapVariableResolverFactory vrf = new MapVariableResolverFactory(vars);

        MVEL.executeExpression(compiledExpr, null, vrf);

        if (functionName != null) {
            String invokeExpr = functionName + "(items)";
            MVEL.executeExpression(
                    MVEL.compileExpression(invokeExpr), null, vrf);
        }
    }

    @Override
    public void handleEvent(Object event) {
        if (log.isTraceEnabled())
            log.trace("EventHandler {} with MVEL post-action plugin {}", event, name);

        Map<String,Object> vars = new HashMap<>();
        vars.put("event", event);
        vars.put("util", pluginContextUtil);

        MapVariableResolverFactory vrf = new MapVariableResolverFactory(vars);

        MVEL.executeExpression(compiledExpr, null, vrf);

        if (functionName != null) {
            String invokeExpr = functionName + "(event)";
            MVEL.executeExpression(
                    MVEL.compileExpression(invokeExpr), null, vrf);
        }
    }
}
