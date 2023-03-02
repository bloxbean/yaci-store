package com.bloxbean.cardano.yaci.store.blocks.controller;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("${apiPrefix}/blocks")
@Slf4j
public class BlockController {

    private BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @GetMapping("{number}")
    @Operation(description = "Get block by number")
    public Block getBlockByNumber(@PathVariable long number) {
        return blockService.getBlockByNumber(number)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found"));
    }

    @GetMapping
    @Operation(description = "Get blocks by page number and count")
    public BlocksPage getBlocks(@RequestParam(name = "page", defaultValue = "0") int page,
                                @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return blockService.getBlocks(p, count);
    }

}
