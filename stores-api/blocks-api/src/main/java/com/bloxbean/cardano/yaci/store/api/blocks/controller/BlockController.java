package com.bloxbean.cardano.yaci.store.api.blocks.controller;

import com.bloxbean.cardano.yaci.store.api.blocks.dto.BlockDto;
import com.bloxbean.cardano.yaci.store.api.blocks.dto.BlockDtoMapper;
import com.bloxbean.cardano.yaci.store.api.blocks.service.BlockService;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Block Service")
@RequestMapping("${apiPrefix}/blocks")
@ConditionalOnExpression("${store.blocks.endpoints.blocks.enabled:true}")
public class BlockController {

    private final BlockService blockService;
    private final BlockDtoMapper dtoMapper;

    @GetMapping("{numberOrHash}")
    @Operation(summary = "Block Information by Number or Hash", description = "Get block information by number or hash.")
    public ResponseEntity<BlockDto> getBlockByNumber(@PathVariable String numberOrHash) {
        if (NumberUtils.isParsable(numberOrHash)) {
            return blockService.getBlockByNumber(Long.parseLong(numberOrHash))
                    .map(block -> ResponseEntity.ok(dtoMapper.toBlockDto(block)))
                    .orElse(ResponseEntity.notFound().build());
        } else {
            return blockService.getBlockByHash(numberOrHash)
                    .map(block -> ResponseEntity.ok(dtoMapper.toBlockDto(block)))
                    .orElse(ResponseEntity.notFound().build());
        }
    }

    @GetMapping
    @Operation(summary = "Block List", description = "Get blocks by page number and count.")
    public ResponseEntity<BlocksPage> getBlocks(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return ResponseEntity.ok(blockService.getBlocks(p, count));
    }

    @GetMapping("pool/{poolId}")
    @Operation(summary = "Slot Leader Block List", description = "Get blocks of slot leader in a specific epoch.")
    public ResponseEntity<List<PoolBlock>> getBlocksBySlotLeaderEpoch(@PathVariable String poolId, @RequestParam int epoch) {
        return ResponseEntity.ok(blockService.getBlocksBySlotLeaderEpoch(poolId, epoch));
    }

    @GetMapping("latest")
    @Operation(summary = "Latest Block", description = "Get the Latest Block Information.")
    public ResponseEntity<BlockDto> getLatestBlock() {
        return blockService.getLatestBlock()
                .map(block -> ResponseEntity.ok(dtoMapper.toBlockDto(block)))
                .orElse(ResponseEntity.notFound().build());
    }

}
