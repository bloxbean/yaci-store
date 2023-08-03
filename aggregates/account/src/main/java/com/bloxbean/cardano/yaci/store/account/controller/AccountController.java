package com.bloxbean.cardano.yaci.store.account.controller;

import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountInfo;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountRewardInfo;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.service.AccountService;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@RestController("AccountController")
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    private final AccountBalanceStorage accountBalanceStorage;
    private final AccountService accountService;

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

    @GetMapping("/accounts/{stakeAddress}")
    @Operation(description = "Obtain information about a specific stake account")
    public StakeAccountInfo getStakeAccountDetails(String stakeAddress) {
        List<StakeAddressBalance> stakeAddressBalances = accountBalanceStorage.getStakeAddressBalance(stakeAddress);
        BigInteger lovellaceBalance = BigInteger.ZERO;
        if (stakeAddressBalances != null && stakeAddressBalances.size() > 0) {
            lovellaceBalance = stakeAddressBalances.stream().filter(addressBalance -> addressBalance.getUnit().equals("lovelace"))
                    .findFirst()
                    .map(addressBalance -> addressBalance.getQuantity())
                    .orElse(BigInteger.ZERO);
        }

        Optional<StakeAccountRewardInfo> stakeAccountInfo = accountService.getAccountInfo(stakeAddress);
        BigInteger withdrawableRewards = stakeAccountInfo.map(StakeAccountRewardInfo::getWithdrawableAmount).orElse(BigInteger.ZERO);

        StakeAccountInfo stakeAccountDetails = new StakeAccountInfo();
        stakeAccountDetails.setStakeAddress(stakeAddress);
        stakeAccountDetails.setControlledAmount(lovellaceBalance.add(withdrawableRewards));
        stakeAccountDetails.setWithdrawableAmount(withdrawableRewards);
        stakeAccountDetails.setPoolId(stakeAccountInfo.map(StakeAccountRewardInfo::getPoolId).orElse(null));

        return stakeAccountDetails;
    }
}
