package com.bloxbean.cardano.yaci.store.api.account.controller;

import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountInfo;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountRewardInfo;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.api.account.service.AccountService;
import com.bloxbean.cardano.yaci.store.api.account.service.UtxoAccountService;
import com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes;
import com.bloxbean.cardano.yaci.store.utxo.domain.Amount;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RestController("AccountController")
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "(Experimental) Account API", description = "APIs for account related operations. This is an experimental module. Some apis may not be stable.")
public class AccountController {
    private final AccountBalanceStorage accountBalanceStorage;
    private final AccountService accountService;
    private final UtxoAccountService utxoAccountService;
    private final AccountStoreProperties accountStoreProperties;

    @GetMapping("/addresses/{address}/balance")
    @Operation(description = "Get current balance at an address")
    public List<AddressBalance> getAddressBalance(String address) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new UnsupportedOperationException("Address balance aggregation is not enabled");

        return accountBalanceStorage.getAddressBalance(address)
                .stream()
                .filter(addressBalance -> addressBalance.getQuantity().compareTo(BigInteger.ZERO) > 0)
                .toList();
    }

    @GetMapping("/accounts/{stakeAddress}/balance")
    @Operation(description = "Get current balance at a stake address")
    public Optional<StakeAddressBalance> getStakeAddressBalance(String stakeAddress) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new UnsupportedOperationException("Address balance aggregation is not enabled");

        return accountBalanceStorage.getStakeAddressBalance(stakeAddress)
                .filter(stakeAddrBalance -> stakeAddrBalance.getQuantity().compareTo(BigInteger.ZERO) > 0);
    }

    @GetMapping("/addresses/{address}/{unit}/balance")
    @Operation(description = "Get current balance at an address at a specific time. This is an experimental feature.")
    public AddressBalance getAddressBalanceAtTime(String address, String unit,
                                                        @RequestParam long timeInSec) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new UnsupportedOperationException("Address balance aggregation is not enabled");

        return accountBalanceStorage.getAddressBalanceByTime(address, unit, timeInSec)
                .filter(addressBalance -> addressBalance.getQuantity().compareTo(BigInteger.ZERO) > 0)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Balance not found for the time"));
    }

    @GetMapping("/accounts/{stakeAddress}/{unit}/balance")
    @Operation(description = "Get current balance at a stake address at a specific time. This is an experimental feature.")
    public StakeAddressBalance getStakeAddressBalanceAtTime(String stakeAddress, String unit,
                                                        @RequestParam long timeInSec) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new UnsupportedOperationException("Address balance aggregation is not enabled");

        return accountBalanceStorage.getStakeAddressBalanceByTime(stakeAddress, timeInSec)
                .filter(stakeAddrBalance -> stakeAddrBalance.getQuantity().compareTo(BigInteger.ZERO) > 0)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Balance not found for the time"));

    }

    @GetMapping("/accounts/{stakeAddress}")
    @Operation(description = "Obtain information about a specific stake account." +
            "It calculates the current lovelace balance at the stake address by aggregating all currrent utxos for the stake address" +
            "and get rewards amount directly from node.")
    public StakeAccountInfo getStakeAccountDetails(@PathVariable @NonNull String stakeAddress) {
        if (!stakeAddress.startsWith(Bech32Prefixes.STAKE_ADDR_PREFIX))
            throw new IllegalArgumentException("Invalid stake address");

        BigInteger lovellaceBalance = BigInteger.ZERO;
        if (accountStoreProperties.isBalanceAggregationEnabled()) {
            Optional<StakeAddressBalance> stakeAddressBalances = accountBalanceStorage.getStakeAddressBalance(stakeAddress);
            lovellaceBalance = stakeAddressBalances
                    .map(addressBalance -> addressBalance.getQuantity())
                    .orElse(BigInteger.ZERO);
        } else { //Do run time aggregation
            List<Amount> amounts = utxoAccountService.getAmountsAtAddress(stakeAddress);
            if (amounts != null && amounts.size() > 0) {
                lovellaceBalance = amounts.stream().filter(amount -> amount.getUnit().equals("lovelace"))
                        .findFirst()
                        .map(amount -> amount.getQuantity())
                        .orElse(BigInteger.ZERO);
            }
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

    @GetMapping("/addresses/{address}/amounts")
    @Operation(description = "Get amounts at an address. For stake address, only lovelace is returned." +
            "It calculates the current balance at the address by aggregating all currrent utxos for the stake address. It may be slow for addresses with too many utxos.")
    public List<Amount> getAddressAmounts(@PathVariable @NonNull String address) {
        List<Amount> amounts = utxoAccountService.getAmountsAtAddress(address);
        if (amounts == null || amounts.size() == 0)
            return Collections.emptyList();

        if (address.startsWith(Bech32Prefixes.STAKE_ADDR_PREFIX)) { //For stake address, return only lovelace
            return amounts.stream().filter(amount -> amount.getUnit().equals("lovelace"))
                    .toList();
        } else {
            return amounts;
        }
    }
}
