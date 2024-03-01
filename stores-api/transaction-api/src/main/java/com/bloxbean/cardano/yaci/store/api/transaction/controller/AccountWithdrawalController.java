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
@Tag(name = "Account Service")
@RequestMapping("${apiPrefix}/accounts")
@ConditionalOnExpression("${store.transaction.endpoints.transaction.enabled:true}")
@RequiredArgsConstructor
public class AccountWithdrawalController {
    private final WithdrawalStorageReader withdrawalStorageReader;

    @GetMapping("{stakeAddress}/withdrawals")
    @Operation(summary = "Get withdrawals by stake address", description = "Get withdrawals by stake address")
    public List<Withdrawal> getWithdrawalsByAccount(@PathVariable(name = "stakeAddress") String stakeAddress,
                                                    @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        return withdrawalStorageReader.getWithdrawalsByAddress(stakeAddress, p, count, Order.desc);
    }
}
