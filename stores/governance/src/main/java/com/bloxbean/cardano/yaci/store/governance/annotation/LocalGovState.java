package com.bloxbean.cardano.yaci.store.governance.annotation;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(LocalGovStateCondition.class)
public @interface LocalGovState {
}
