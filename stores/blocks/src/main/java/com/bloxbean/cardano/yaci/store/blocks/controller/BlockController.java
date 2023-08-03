package com.bloxbean.cardano.yaci.store.blocks.controller;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;
import com.bloxbean.cardano.yaci.store.blocks.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController("BlockController")
@RequestMapping("${apiPrefix}/blocks")
@Slf4j
public class BlockController {

    private BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    @GetMapping("{numberOrHash}")
    @Operation(description = "Get block by number or hash")
    public Block getBlockByNumber(@PathVariable String numberOrHash) {
        if (NumberUtils.isParsable(numberOrHash)) {
            return blockService.getBlockByNumber(Long.parseLong(numberOrHash))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found"));
        } else {
            return blockService.getBlockByHash(numberOrHash)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found"));
        }
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

    @GetMapping("pool/{poolId}")
    @Operation(description = "Get block by slot leader and epoch")
    public List<PoolBlock> getBlocksBySlotLeaderEpoch(@PathVariable String poolId, @RequestParam int epoch) {
        return blockService.getBlocksBySlotLeaderEpoch(poolId, epoch);
    }

    @GetMapping("latest")
    @Operation(description = "Get latest block")
    public Block getLatestBlock() {
        return blockService.getLatestBlock()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found"));
    }

}
