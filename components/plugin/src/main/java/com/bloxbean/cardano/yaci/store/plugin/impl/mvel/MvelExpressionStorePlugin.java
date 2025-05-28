package com.bloxbean.cardano.yaci.store.plugin.impl.mvel;

import com.bloxbean.cardano.yaci.store.common.plugin.PluginDef;
import com.bloxbean.cardano.yaci.store.plugin.api.FilterPlugin;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
public class MvelExpressionStorePlugin<T> implements FilterPlugin<T> {
    private final String name;
    private final PluginDef pluginDef;
    private final Serializable compiledExpr;

    public MvelExpressionStorePlugin(PluginDef pluginDef) {
        this.name = pluginDef.getName();
        this.pluginDef = pluginDef;
        this.compiledExpr = MVEL.compileExpression(pluginDef.getExpression());
        log.info("Created MVEL filter {} with expression:\n{}", name, pluginDef.getExpression());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<T> filter(Collection<T> items) {
        return items.stream()
                .filter(item -> {
                    Object result = MVEL.executeExpression(compiledExpr, item);
                    return Boolean.TRUE.equals(result);
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
}
