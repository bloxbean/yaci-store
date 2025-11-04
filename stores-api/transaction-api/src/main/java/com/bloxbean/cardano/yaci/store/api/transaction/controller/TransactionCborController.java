package com.bloxbean.cardano.yaci.store.api.transaction.controller;

import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionCborStorageReader;
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
 * REST API for retrieving transaction CBOR data.
 * This is useful for transaction verification, multi-party protocols, and trustless validation.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Transaction CBOR Service", description = "Endpoints for retrieving raw transaction CBOR data")
@RequestMapping("${apiPrefix}/txs")
@ConditionalOnExpression("${store.transaction.endpoints.cbor.enabled:true}")
public class TransactionCborController {
    private final TransactionCborStorageReader transactionCborStorageReader;

    @GetMapping(value = "{txHash}/cbor", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(
        summary = "Transaction CBOR Data",
        description = "Get raw CBOR bytes of a transaction. " +
                     "This endpoint returns the original CBOR representation of the transaction body, " +
                     "which can be used for verification, multi-party protocols, and trustless validation. " +
                     "Note: This feature must be enabled via store.transaction.save-cbor=true"
    )
    public ResponseEntity<byte[]> getTransactionCbor(
            @PathVariable 
            @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "Invalid transaction hash format") 
            String txHash) {
        
        byte[] cborData = transactionCborStorageReader.getTxCborByHash(txHash)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, 
                    "Transaction CBOR data not found. " +
                    "Make sure store.transaction.save-cbor=true is enabled."
                ));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(cborData.length);
        headers.set("Content-Disposition", "attachment; filename=\"" + txHash + ".cbor\"");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(cborData);
    }
    
    @GetMapping("{txHash}/cbor/exists")
    @Operation(
        summary = "Check Transaction CBOR Existence",
        description = "Check if CBOR data exists for a specific transaction"
    )
    public ResponseEntity<Boolean> checkTransactionCborExists(
            @PathVariable 
            @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "Invalid transaction hash format") 
            String txHash) {
        
        boolean exists = transactionCborStorageReader.cborExists(txHash);
        return ResponseEntity.ok(exists);
    }
}


