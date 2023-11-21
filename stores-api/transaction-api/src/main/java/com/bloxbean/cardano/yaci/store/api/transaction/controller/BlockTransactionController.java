package com.bloxbean.cardano.yaci.store.api.transaction.controller;

import com.bloxbean.cardano.yaci.store.api.transaction.service.TransactionService;
import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Block Service")
@RequestMapping("${apiPrefix}/blocks")
public class BlockTransactionController {

    private final TransactionService transactionService;

    @GetMapping("{block}/txs")
    @Operation(summary = "Block Transactions", description = "Get a list of all transactions included in the provided block.")
    public List<TransactionSummary> getTransactions(@PathVariable String block) {
        if (block == null || block.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Block hash / Block number is required");
        }

        if (NumberUtils.isDigits(block)) {
            return transactionService.getTransactionsByBlockNumber(Long.parseLong(block));
        } else {
            return transactionService.getTransactionsByBlockHash(block);
        }
    }
}
