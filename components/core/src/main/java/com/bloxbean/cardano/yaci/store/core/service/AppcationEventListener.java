package com.bloxbean.cardano.yaci.store.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        value="sync.auto.start",
        havingValue = "true",
        matchIfMissing = true
)
@Slf4j
public class AppcationEventListener {
    private final StartService startService;

    @EventListener
    public void initialize(ApplicationReadyEvent applicationReadyEvent) {
        startService.start();
    }
}
