package com.bloxbean.cardano.yaci.store.core.processor;

import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
import com.bloxbean.cardano.yaci.store.events.ErrorEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ErrorEventProcessor {
    private final ErrorStorage errorStorage;

    @EventListener
    @Transactional
    public void handleErrorEvent(ErrorEvent errorEvent) {
        try {
            errorStorage.save(errorEvent);
        } catch (Exception e) {
            log.error("Error while processing ErrorEvent: {}", errorEvent, e);
        }
    }
}
