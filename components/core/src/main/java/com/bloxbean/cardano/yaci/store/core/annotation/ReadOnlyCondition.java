package com.bloxbean.cardano.yaci.store.core.annotation;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Map;

public class ReadOnlyCondition implements Condition {

    /**
     * Matches if the read-only mode specified in the annotation matches the read-only mode specified
     * in the environment.
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();

        boolean readOnly = Boolean.parseBoolean(env.getProperty("store.read-only-mode", "false"));

        Map<String, Object> attributes = metadata.getAnnotationAttributes(ReadOnly.class.getName());
        boolean expectedReadOnly = (boolean) attributes.getOrDefault("value", true);

        // Check if the read-only mode specified in the annotation matches the read-only mode specified
        // in the environment
        return readOnly == expectedReadOnly;
    }
}
