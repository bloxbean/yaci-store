package com.bloxbean.cardano.yaci.store.events.api;

public interface DomainEventPublisher {
    <T> void publishEvent(T event);
}
