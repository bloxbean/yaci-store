package com.bloxbean.cardano.yaci.store.plugin.impl.spel;

import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.config.ScriptRef;
import com.bloxbean.cardano.yaci.store.plugin.api.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class SpelStorePluginFactory implements PluginFactory {
    private final SpelExpressionParser parser = new SpelExpressionParser();

    public SpelStorePluginFactory() {
        log.info("SpEL Plugin Factory created >>");
    }

    @Override public String getLang() { return "spel"; }

    @Override
    public void initGlobalScripts(List<ScriptRef> scriptRef) {
        //do nothing
    }

    @Override
    public <T> InitPlugin createInitPlugin(PluginDef def) {
        throw new IllegalArgumentException("Init plugin is not supported for spel.");
    }

    @Override
    public <T> FilterPlugin<T> createFilterPlugin(PluginDef def) {
        if (def.getExpression() == null) {
            throw new IllegalArgumentException("No expression found in filter definition for spel filter: " + def);
        }
        Expression expr = parser.parseExpression(def.getExpression());
        return new SpelExpressionStorePlugin<>(def, PluginType.FILTER);
    }

    @Override
    public <T> PostActionPlugin<T> createPostActionPlugin(PluginDef def) {
        throw new UnsupportedOperationException("Post Action plugin is not supported for spel");
    }

    @Override
    public <T> PreActionPlugin<T> createPreActionPlugin(PluginDef def) {
        throw new UnsupportedOperationException("Pre Action plugin is not supported for spel");
    }

    @Override
    public <T> EventHandlerPlugin<T> createEventHandlerPlugin(PluginDef def) {
        throw new UnsupportedOperationException("Event Handler plugin is not supported for spel");
    }

}

