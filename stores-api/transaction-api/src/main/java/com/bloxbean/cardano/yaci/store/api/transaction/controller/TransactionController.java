package com.bloxbean.cardano.yaci.store.api.transaction.controller;

import com.bloxbean.cardano.yaci.store.api.transaction.dto.CborHexResponse;
import com.bloxbean.cardano.yaci.store.api.transaction.service.TransactionService;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionDetails;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionPage;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxInputsOutputs;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.storage.TransactionCborStorageReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
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
@Tag(name = "Transaction Service")
@RequestMapping("${apiPrefix}/txs")
@ConditionalOnExpression("${store.transaction.endpoints.transaction.enabled:true}")
public class TransactionController {
    private final TransactionService transactionService;
    
    @Autowired(required = false)
    private TransactionCborStorageReader transactionCborStorageReader;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("{txHash}")
    @Operation(summary = "Transaction Information", description = "Get detailed information about a specific transaction.")
    public TransactionDetails getTransaction(@PathVariable @Pattern(regexp = "^[0-9a-fA-F]{64}$") String txHash) {
        return transactionService.getTransaction(txHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    @GetMapping("{txHash}/utxos")
    @Operation(summary = "Transaction UTxOs", description = "Return the UTxOs of a specific transaction.")
    public TxInputsOutputs getTransactionInputsOutputs(@PathVariable @Pattern(regexp = "^[0-9a-fA-F]{64}$") String txHash) {
        return transactionService.getTransaction(txHash)
                .map(transactionDetails -> {
                    TxInputsOutputs txInputsOutputs = new TxInputsOutputs();
                    txInputsOutputs.setHash(txHash);
                    txInputsOutputs.setInputs(transactionDetails.getInputs());
                    txInputsOutputs.setOutputs(transactionDetails.getOutputs());
                    return txInputsOutputs;
                }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    @GetMapping
    @Operation(summary = "Transactions List", description = "Return list of transaction information by paging parameters.")
    public TransactionPage getTransactions(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                           @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return transactionService.getTransactions(p, count);
    }

    @GetMapping("{txHash}/witnesses")
    @Operation(summary = "Transaction Witnesses", description = "Return list of witnesses of a specific transaction.")
    public List<TxnWitness> getTransactionWitnesses(@PathVariable @Pattern(regexp = "^[0-9a-fA-F]{64}$") String txHash) {
        return transactionService.getTransactionWitnesses(txHash);
    }
    
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
        
        if (transactionCborStorageReader == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, 
                "Transaction CBOR feature is not enabled"
            );
        }
        
        var txnCbor = transactionCborStorageReader.getTxCborByHash(txHash)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, 
                    "Transaction CBOR data not found. " +
                    "Make sure store.transaction.save-cbor=true is enabled."
                ));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(txnCbor.getCborData().length);
        headers.set("Content-Disposition", "attachment; filename=\"" + txHash + ".cbor\"");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(txnCbor.getCborData());
    }
    
    @GetMapping(value = "{txHash}/cbor/hex", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Transaction CBOR Data (Hex Format)",
        description = "Get raw CBOR bytes of a transaction as hexadecimal string. " +
                     "This is useful for JSON-based clients that cannot handle binary data. " +
                     "Returns the CBOR data encoded as hex string inside JSON body. " +
                     "Note: This feature must be enabled via store.transaction.save-cbor=true"
    )
    public ResponseEntity<CborHexResponse> getTransactionCborHex(
            @PathVariable 
            @Pattern(regexp = "^[0-9a-fA-F]{64}$", message = "Invalid transaction hash format") 
            String txHash) {
        
        if (transactionCborStorageReader == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, 
                "Transaction CBOR feature is not enabled"
            );
        }
        
        var txnCbor = transactionCborStorageReader.getTxCborByHash(txHash)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, 
                    "Transaction CBOR data not found. " +
                    "Make sure store.transaction.save-cbor=true is enabled."
                ));
        
        String cborHex = HexFormat.of().formatHex(txnCbor.getCborData());
        return ResponseEntity.ok(CborHexResponse.builder().cborHex(cborHex).build());
    }
}
