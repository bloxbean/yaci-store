package com.bloxbean.cardano.yaci.store.transaction.controller;

import com.bloxbean.cardano.yaci.store.transaction.dto.TransactionDetails;
import com.bloxbean.cardano.yaci.store.transaction.dto.TransactionPage;
import com.bloxbean.cardano.yaci.store.transaction.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Optional;

@RestController
@RequestMapping("/txs")
@Slf4j
public class TransactionController {
    private TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("{txHash}")
    public Mono<TransactionDetails> getTransaction(@PathVariable String txHash) {
        Optional<TransactionDetails> txDtlsOptional = transactionService.getTransaction(txHash);
        if (txDtlsOptional.isPresent())
            return Mono.just(txDtlsOptional.get());
         else
             return notFound();
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Mono<TransactionDetails> notFound() {
        return Mono.empty();
    }


    @GetMapping
    public Mono<TransactionPage> getTransactions(@RequestParam(name = "page", defaultValue = "0") int page,
                                                 @RequestParam(name = "count", defaultValue = "10") int count) {
        return Mono.just(transactionService.getTransactions(page, count));
    }
}
