package com.bloxbean.cardano.yaci.store.cip139.transaction.controller;

import com.bloxbean.cardano.yaci.store.cip139.transaction.dto.TransactionDto;
import com.bloxbean.cardano.yaci.store.cip139.transaction.service.Cip139TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "CIP-139 Transaction")
@RequestMapping("${cip139.apiPrefix}/transaction")
@ConditionalOnExpression("${store.extensions.cip139.transaction.enabled:true}")
public class Cip139TransactionController {

    private final Cip139TransactionService cip139TransactionService;

    @PostConstruct
    public void postConstruct() {
        log.info("CIP-139 TransactionController initialized >>>");
    }

    @GetMapping("hash")
    @Operation(summary = "Specific Transaction for a given Hash.", description = "Get the transaction with the supplied transaction hash.")
    public TransactionDto getTransactionByHash(@RequestParam(name = "transaction_hash") String hash) {
        return cip139TransactionService.getTransactionByHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found for hash: " + hash));
    }

}
