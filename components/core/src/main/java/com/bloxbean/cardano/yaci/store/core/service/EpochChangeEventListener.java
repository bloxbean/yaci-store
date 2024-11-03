package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.events.internal.EpochTransitionCommitEvent;
import com.bloxbean.cardano.yaci.store.events.internal.PreEpochTransitionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener for handling epoch change events and publish two separate events
 * <p>
 * 1. PreEpochTransitionEvent
 * </p>
 * <p>
 * 2. EpochTransitionCommitEvent
 * </p>
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EpochChangeEventListener {
    private final ApplicationEventPublisher publisher;

    @EventListener
    @Transactional
    public void onEpochChange(EpochChangeEvent epochChangeEvent) {

        //Publish epoch transition individual calculation events
        var preEpochTransitionEvent = PreEpochTransitionEvent.builder()
                .metadata(epochChangeEvent.getEventMetadata())
                .previousEpoch(epochChangeEvent.getPreviousEpoch())
                .epoch(epochChangeEvent.getEpoch())
                .previousEra(epochChangeEvent.getPreviousEra())
                .era(epochChangeEvent.getEra())
                .build();

        publisher.publishEvent(preEpochTransitionEvent);

        //Publish epoch transition commit event
        var epochTransitionCommitEvent = EpochTransitionCommitEvent.builder()
                .metadata(epochChangeEvent.getEventMetadata())
                .previousEpoch(epochChangeEvent.getPreviousEpoch())
                .epoch(epochChangeEvent.getEpoch())
                .previousEra(epochChangeEvent.getPreviousEra())
                .era(epochChangeEvent.getEra())
                .build();

        publisher.publishEvent(epochTransitionCommitEvent);
    }
}
