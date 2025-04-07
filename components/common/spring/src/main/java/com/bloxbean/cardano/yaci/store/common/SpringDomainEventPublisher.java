package com.bloxbean.cardano.yaci.store.common;

import com.bloxbean.cardano.yaci.store.events.api.DomainEventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public <T> void publishEvent(T event) {
        applicationEventPublisher.publishEvent(event);
    }
}
