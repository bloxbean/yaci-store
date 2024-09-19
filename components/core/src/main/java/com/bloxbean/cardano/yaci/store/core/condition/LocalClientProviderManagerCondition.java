package com.bloxbean.cardano.yaci.store.core.condition;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

@Slf4j
public class LocalClientProviderManagerCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        return  env.containsProperty("store.cardano.n2c-node-socket-path") || env.containsProperty("store.cardano.n2c-host");
    }
}
