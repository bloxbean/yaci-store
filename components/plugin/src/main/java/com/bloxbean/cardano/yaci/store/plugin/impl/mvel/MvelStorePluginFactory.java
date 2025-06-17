package com.bloxbean.cardano.yaci.store.plugin.impl.mvel;

import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.config.ScriptRef;
import com.bloxbean.cardano.yaci.store.plugin.api.*;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginStateService;
import com.bloxbean.cardano.yaci.store.plugin.variables.VariableProviderFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MvelStorePluginFactory implements PluginFactory {

    private final PluginStateService pluginCacheService;
    private final VariableProviderFactory variableProviderFactory;

    @PostConstruct
    public void init() {
      log.info("MVEL Plugin Factory created >>");
    }

    @Override
    public String getLang() {
        return "mvel";
    }

    @Override
    public void initGlobalScripts(List<ScriptRef> scriptRef) {
        //do nothing
    }

    @Override
    public <T> InitPlugin createInitPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for init plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return createFromScript(def, PluginType.INIT);
        else
            throw new IllegalArgumentException("No inline-script or script found in init definition for mvel plugin: " + def);
    }

    @Override
    public <T> FilterPlugin<T> createFilterPlugin(PluginDef def) {
        if (def.getExpression() != null)
            return new MvelExpressionStorePlugin<>(def, PluginType.FILTER);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return createFromScript(def, PluginType.FILTER);
        else
            throw new IllegalArgumentException("No expression or script found in filter definition for mvel plugin: " + def);
    }

    @Override
    public <T> PostActionPlugin<T> createPostActionPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for post-action plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return createFromScript(def, PluginType.POST_ACTION);
        else
            throw new IllegalArgumentException("No script or inline-script found in filter definition for mvel plugin: " + def);
    }

    @Override
    public <T> PreActionPlugin<T> createPreActionPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for pre-action plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return createFromScript(def, PluginType.PRE_ACTION);
        else
            throw new IllegalArgumentException("No script or inline-script found in filter definition for mvel plugin: " + def);
    }

    @Override
    public <T> EventHandlerPlugin<T> createEventHandlerPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for event-handler plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return createFromScript(def, PluginType.EVENT_HANDLER);
        else
            throw new IllegalArgumentException("No script or inline-script found in event-handler definition for mvel plugin: " + def);
    }

    private <T> MvelScriptStorePlugin<T> createFromScript(PluginDef def, PluginType pluginType) {
        if (def.getScript() != null) {
            var script = def.getScript();
            if (script.getFile() == null)
                throw new IllegalArgumentException("Script file is not defined in plugin def: " + def);
            return new MvelScriptStorePlugin<>(def, pluginType, pluginCacheService, variableProviderFactory);
        } else if (def.getInlineScript() != null) {
            return new MvelScriptStorePlugin<>(def, pluginType, null, pluginCacheService, variableProviderFactory);
        } else {
            throw new IllegalArgumentException("No script or inline-script found in filter definition for mvel plugin: " + def);
        }
    }

}
