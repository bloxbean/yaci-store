package com.bloxbean.cardano.yaci.store.blocks.controller;

import com.bloxbean.cardano.yaci.store.blocks.dto.BlockDetails;
import com.bloxbean.cardano.yaci.store.blocks.dto.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.service.BlockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/blocks")
@Slf4j
public class BlockController {

    private BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @GetMapping("{number}")
    public BlockDetails getBlockByNumber(@PathVariable long number) {
        return blockService.getBlockByNumber(number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found"));
    }

    @GetMapping
    public BlocksPage getTransactions(@RequestParam(name = "page", defaultValue = "0") int page,
                                            @RequestParam(name = "count", defaultValue = "10") int count) {
        return blockService.getBlocks(page, count);
    }

}
