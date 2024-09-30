package com.bloxbean.cardano.yaci.store.epoch.annotation;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(LocalEpochParamCondition.class)
public @interface LocalEpochParam {
}
