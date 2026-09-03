package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.TipFinder;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@ReadOnly(false)
@Slf4j
public class TipFinderService {
    private final StoreProperties properties;

    public Mono<Tip> getTip() {
        long startNanos = System.nanoTime();
        if (log.isDebugEnabled())
            log.debug("Starting TipFinder host={}, port={}, thread={}",
                    properties.getCardanoHost(), properties.getCardanoPort(), Thread.currentThread().getName());
        TipFinder tipFinder = new TipFinder(properties.getCardanoHost(), properties.getCardanoPort(),
            Point.ORIGIN, properties.getProtocolMagic());
        tipFinder.start();

        return tipFinder.find()
                .doOnSubscribe(subscription -> {
                    if (log.isDebugEnabled())
                        log.debug("TipFinder find subscribed");
                })
                .doOnNext(tip -> {
                    if (log.isDebugEnabled())
                        log.debug("TipFinder found tip slot={}, hash={}, durationMs={}",
                                tip.getPoint().getSlot(), tip.getPoint().getHash(), elapsedMs(startNanos));
                })
                .doOnError(throwable ->
                        log.error("TipFinder failed after {} ms", elapsedMs(startNanos), throwable))
                .doFinally(signalType -> {
                    tipFinder.shutdown();
                    if (log.isDebugEnabled())
                        log.debug("TipFinder shutdown signal={}, durationMs={}", signalType, elapsedMs(startNanos));
                });
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
