package com.bloxbean.cardano.yaci.store.plugin.filter.mvel;

import com.bloxbean.cardano.yaci.store.plugin.api.FilterPlugin;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
public class MvelExpressionStorePlugin<T> implements FilterPlugin<T> {
    private final String name;
    private final Serializable compiledExpr;

    public MvelExpressionStorePlugin(String name, String expression) {
        this.name = name;
        this.compiledExpr = MVEL.compileExpression(expression);
        log.info("Created MVEL filter {} with expression:\n{}", name, expression);
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
}
