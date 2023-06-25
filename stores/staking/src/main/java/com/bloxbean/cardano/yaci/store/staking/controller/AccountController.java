package com.bloxbean.cardano.yaci.store.staking.controller;

import com.bloxbean.cardano.yaci.store.staking.dto.StakeAccountInfo;
import com.bloxbean.cardano.yaci.store.staking.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("${apiPrefix}/accounts")
@ConditionalOnBean(AccountService.class)
@Slf4j
public class AccountController {
    private final AccountService accountService;

    @GetMapping("{stake_address}")
    @Operation(description = "Obtain information about a specific stake account")
    public StakeAccountInfo getStakeAccountInfo(@PathVariable("stake_address") String stakeAddress) {
       return accountService.getAccountInfo(stakeAddress)
               .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stake account not found"));
    }
}
