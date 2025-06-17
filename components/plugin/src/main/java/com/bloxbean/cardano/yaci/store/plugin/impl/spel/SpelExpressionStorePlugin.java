package com.bloxbean.cardano.yaci.store.plugin.impl.spel;

import com.bloxbean.cardano.yaci.store.plugin.api.config.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.FilterPlugin;
import com.bloxbean.cardano.yaci.store.plugin.api.PluginType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
public class SpelExpressionStorePlugin<T> implements FilterPlugin<T> {
    private final String name;
    private final PluginDef pluginDef;
    private final PluginType pluginType;
    private final Expression predicate;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    public SpelExpressionStorePlugin(PluginDef pluginDef, PluginType pluginType) {
        this.name = pluginDef.getName();
        this.pluginDef = pluginDef;
        this.pluginType = pluginType;
        this.predicate = parser.parseExpression(pluginDef.getExpression());
        log.info("Created filter {} with expression {}", name, pluginDef.getExpression());
    }

    @Override
    public Collection<T> filter(Collection<T> items) {
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        return items.stream()
                .filter(item -> {
                    ctx.setRootObject(item);
                    return Boolean.TRUE.equals(predicate.getValue(ctx, Boolean.class));
                })
                .collect(Collectors.toList());
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
}

