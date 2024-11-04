package com.bloxbean.cardano.yaci.store.epoch.annotation;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition to check if store.epoch.n2c-epoch-param-enabled property is set to true.
 * This is used to enable or disable local epoch parameters in the store.
 */
public class LocalEpochParamCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();

        String localEpochParamEnabled = env.getProperty("store.epoch.n2c-epoch-param-enabled", "true");

        return localEpochParamEnabled.equals("true");
    }
}
