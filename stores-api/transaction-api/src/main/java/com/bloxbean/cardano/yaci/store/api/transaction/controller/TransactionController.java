package com.bloxbean.cardano.yaci.store.api.transaction.controller;

import com.bloxbean.cardano.yaci.store.api.transaction.service.TransactionService;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionDetails;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionPage;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxInputsOutputs;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@Tag(name = "Transaction Service")
@RequestMapping("${apiPrefix}/txs")
public class TransactionController {
    private final TransactionService transactionService;

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
}
