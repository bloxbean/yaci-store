package com.bloxbean.cardano.yaci.store.governance.annotation;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition to check if store.governance.n2c-gov-state-enabled property is set to true.
 * This is used to enable or disable local governance state in the store.
 */
public class LocalGovStateCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();
        String govStateEnabled = env.getProperty("store.governance.n2c-gov-state-enabled", "true");

        return govStateEnabled.equals("true");
    }
}
