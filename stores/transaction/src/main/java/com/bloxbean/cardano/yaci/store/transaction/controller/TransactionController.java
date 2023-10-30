package com.bloxbean.cardano.yaci.store.transaction.controller;

import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionDetails;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionPage;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxInputsOutputs;
import com.bloxbean.cardano.yaci.store.transaction.domain.TxnWitness;
import com.bloxbean.cardano.yaci.store.transaction.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("${apiPrefix}/txs")
@Slf4j
public class TransactionController {
    private TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("{txHash}")
    public TransactionDetails getTransaction(@PathVariable String txHash) {
        return transactionService.getTransaction(txHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    @GetMapping("{txHash}/utxos")
    public TxInputsOutputs getTransactionInputsOutputs(@PathVariable String txHash) {
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
    public TransactionPage getTransactions(@RequestParam(name = "page", defaultValue = "0") int page,
                                           @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return transactionService.getTransactions(p, count);
    }

    @GetMapping("{txHash}/witnesses")
    public List<TxnWitness> getTransactionWitnesses(@PathVariable String txHash) {
        return transactionService.getTransactionWitnesses(txHash);
    }
}
