package com.bloxbean.cardano.yaci.store.api.account.controller;

import com.bloxbean.cardano.client.api.util.AssetUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.account.AccountStoreProperties;
import com.bloxbean.cardano.yaci.store.account.domain.AddressBalance;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountInfo;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAccountRewardInfo;
import com.bloxbean.cardano.yaci.store.account.domain.StakeAddressBalance;
import com.bloxbean.cardano.yaci.store.account.service.AccountConfigService;
import com.bloxbean.cardano.yaci.store.account.storage.AccountBalanceStorage;
import com.bloxbean.cardano.yaci.store.account.util.ConfigIds;
import com.bloxbean.cardano.yaci.store.api.account.dto.AddressBalanceDto;
import com.bloxbean.cardano.yaci.store.api.account.service.AccountService;
import com.bloxbean.cardano.yaci.store.api.account.service.UtxoAccountService;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.common.util.Bech32Prefixes;
import com.bloxbean.cardano.yaci.store.common.util.StringUtil;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.core.util.Constants.LOVELACE;

@RestController("AccountController")
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Account API", description = "APIs for account related operations. This is an experimental module. Some apis may not be stable.")
public class AccountController {
    private final AccountBalanceStorage accountBalanceStorage;
    private final AccountService accountService;
    private final UtxoAccountService utxoAccountService;
    private final AccountStoreProperties accountStoreProperties;
    private final AccountConfigService accountConfigService;

    @GetMapping("/addresses/{address}/balance")
    @Operation(description = "Get current balance at an address")
    public Optional<AddressBalanceDto> getAddressBalance(@PathVariable @NonNull String address) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new UnsupportedOperationException("Address balance aggregation is not enabled");

        var addresssBalances = accountBalanceStorage.getAddressBalance(address)
                .stream()
                .filter(addressBalance -> addressBalance.getQuantity().compareTo(BigInteger.ZERO) > 0)
                .toList();

        if (addresssBalances.size() == 0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Balance not found");

        var addressBalanceDto = toAddressDto(addresssBalances);

        return addressBalanceDto;
    }

    @GetMapping("/accounts/{stakeAddress}/balance")
    @Operation(description = "Get current balance at a stake address")
    public Optional<StakeAddressBalance> getStakeAddressBalance(@PathVariable @NonNull String stakeAddress) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new UnsupportedOperationException("Address balance aggregation is not enabled");

        return accountBalanceStorage.getStakeAddressBalance(stakeAddress)
                .filter(stakeAddrBalance -> stakeAddrBalance.getQuantity().compareTo(BigInteger.ZERO) > 0);
    }

    @GetMapping("/addresses/{address}/{unit}/{timeInSec}/balance")
    @Operation(description = "Get current balance at an address at a specific time. This is an experimental feature.")
    public Optional<AddressBalanceDto> getAddressBalanceAtTime(@PathVariable String address,@PathVariable String unit,@PathVariable long timeInSec) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new UnsupportedOperationException("Address balance aggregation is not enabled");

        var addressBalances = accountBalanceStorage.getAddressBalanceByTime(address, unit, timeInSec)
                .filter(addressBalance -> addressBalance.getQuantity().compareTo(BigInteger.ZERO) > 0)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Balance not found for the time"));

        return toAddressDto(addressBalances);
    }

    @GetMapping("/accounts/{stakeAddress}/{timeInSec}/balance")
    @Operation(description = "Get current balance at a stake address at a specific time. This is an experimental feature.")
    public StakeAddressBalance getStakeAddressBalanceAtTime(@PathVariable @NonNull String stakeAddress,
                                                            @PathVariable long timeInSec) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new UnsupportedOperationException("Address balance aggregation is not enabled");

        return accountBalanceStorage.getStakeAddressBalanceByTime(stakeAddress, timeInSec)
                .filter(stakeAddrBalance -> stakeAddrBalance.getQuantity().compareTo(BigInteger.ZERO) > 0)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Balance not found for the time"));

    }

    @GetMapping("/accounts/{stakeAddress}")
    @Operation(description = "Obtain information about a specific stake account." +
            "It gets stake account balance from aggregated stake account balance if aggregation is enabled, " +
            "otherwise it calculates the current lovelace balance by aggregating all current utxos for the stake address" +
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

    private Optional<AddressBalanceDto> toAddressDto(List<AddressBalance> addresssBalances) {
        if (addresssBalances == null || addresssBalances.size() == 0)
            return Optional.empty();

        var amts = addresssBalances.stream()
                .map(addressBalance -> {
                    var policyIdAssetNameTuple = getPolicyIdAndAssetName(addressBalance.getUnit());
                    return Amt.builder()
                            .unit(addressBalance.getUnit())
                            .policyId(policyIdAssetNameTuple._1)
                            .assetName(policyIdAssetNameTuple._2)
                            .quantity(addressBalance.getQuantity())
                            .build();
                }).toList();

        Long lastTxBlock = 0L;
        Long lastTxSlot = 0L;
        Long lastTxBlockTime = 0L;
        for (AddressBalance addressBalance: addresssBalances) {
            if (addressBalance.getBlockNumber() > lastTxBlock) {
                lastTxBlock = addressBalance.getBlockNumber();
                lastTxSlot = addressBalance.getSlot();
                lastTxBlockTime = addressBalance.getBlockTime();
            }
        }

        var addressBalanceDto = new AddressBalanceDto();
        addressBalanceDto.setAddress(addresssBalances.get(0).getAddress());
        addressBalanceDto.setAmounts(amts);
        addressBalanceDto.setBlockNumber(lastTxBlock);
        addressBalanceDto.setSlot(lastTxSlot);
        addressBalanceDto.setBlockTime(lastTxBlockTime);

        accountConfigService.getConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK)
                .ifPresent(accountConfigEntity -> {
                    addressBalanceDto.setLastBalanceCalculationBlock(accountConfigEntity.getBlock());
                });

        return Optional.of(addressBalanceDto);
    }

    private Optional<AddressBalanceDto> toAddressDto(AddressBalance addressBalance) {
        if (addressBalance == null)
            return Optional.empty();

        var policyIdAssetName = getPolicyIdAndAssetName(addressBalance.getUnit());

        var amts =  Amt.builder()
                        .unit(addressBalance.getUnit())
                        .policyId(policyIdAssetName._1)
                        .assetName(policyIdAssetName._2)
                        .quantity(addressBalance.getQuantity())
                        .build();

        AddressBalanceDto addressBalanceDto = new AddressBalanceDto();
        addressBalanceDto.setAddress(addressBalance.getAddress());
        addressBalanceDto.setAmounts(List.of(amts));
        addressBalanceDto.setBlockNumber(addressBalance.getBlockNumber());
        addressBalanceDto.setSlot(addressBalance.getSlot());
        addressBalanceDto.setBlockTime(addressBalance.getBlockTime());

        accountConfigService.getConfig(ConfigIds.LAST_ACCOUNT_BALANCE_PROCESSED_BLOCK)
                .ifPresent(accountConfigEntity -> {
                    addressBalanceDto.setLastBalanceCalculationBlock(accountConfigEntity.getBlock());
                });

        return Optional.of(addressBalanceDto);
    }

    private Tuple<String, String> getPolicyIdAndAssetName(String unit) {
        if (unit == null || unit.equals(LOVELACE))
            return new Tuple<>(null, null);

        try {
            var policyAndAssetName = AssetUtil.getPolicyIdAndAssetName(unit);
            if (policyAndAssetName == null)
                return new Tuple<>(null, null);

            var assetNameBytes = HexUtil.decodeHexString(policyAndAssetName._2);
            var assetName = StringUtil.isValidUTF8(assetNameBytes)? new String(assetNameBytes, StandardCharsets.UTF_8): policyAndAssetName._2;

            return new Tuple<>(policyAndAssetName._1, assetName);
        } catch (Exception e) {
            return new Tuple<>(null, null);
        }
    }



}
