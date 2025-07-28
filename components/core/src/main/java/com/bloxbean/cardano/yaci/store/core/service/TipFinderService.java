package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.TipFinder;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.core.annotation.ReadOnly;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

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
}
