package com.bloxbean.cardano.yaci.store.service;

import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

@RequiredArgsConstructor
@Slf4j
public class ApplicationStartListener {
    private final LocalClientProvider localClientProvider;

    @EventListener
    @Async
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (localClientProvider != null && !localClientProvider.isRunning()) {
            log.info("LocalClientProvider Started ---->>");
            localClientProvider.start();
        }
    }

    @EventListener
    @Async
    public void onApplicationEvent(ContextClosedEvent event) {
        if (localClientProvider != null) {
            log.info("LocalClientProvider Stopped ---->>");
            localClientProvider.shutdown();
        }
    }
}
