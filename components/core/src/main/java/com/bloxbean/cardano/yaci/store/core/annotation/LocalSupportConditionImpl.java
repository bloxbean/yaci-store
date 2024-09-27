package com.bloxbean.cardano.yaci.store.core.annotation;

import java.util.Map;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.env.Environment;

public class LocalSupportConditionImpl implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment env = context.getEnvironment();

        String n2cNodeSocketPath = env.getProperty("store.cardano.n2c-node-socket-path", "");
        String n2cHost = env.getProperty("store.cardano.n2c-host", "");
        String readOnlyMode = env.getProperty("store.read-only-mode", "false");
        boolean isReadOnly = "true".equals(readOnlyMode);

        boolean n2cEnabled = !n2cNodeSocketPath.isEmpty() || !n2cHost.isEmpty();

        Boolean checkProperty = checkProperty(metadata, env);

        return n2cEnabled && !isReadOnly && checkProperty;
    }

    private Boolean checkProperty(AnnotatedTypeMetadata metadata, Environment env) {
        // Get the prefix and name from annotation attributes
        Map<String, Object> mapValueByAttributes = metadata.getAnnotationAttributes(
            LocalSupportCondition.class.getName());
        if (mapValueByAttributes == null) {
            return false;
        }

        String prefix = (String) mapValueByAttributes.getOrDefault("prefix", "");
        String name = (String) mapValueByAttributes.getOrDefault("name", "");
        String havingValue = (String) mapValueByAttributes.getOrDefault("havingValue", "true");
        Boolean matchIfMissing = (Boolean) mapValueByAttributes.getOrDefault("matchIfMissing", true);

        String fullKey = prefix + "." + name;
        String propertyValue = env.getProperty(fullKey);

        return propertyValue == null || havingValue.equalsIgnoreCase(propertyValue);
    }
}
