package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.TipFinder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
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
