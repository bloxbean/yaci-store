package com.bloxbean.cardano.yaci.indexer.controller;

import com.bloxbean.cardano.yaci.indexer.model.Range;
import com.bloxbean.cardano.yaci.indexer.service.BlockFetchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/chain")
public class ChainController {

    private BlockFetchService blockFetchService;

    public ChainController(BlockFetchService blockFetchService) {
        this.blockFetchService = blockFetchService;
    }

    @PostMapping(value = "/fetch-block", consumes = "application/json")
    public Mono<Void> startFetchBlocks(@RequestBody Range range) {
        blockFetchService.startFetch(range.getFrom(), range.getTo());

        return Mono.empty();
    }
}
