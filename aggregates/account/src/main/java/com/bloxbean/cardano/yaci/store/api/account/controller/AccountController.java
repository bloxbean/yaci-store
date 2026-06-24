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
@Tag(name = "Account API",
        description = "Account-related queries for Cardano addresses and stake accounts. " +
                "Includes current and historical address balances, aggregated stake balances, live UTxO-based amounts, " +
                "and full stake-account details (controlled ADA, withdrawable rewards, delegated pool).\n\n" +
                "Depending on the endpoint, data is served from pre-aggregated balance tables, by live UTxO aggregation, " +
                "or by querying the connected local Cardano node (n2c). Endpoints whose required feature flag is " +
                "disabled return HTTP 503; see each endpoint description for specifics.")
public class AccountController {
    private final AccountBalanceStorage accountBalanceStorage;
    private final AccountService accountService;
    private final UtxoAccountService utxoAccountService;
    private final AccountStoreProperties accountStoreProperties;
    private final AccountConfigService accountConfigService;

    @GetMapping("/addresses/{address}/balance")
    @Operation(description =
            "Returns the current balance (lovelace and native assets) at a payment-type address.\n\n" +
            "Address: Bech32 payment address (`addr...`) — payment, base, enterprise, or pointer.\n\n" +
            "Data source: pre-aggregated address balance table.\n\n" +
            "Requires `store.account.balance-aggregation-enabled=true` AND `store.account.address-balance-enabled=true`; " +
            "otherwise returns HTTP 503.\n\n" +
            "Returns 404 if the address has no positive balance.")
    public Optional<AddressBalanceDto> getAddressBalance(@PathVariable @NonNull String address) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Balance aggregation is not enabled (store.account.balance-aggregation-enabled)");

        if (!accountStoreProperties.isAddressBalanceEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Address balance aggregation is not enabled (store.account.address-balance-enabled)");

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
    @Operation(description =
            "Returns the current aggregated lovelace balance for a stake account. " +
            "Withdrawable rewards are NOT included.\n\n" +
            "Address: Bech32 stake address (`stake...`).\n\n" +
            "Data source: pre-aggregated stake-address balance table.\n\n" +
            "Requires `store.account.balance-aggregation-enabled=true` AND " +
            "`store.account.stake-address-balance-enabled=true` " +
            "(the latter controls whether the stake-address balance table is populated). " +
            "If `balance-aggregation-enabled` is false, the endpoint returns HTTP 503.\n\n" +
            "Returns an empty body when no positive balance exists for the stake address.")
    public Optional<StakeAddressBalance> getStakeAddressBalance(@PathVariable @NonNull String stakeAddress) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new UnsupportedOperationException("Address balance aggregation is not enabled");

        return accountBalanceStorage.getStakeAddressBalance(stakeAddress)
                .filter(stakeAddrBalance -> stakeAddrBalance.getQuantity().compareTo(BigInteger.ZERO) > 0);
    }

    @GetMapping("/addresses/{address}/{unit}/{timeInSec}/balance")
    @Operation(description =
            "Returns the historical balance for a single asset `unit` at or before `timeInSec`.\n\n" +
            "Address: Bech32 payment address (`addr...`).\n" +
            "Unit: `lovelace` or hex `policyId+assetNameHex`.\n" +
            "timeInSec: UNIX seconds.\n\n" +
            "Data source: historical entries in the address balance table.\n\n" +
            "Requires `store.account.balance-aggregation-enabled=true` AND `store.account.address-balance-enabled=true`; " +
            "otherwise returns HTTP 503.\n\n" +
            "Returns 404 if no record exists at the requested time.")
    public Optional<AddressBalanceDto> getAddressBalanceAtTime(@PathVariable String address,@PathVariable String unit,@PathVariable long timeInSec) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Balance aggregation is not enabled (store.account.balance-aggregation-enabled)");

        if (!accountStoreProperties.isAddressBalanceEnabled())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Address balance aggregation is not enabled (store.account.address-balance-enabled)");

        var addressBalances = accountBalanceStorage.getAddressBalanceByTime(address, unit, timeInSec)
                .filter(addressBalance -> addressBalance.getQuantity().compareTo(BigInteger.ZERO) > 0)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Balance not found for the time"));

        return toAddressDto(addressBalances);
    }

    @GetMapping("/accounts/{stakeAddress}/{timeInSec}/balance")
    @Operation(description =
            "Returns the historical aggregated lovelace balance for a stake account at or before " +
            "`timeInSec`. Withdrawable rewards are NOT included.\n\n" +
            "Address: Bech32 stake address (`stake...`).\n" +
            "timeInSec: UNIX seconds.\n\n" +
            "Data source: historical entries in the stake-address balance table.\n\n" +
            "Requires `store.account.balance-aggregation-enabled=true` AND " +
            "`store.account.stake-address-balance-enabled=true` " +
            "(the latter controls whether the stake-address balance table is populated). " +
            "If `balance-aggregation-enabled` is false, the endpoint returns HTTP 503.\n\n" +
            "Returns 404 if no record exists at the requested time.")
    public StakeAddressBalance getStakeAddressBalanceAtTime(@PathVariable @NonNull String stakeAddress,
                                                            @PathVariable long timeInSec) {
        if (!accountStoreProperties.isBalanceAggregationEnabled())
            throw new UnsupportedOperationException("Address balance aggregation is not enabled");

        return accountBalanceStorage.getStakeAddressBalanceByTime(stakeAddress, timeInSec)
                .filter(stakeAddrBalance -> stakeAddrBalance.getQuantity().compareTo(BigInteger.ZERO) > 0)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Balance not found for the time"));

    }

    @GetMapping("/accounts/{stakeAddress}")
    @Operation(description =
            "Returns details for a stake account: `controlledAmount`, `withdrawableAmount`, and `poolId`.\n\n" +
            "Address: Bech32 stake address (`stake...`). Returns 400 for any other prefix.\n\n" +
            "`controlledAmount` = lovelace held by the stake credential + withdrawable rewards.\n\n" +
            "Lovelace source: pre-aggregated stake-address balance table when " +
            "`store.account.balance-aggregation-enabled=true`; otherwise computed at request time by summing all " +
            "current UTxOs for the stake address (may be slow for accounts with many UTxOs).\n\n" +
            "`withdrawableAmount` and `poolId`: obtained from the connected local Cardano node (n2c) via the " +
            "`DelegationsAndRewardAccounts` query, with fallback to the AdaPot reward provider when available. " +
            "If neither source is reachable, `poolId` is null and `withdrawableAmount` is 0.")
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
    @Operation(description =
            "Returns the live amounts (lovelace and native assets) held at an address by summing all current UTxOs " +
            "at request time.\n\n" +
            "Address: any Bech32 address — payment (`addr...`) or stake (`stake...`).\n" +
            "For stake addresses, only the lovelace entry is returned.\n\n" +
            "Data source: live UTxO aggregation. Does NOT depend on the balance-aggregation flags, so it works even " +
            "when address or stake balance aggregation is disabled. May be slow for addresses with very many UTxOs.")
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
