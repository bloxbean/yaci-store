package com.bloxbean.cardano.yaci.store.core.annotation;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Condition to check if store.governance.n2c-drep-stake-enabled property is set to true.
 * This is used to enable or disable local drep stake distribution in the store.
 */
public class LocalDRepStakeCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();

        String localEpochParamEnabled = env.getProperty("store.governance.n2c-drep-stake-enabled", "true");

        return localEpochParamEnabled.equals("true");
    }
}
