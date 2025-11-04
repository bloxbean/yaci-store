package com.bloxbean.cardano.yaci.store.api.blocks.controller;

import com.bloxbean.cardano.yaci.store.blocks.storage.BlockCborStorageReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST API for retrieving block CBOR data.
 * This is useful for block verification and debugging.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Block CBOR Service", description = "Endpoints for retrieving raw block CBOR data")
@RequestMapping("${apiPrefix}/blocks")
@ConditionalOnExpression("${store.blocks.endpoints.cbor.enabled:true}")
public class BlockCborController {
    private final BlockCborStorageReader blockCborStorageReader;

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
        
        byte[] cborData = blockCborStorageReader.getBlockCborByHash(blockHash)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, 
                    "Block CBOR data not found. " +
                    "Make sure store.blocks.save-cbor=true is enabled."
                ));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(cborData.length);
        headers.set("Content-Disposition", "attachment; filename=\"" + blockHash + ".cbor\"");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(cborData);
    }
    
    @GetMapping("{blockHash}/cbor/exists")
    @Operation(
        summary = "Check Block CBOR Existence",
        description = "Check if CBOR data exists for a specific block"
    )
    public ResponseEntity<Boolean> checkBlockCborExists(
            @PathVariable 
            @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "Invalid block hash format") 
            String blockHash) {
        
        boolean exists = blockCborStorageReader.cborExists(blockHash);
        return ResponseEntity.ok(exists);
    }
}


