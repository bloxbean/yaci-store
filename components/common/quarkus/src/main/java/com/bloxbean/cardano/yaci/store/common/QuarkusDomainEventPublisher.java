package com.bloxbean.cardano.yaci.store.common;

import com.bloxbean.cardano.yaci.store.events.api.DomainEventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class QuarkusDomainEventPublisher implements DomainEventPublisher {

    @Inject
    jakarta.enterprise.event.Event<Object> quarkusEvent;

    @Override
    public <T> void publishEvent(T event) {
        quarkusEvent.fire(event);
    }
}

