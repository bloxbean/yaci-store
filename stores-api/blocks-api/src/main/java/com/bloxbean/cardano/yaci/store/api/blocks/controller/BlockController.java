package com.bloxbean.cardano.yaci.store.api.blocks.controller;

import com.bloxbean.cardano.yaci.store.api.blocks.dto.BlockDto;
import com.bloxbean.cardano.yaci.store.api.blocks.dto.BlockDtoMapper;
import com.bloxbean.cardano.yaci.store.api.blocks.service.BlockService;
import com.bloxbean.cardano.yaci.store.blocks.domain.BlocksPage;
import com.bloxbean.cardano.yaci.store.blocks.domain.PoolBlock;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockCborStorageReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HexFormat;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Block Service")
@RequestMapping("${apiPrefix}/blocks")
@ConditionalOnExpression("${store.blocks.endpoints.block.enabled:true}")
public class BlockController {

    private final BlockService blockService;
    private final BlockDtoMapper dtoMapper;
    
    @Autowired(required = false)
    private BlockCborStorageReader blockCborStorageReader;

    @GetMapping("{numberOrHash}")
    @Operation(summary = "Block Information by Number or Hash", description = "Get block information by number or hash.")
    public ResponseEntity<BlockDto> getBlockByNumber(@PathVariable String numberOrHash) {
        BlockDto blockDto;
        if (NumberUtils.isParsable(numberOrHash)) {
            blockDto = blockService.getBlockByNumber(Long.parseLong(numberOrHash))
                    .map(block -> dtoMapper.toBlockDto(block)).orElse(null);
        } else {
            blockDto = blockService.getBlockByHash(numberOrHash)
                    .map(block -> dtoMapper.toBlockDto(block)).orElse(null);
        }

        if (blockDto == null) {
            return ResponseEntity.notFound().build();
        }

        var confirmation = blockService.getLatestBlock()
                .map(latestBlock -> latestBlock.getNumber() - blockDto.getNumber())
                .orElse(0L);

        var nextBlockHash = blockService.getBlockByNumber(blockDto.getNumber() + 1)
                .map(block -> block.getHash())
                .orElse(null);

        blockDto.setNextBlock(nextBlockHash);
        blockDto.setConfirmations(confirmation);

        return ResponseEntity.ok(blockDto);
    }

    @GetMapping("epoch/{epoch}")
    @Operation(summary = "Block List by Epoch", description = "Get blocks by epoch number.")
    public ResponseEntity<BlocksPage> getBlocksByEpoch(@PathVariable int epoch, @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                           @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count){
        int p = page;
        if (p > 0)
            p = p - 1;
        return ResponseEntity.ok(blockService.getBlocksByEpoch(epoch, p, count));
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
    
    @GetMapping(value = "{blockHash}/cbor", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(
        summary = "Block CBOR Data",
        description = "Get raw CBOR bytes of a block. " +
                     "This endpoint returns the original CBOR representation of the block, " +
                     "which can be used for verification and debugging. " +
                     "Note: This feature must be enabled via store.blocks.save-cbor=true"
    )
    public ResponseEntity<byte[]> getBlockCbor(
            @PathVariable 
            @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "Invalid block hash format") 
            String blockHash) {
        
        if (blockCborStorageReader == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, 
                "Block CBOR feature is not enabled"
            );
        }
        
        var blockCbor = blockCborStorageReader.getBlockCborByHash(blockHash)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, 
                    "Block CBOR data not found. " +
                    "Make sure store.blocks.save-cbor=true is enabled."
                ));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(blockCbor.getCborData().length);
        headers.set("Content-Disposition", "attachment; filename=\"" + blockHash + ".cbor\"");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(blockCbor.getCborData());
    }
    
    @GetMapping(value = "{blockHash}/cbor/hex", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
        summary = "Block CBOR Data (Hex Format)",
        description = "Get raw CBOR bytes of a block as hexadecimal string. " +
                     "This is useful for JSON-based clients that cannot handle binary data. " +
                     "Returns the CBOR data encoded as hex string. " +
                     "Note: This feature must be enabled via store.blocks.save-cbor=true"
    )
    public ResponseEntity<String> getBlockCborHex(
            @PathVariable 
            @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "Invalid block hash format") 
            String blockHash) {
        
        if (blockCborStorageReader == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, 
                "Block CBOR feature is not enabled"
            );
        }
        
        var blockCbor = blockCborStorageReader.getBlockCborByHash(blockHash)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, 
                    "Block CBOR data not found. " +
                    "Make sure store.blocks.save-cbor=true is enabled."
                ));
        
        String cborHex = HexFormat.of().formatHex(blockCbor.getCborData());
        return ResponseEntity.ok(cborHex);
    }

}
