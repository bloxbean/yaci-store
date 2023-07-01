package com.bloxbean.cardano.yaci.store.account.controller;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("AccountController")
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    private final AccountBalanceStorage accountBalanceStorage;

    @GetMapping("/addresses/{address}/balance")
    @Operation(description = "Get current balance at an address")
    public List<AddressBalance> getAddressBalance(String address) {
        return accountBalanceStorage.getAddressBalance(address);
    }

    @GetMapping("/accounts/{address}/balance")
    @Operation(description = "Get current balance at a stake address")
    public List<StakeAddressBalance> getStakeAddressBalance(String stakeAddr) {
        return accountBalanceStorage.getStakeAddressBalance(stakeAddr);
    }

}
