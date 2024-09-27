package com.bloxbean.cardano.yaci.store.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Conditional;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(LocalSupportConditionImpl.class)
public @interface LocalSupportCondition {
    String prefix() default "store.governance";
    String name() default "n2c-gov-state-enabled";
    String havingValue() default "true";
    boolean matchIfMissing() default true;
}
