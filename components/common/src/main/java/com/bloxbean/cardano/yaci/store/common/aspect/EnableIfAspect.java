package com.bloxbean.cardano.yaci.store.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * This aspect is required for the GraalVM Native Image to work with the `store.{store_name}.enabled` flag at runtime.
 * During the native image build time, all store-specific flags are enabled, and this aspect controls runtime behavior,
 * as the default Conditional annotation does not work with the native image at runtime.
 */
@Aspect
@Component
@Slf4j
public class EnableIfAspect {

    @Autowired
    private Environment env;

    @Around("execution(public * *(..)) && @within(conditionalEventListener)")
    public Object handleConditionalEvent(ProceedingJoinPoint joinPoint, EnableIf conditionalEventListener) throws Throwable {
        Boolean enabled = env.getProperty(conditionalEventListener.value(), Boolean.class);
        if ((enabled == null && !conditionalEventListener.defaultValue())
                || (enabled != null && !enabled)) {
            return null;
        }

        return joinPoint.proceed();
    }
}

