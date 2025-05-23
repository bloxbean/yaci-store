package com.bloxbean.cardano.yaci.store.plugin.filter.spel;

import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.*;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

@Component
public class SpelStorePluginFactory implements PluginFactory {
    private final SpelExpressionParser parser = new SpelExpressionParser();
    @Override public String getType() { return "spel"; }

    @Override
    public <T> FilterPlugin<T> createFilterPlugin(PluginDef def) {
        if (def.getExpression() == null) {
            throw new IllegalArgumentException("No expression found in filter definition for spel filter: " + def);
        }
        Expression expr = parser.parseExpression(def.getExpression());
        return new SpelExpressionStorePlugin<>(def.getName(), expr.getExpressionString());
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

