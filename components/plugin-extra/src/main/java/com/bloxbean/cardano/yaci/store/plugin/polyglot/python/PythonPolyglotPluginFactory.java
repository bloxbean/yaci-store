package com.bloxbean.cardano.yaci.store.plugin.polyglot.python;

import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.*;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.*;

@Slf4j
public class PythonPolyglotPluginFactory implements PluginFactory {
    private PluginContextUtil pluginContextUtil;
    private PluginCacheService pluginCacheService;
    private Engine engine;
    private String venvPath;

    public PythonPolyglotPluginFactory(PluginContextUtil pluginContextUtil, PluginCacheService pluginCacheService) {
        this.pluginContextUtil = pluginContextUtil;
        this.pluginCacheService = pluginCacheService;
        this.engine = Engine.create();

        log.info("Python Polyglot Plugin Factory created with GraalVM engine >>");
    }

    @Override
    public String getType() {
        return "python";
    }

    public void setVirtualEnvPath(String venvPath) {
        this.venvPath = venvPath;
    }

    @Override
    public <T> InitPlugin<T> createInitPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for init plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return new PythonScriptStorePlugin<>(engine, def, venvPath, pluginContextUtil, pluginCacheService, true);
        else
            throw new IllegalArgumentException("No script or inline-script found in init python plugin: " + def);
    }

    @Override
    public <T> FilterPlugin<T> createFilterPlugin(PluginDef def) {
        if (def.getExpression() != null)
            return new PythonScriptStorePlugin<>(engine, def, venvPath, pluginContextUtil, pluginCacheService);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return new PythonScriptStorePlugin<>(engine, def, venvPath, pluginContextUtil, pluginCacheService);
        else
            throw new IllegalArgumentException("No expression or script found in filter definition for python plugin: " + def);
    }

    @Override
    public <T> PostActionPlugin<T> createPostActionPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for post-action plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return new PythonScriptStorePlugin<>(engine, def, venvPath, pluginContextUtil, pluginCacheService);
        else
            throw new IllegalArgumentException("No script or inline-script found in filter definition for python plugin: " + def);
    }

    @Override
    public <T> PreActionPlugin<T> createPreActionPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for pre-action plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return new PythonScriptStorePlugin<>(engine, def, venvPath, pluginContextUtil, pluginCacheService);
        else
            throw new IllegalArgumentException("No script or inline-script found in filter definition for python plugin: " + def);
    }

    @Override
    public <T> EventHandlerPlugin<T> createEventHandlerPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for event-handler plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return new PythonScriptStorePlugin<>(engine, def, venvPath, pluginContextUtil, pluginCacheService);
        else
            throw new IllegalArgumentException("No script or inline-script found in event-handler definition for python plugin: " + def);
    }

}
