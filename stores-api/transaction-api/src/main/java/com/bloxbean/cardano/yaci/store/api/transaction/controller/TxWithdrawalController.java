package com.bloxbean.cardano.yaci.store.api.transaction.controller;

import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.transaction.domain.Withdrawal;
import com.bloxbean.cardano.yaci.store.transaction.storage.WithdrawalStorageReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@Tag(name = "Transaction Service")
@RequestMapping("${apiPrefix}/txs")
@ConditionalOnExpression("${store.transaction.endpoints.transaction.enabled:true}")
@RequiredArgsConstructor
public class TxWithdrawalController {
    private final WithdrawalStorageReader withdrawalStorageReader;

    @GetMapping("withdrawals")
    @Operation(summary = "Get withdrawals", description = "Get withdrawals")
    public List<Withdrawal> getWithdrawals(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                           @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        return withdrawalStorageReader.getWithdrawals(p, count, Order.desc);
    }

    @GetMapping("{txHash}/withdrawals")
    @Operation(summary = "Get withdrawals by transaction", description = "Get withdrawals by transaction")
    public List<Withdrawal> getWithdrawalsByTransaction(@PathVariable(name = "txHash") String txHash) {
        return withdrawalStorageReader.getWithdrawalsByTxHash(txHash);
    }

}
