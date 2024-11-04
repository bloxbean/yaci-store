package com.bloxbean.cardano.yaci.store.core.annotation;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition to check if local data support is enabled.
 * It is enabled if either store.cardano.n2c-node-socket-path or store.cardano.n2c-host is specified.
 */
public class LocalSupportCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();

        String n2cNodeSocketPath = env.getProperty("store.cardano.n2c-node-socket-path", "");
        String n2cHost = env.getProperty("store.cardano.n2c-host", "");

        return !n2cNodeSocketPath.isEmpty() || !n2cHost.isEmpty();
    }
}
