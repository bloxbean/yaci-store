package com.bloxbean.cardano.yaci.store.plugin.filter.spel;

import com.bloxbean.cardano.yaci.store.plugin.api.FilterPlugin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Collection;
import java.util.stream.Collectors;

@Slf4j
public class SpelExpressionStorePlugin<T> implements FilterPlugin<T> {
    private final String name;
    private final Expression predicate;
    private final SpelExpressionParser parser = new SpelExpressionParser();

    public SpelExpressionStorePlugin(String name, String expr) {
        this.name = name;
        this.predicate = parser.parseExpression(expr);
        log.info("Created filter {} with expression {}", name, expr);
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
}

