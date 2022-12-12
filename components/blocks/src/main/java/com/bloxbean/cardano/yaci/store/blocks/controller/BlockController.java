package com.bloxbean.cardano.yaci.store.blocks.controller;

import com.bloxbean.cardano.yaci.store.blocks.dto.BlockDetails;
import com.bloxbean.cardano.yaci.store.blocks.dto.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.service.BlockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/blocks")
@Slf4j
public class BlockController {

    private BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @GetMapping("{number}")
    public Mono<BlockDetails> getBlockByNumber(@PathVariable long number) {
        return blockService.getBlockByNumber(number)
                .map(blockDetails -> Mono.just(blockDetails))
                .orElse(notFound());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<BlockDetails> notFound() {
        return Mono.empty();
    }

    @GetMapping
    public Mono<BlocksPage> getTransactions(@RequestParam(name = "page", defaultValue = "0") int page,
                                            @RequestParam(name = "count", defaultValue = "10") int count) {
        return Mono.just(blockService.getBlocks(page, count));
    }

}
