package com.bloxbean.cardano.yaci.store.plugin.impl.mvel;

import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.*;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import com.bloxbean.cardano.yaci.store.plugin.scheduler.SchedulerVariableContext;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MvelScriptStorePlugin<T> implements InitPlugin<T>, FilterPlugin<T>, PreActionPlugin<T>, PostActionPlugin<T>, EventHandlerPlugin<T>, SchedulerPlugin<T> {
    private final String name;
    private final PluginDef pluginDef;
    private final PluginType pluginType;
    private final Serializable compiledExpr;
    private final String functionName;
    private final PluginStateService stateService;
    private final VariableProviderFactory variableProviderFactory;

    public MvelScriptStorePlugin(PluginDef pluginDef,
                                 PluginType pluginType,
                                 PluginStateService stateService,
                                 VariableProviderFactory variableProviderFactory) {
        this.name = pluginDef.getName();
        this.pluginDef = pluginDef;
        this.pluginType = pluginType;
        String file = pluginDef.getScript().getFile();
        this.functionName = pluginDef.getScript().getFunction();
        this.stateService = stateService;
        this.variableProviderFactory = variableProviderFactory;

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Script file cannot be null or empty " + pluginDef.getScript());
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

    public MvelScriptStorePlugin(PluginDef pluginDef,
                                 PluginType pluginType,
                                 String function,
                                 PluginStateService stateService,
                                 VariableProviderFactory variableProviderFactory
                                 ) {
        this.name = pluginDef.getName();
        this.pluginDef = pluginDef;
        this.pluginType = pluginType;
        this.functionName = function;
        this.stateService = stateService;
        this.variableProviderFactory = variableProviderFactory;

        String inlineScript = pluginDef.getInlineScript();

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
    public PluginDef getPluginDef() {
        return pluginDef;
    }

    @Override
    public PluginType getPluginType() {
        return pluginType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<T> filter(Collection<T> items) {
        if(log.isTraceEnabled())
            log.trace("Filter {} items with MVEL filter {}", items.size(), name);

        if (items == null || items.isEmpty()) {
            return items;
        }

        Map<String,Object> vars = new HashMap<>();
        setCommonVariables(vars);
        vars.put("items", items);

        MapVariableResolverFactory vrf = new MapVariableResolverFactory(vars);

        if (functionName != null) {
            MVEL.executeExpression(compiledExpr, null, vrf);

            String invokeExpr = functionName + "(items)";
            return (Collection<T>) MVEL.executeExpression(
                    MVEL.compileExpression(invokeExpr), null, vrf);
        } else {
            return (Collection<T>) MVEL.executeExpression(compiledExpr, null, vrf);
        }
    }

    @Override
    public Collection<T> preAction(Collection<T> items) {
        if(log.isTraceEnabled())
            log.trace("PreAction {} items with MVEL pre-action {}", items.size(), name);

        if (items == null || items.isEmpty()) {
            return items;
        }

        Map<String,Object> vars = new HashMap<>();
        setCommonVariables(vars);
        vars.put("items", items);

        MapVariableResolverFactory vrf = new MapVariableResolverFactory(vars);

        if (functionName != null) {
            MVEL.executeExpression(compiledExpr, null, vrf);

            String invokeExpr = functionName + "(items)";
            return (Collection<T>) MVEL.executeExpression(
                    MVEL.compileExpression(invokeExpr), null, vrf);
        } else {
            return (Collection<T>) MVEL.executeExpression(compiledExpr, null, vrf);
        }
    }

    @Override
    public void postAction(Collection<T> items) {
        if (log.isTraceEnabled())
            log.trace("PostAction {} items with MVEL post-action plugin {}", items.size(), name);

        Map<String,Object> vars = new HashMap<>();
        vars.put("items", items);
        setCommonVariables(vars);

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
        setCommonVariables(vars);

        MapVariableResolverFactory vrf = new MapVariableResolverFactory(vars);

        MVEL.executeExpression(compiledExpr, null, vrf);

        if (functionName != null) {
            String invokeExpr = functionName + "(event)";
            MVEL.executeExpression(
                    MVEL.compileExpression(invokeExpr), null, vrf);
        }
    }

    @Override
    public void initPlugin() {
        if (log.isTraceEnabled())
            log.trace("Init plugin {} - MVEL", name);

        Map<String,Object> vars = new HashMap<>();
        setCommonVariables(vars);

        MapVariableResolverFactory vrf = new MapVariableResolverFactory(vars);

        MVEL.executeExpression(compiledExpr, null, vrf);
    }

    @Override
    public void execute() {
        if (log.isTraceEnabled())
            log.trace("Execute scheduler plugin {} - MVEL", name);

        Map<String,Object> vars = new HashMap<>();
        setCommonVariables(vars);
        
        // Add scheduler-specific variables from context
        Map<String, Object> schedulerVars = SchedulerVariableContext.getVariables();
        if (schedulerVars != null && !schedulerVars.isEmpty()) {
            vars.putAll(schedulerVars);
            if (log.isDebugEnabled()) {
                log.debug("Added {} scheduler variables for plugin {}", schedulerVars.size(), name);
            }
        }

        MapVariableResolverFactory vrf = new MapVariableResolverFactory(vars);

        if (functionName != null) {
            MVEL.executeExpression(compiledExpr, null, vrf);

            String invokeExpr = functionName + "()";
            MVEL.executeExpression(
                    MVEL.compileExpression(invokeExpr), null, vrf);
        } else {
            MVEL.executeExpression(compiledExpr, null, vrf);
        }
    }

    private void setCommonVariables(Map<String,Object> vars) {
        var variables = variableProviderFactory != null? variableProviderFactory.getVariables(): null;
        if (variables != null)
            vars.putAll(variables);

        vars.put("state", stateService.forPlugin(name));
    }
}
