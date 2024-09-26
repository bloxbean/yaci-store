package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.TipFinder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@ConditionalOnProperty(
        prefix = "store",
        name = "read-only-mode",
        havingValue = "false",
        matchIfMissing = true
)
public class TipFinderService {
    private TipFinder tipFinder;

    public TipFinderService(TipFinder tipFinder) {
        //tipFinder.start();
        this.tipFinder = tipFinder;
    }

    public Mono<Tip> getTip() {
        return this.tipFinder.find();
    }
}
