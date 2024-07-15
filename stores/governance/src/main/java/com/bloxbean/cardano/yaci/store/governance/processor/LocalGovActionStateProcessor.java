package com.bloxbean.cardano.yaci.store.governance.processor;

import com.bloxbean.cardano.yaci.store.events.EpochChangeEvent;
import com.bloxbean.cardano.yaci.store.governance.service.LocalGovStateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(LocalGovStateService.class)
public class LocalGovActionStateProcessor {
    private final LocalGovStateService localGovStateService;

    @EventListener
    @Transactional
    public void handleEpochChangeEvent(EpochChangeEvent epochChangeEvent) {
//        if (epochChangeEvent.getEventMetadata().isSyncMode()) {
//            localGovStateService.fetchAndSetGovState();
//        }
        localGovStateService.fetchAndSetGovState();
    }
}
