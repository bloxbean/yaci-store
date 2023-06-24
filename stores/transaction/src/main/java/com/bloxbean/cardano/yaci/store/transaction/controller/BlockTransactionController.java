package com.bloxbean.cardano.yaci.store.transaction.controller;

import com.bloxbean.cardano.yaci.store.transaction.domain.TransactionSummary;
import com.bloxbean.cardano.yaci.store.transaction.service.TransactionService;
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

@RestController
@RequestMapping("${apiPrefix}/blocks")
@RequiredArgsConstructor
@Slf4j
public class BlockTransactionController {
    private final TransactionService transactionService;

    @GetMapping("{block}/txs")
    public List<TransactionSummary> getTransactions(@PathVariable String block) {
        if (block == null || block.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Block hash / Block number is required");

        if (NumberUtils.isDigits(block)) {
            return transactionService.getTransactionsByBlockNumber(Long.parseLong(block));
        } else {
            return transactionService.getTransactionsByBlockHash(block);
        }
    }
}
