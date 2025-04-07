package com.bloxbean.cardano.yaci.store.events.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface DomainEventListener {
}
