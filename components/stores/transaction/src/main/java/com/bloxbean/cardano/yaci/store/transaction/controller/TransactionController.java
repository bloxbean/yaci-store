package com.bloxbean.cardano.yaci.store.transaction.controller;

import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionDetails;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionPage;
import com.bloxbean.cardano.yaci.store.transaction.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/txs")
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

    @GetMapping
    public TransactionPage getTransactions(@RequestParam(name = "page", defaultValue = "0") int page,
                                           @RequestParam(name = "count", defaultValue = "10") int count) {
        return transactionService.getTransactions(page, count);
    }
}
