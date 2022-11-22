package com.bloxbean.cardano.yaci.indexer.service;

import com.bloxbean.cardano.yaci.core.helpers.TipFinder;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
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
