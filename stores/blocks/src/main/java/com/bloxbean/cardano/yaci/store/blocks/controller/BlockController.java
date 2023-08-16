package com.bloxbean.cardano.yaci.store.blocks.controller;

import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;
import com.bloxbean.cardano.yaci.store.blocks.dto.BlockDto;
import com.bloxbean.cardano.yaci.store.blocks.dto.BlockDtoMapper;
import com.bloxbean.cardano.yaci.store.blocks.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController("BlockController")
@RequestMapping("${apiPrefix}/blocks")
@RequiredArgsConstructor
@Slf4j
public class BlockController {
    private final BlockService blockService;
    private final BlockDtoMapper dtoMapper;

    @GetMapping("{numberOrHash}")
    @Operation(description = "Get block by number or hash")
    public BlockDto getBlockByNumber(@PathVariable String numberOrHash) {
        if (NumberUtils.isParsable(numberOrHash)) {
            return blockService.getBlockByNumber(Long.parseLong(numberOrHash))
                    .map(block -> dtoMapper.toBlockDto(block))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found"));
        } else {
            return blockService.getBlockByHash(numberOrHash)
                    .map(block -> dtoMapper.toBlockDto(block))
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
    public BlockDto getLatestBlock() {
        return blockService.getLatestBlock()
                .map(block -> dtoMapper.toBlockDto(block))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Block not found"));
    }

}
