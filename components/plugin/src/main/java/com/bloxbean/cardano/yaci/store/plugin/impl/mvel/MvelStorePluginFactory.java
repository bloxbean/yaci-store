package com.bloxbean.cardano.yaci.store.plugin.impl.mvel;

import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.*;
import com.bloxbean.cardano.yaci.store.plugin.cache.PluginCacheService;
import com.bloxbean.cardano.yaci.store.plugin.util.PluginContextUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MvelStorePluginFactory implements PluginFactory {

    private final PluginContextUtil pluginContextUtil;
    private final PluginCacheService pluginCacheService;

    @PostConstruct
    public void init() {
      log.info("MVEL Plugin Factory created >>");
    }

    @Override
    public String getType() {
        return "mvel";
    }

    @Override
    public <T> InitPlugin createInitPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for init plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return createFromScript(def);
        else
            throw new IllegalArgumentException("No inline-script or script found in init definition for mvel plugin: " + def);
    }

    @Override
    public <T> FilterPlugin<T> createFilterPlugin(PluginDef def) {
        if (def.getExpression() != null)
            return new MvelExpressionStorePlugin<>(def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return createFromScript(def);
        else
            throw new IllegalArgumentException("No expression or script found in filter definition for mvel plugin: " + def);
    }

    @Override
    public <T> PostActionPlugin<T> createPostActionPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for post-action plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return createFromScript(def);
        else
            throw new IllegalArgumentException("No script or inline-script found in filter definition for mvel plugin: " + def);
    }

    @Override
    public <T> PreActionPlugin<T> createPreActionPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for pre-action plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return createFromScript(def);
        else
            throw new IllegalArgumentException("No script or inline-script found in filter definition for mvel plugin: " + def);
    }

    @Override
    public <T> EventHandlerPlugin<T> createEventHandlerPlugin(PluginDef def) {
        if (def.getExpression() != null)
            throw new IllegalArgumentException("Use script or inline-script for event-handler plugin. {}" + def);
        else if (def.getInlineScript() != null || def.getScript() != null)
            return createFromScript(def);
        else
            throw new IllegalArgumentException("No script or inline-script found in event-handler definition for mvel plugin: " + def);
    }

    private <T> MvelScriptStorePlugin<T> createFromScript(PluginDef def) {
        if (def.getScript() != null) {
            var script = def.getScript();
            if (script.getFile() == null)
                throw new IllegalArgumentException("Script file is not defined in plugin def: " + def);
            return new MvelScriptStorePlugin<>(def, pluginContextUtil, pluginCacheService);
        } else if (def.getInlineScript() != null) {
            return new MvelScriptStorePlugin<>(def, null, pluginContextUtil, pluginCacheService);
        } else {
            throw new IllegalArgumentException("No script or inline-script found in filter definition for mvel plugin: " + def);
        }
    }

}
