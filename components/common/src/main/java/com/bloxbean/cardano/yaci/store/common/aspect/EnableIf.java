package com.bloxbean.cardano.yaci.store.common.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to trigger the {@link EnableIfAspect} aspect, which is required for the GraalVM Native Image to work with the `store.{store_name}.enabled` flag at runtime.
 * During the native image build time, all store-specific flags are enabled, and this annotation controls runtime behavior,
 * as the default Conditional annotation does not work with the native image at runtime.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnableIf {
    String value();
    boolean defaultValue() default true;
}
