package com.bloxbean.cardano.yaci.store.core.annotation;

import java.lang.annotation.*;

import org.springframework.context.annotation.Conditional;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(LocalSupportCondition.class)
public @interface LocalSupport {
}
