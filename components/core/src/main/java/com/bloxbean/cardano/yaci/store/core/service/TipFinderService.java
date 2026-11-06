package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.TipFinder;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@ReadOnly(false)
public class TipFinderService {
    private final StoreProperties properties;

    public Mono<Tip> getTip() {
        TipFinder tipFinder = new TipFinder(properties.getCardanoHost(), properties.getCardanoPort(),
            Point.ORIGIN, properties.getProtocolMagic());
        tipFinder.start();

        return tipFinder.find()
                .doFinally(signalType -> tipFinder.shutdown());
    }

    /**
     * Finds the tip, waiting at most {@code timeoutMs} (0 for no limit). Unlike {@code getTip().block(duration)},
     * the limit also covers the connection and the handshake, which yaci waits for with no limit.
     *
     * @throws NodeNotAnsweringException if the node did not answer in time
     */
    public Tip findTip(long timeoutMs) {
        AtomicReference<TipFinder> current = new AtomicReference<>();
        AtomicReference<CompletableFuture<Tip>> pending = new AtomicReference<>();
        AtomicBoolean cancelled = new AtomicBoolean();
        return NodeCalls.call("find the tip", timeoutMs, () -> {
            TipFinder tipFinder = new TipFinder(properties.getCardanoHost(), properties.getCardanoPort(),
                    Point.ORIGIN, properties.getProtocolMagic());
            current.set(tipFinder);
            try {
                tipFinder.start();
                CompletableFuture<Tip> tip = tipFinder.find().toFuture();
                pending.set(tip);
                // yaci may swallow the interrupt of a cancelled handshake: do not start waiting after a cancel
                if (cancelled.get())
                    throw new NodeNotAnsweringException("Cancelled: find the tip");
                return tip.get();
            } finally {
                tipFinder.shutdown();
            }
        }, () -> {
            cancelled.set(true);
            CompletableFuture<Tip> tip = pending.get();
            if (tip != null)
                tip.cancel(true);
            TipFinder tipFinder = current.get();
            if (tipFinder != null)
                tipFinder.shutdown();
        });
    }
}
