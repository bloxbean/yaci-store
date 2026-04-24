package com.bloxbean.cardano.yaci.store.blockfrost.account.controller;

import com.bloxbean.cardano.yaci.store.blockfrost.account.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.account.service.BFAccountService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Accounts")
@RequestMapping("${blockfrost.apiPrefix}/accounts")
@ConditionalOnExpression("${store.extensions.blockfrost.account.enabled:false}")
public class BFAccountController {

    private final BFAccountService bfAccountService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost AccountController initialized >>>");
    }

    @GetMapping("{stake_address}")
    @Operation(summary = "Specific account address", description = "Obtain information about a specific stake account.")
    public BFAccountContentDto getAccountInfo(@PathVariable("stake_address") String stakeAddress) {
        return bfAccountService.getAccountInfo(stakeAddress);
    }

    @GetMapping("{stake_address}/rewards")
    @Operation(summary = "Account reward history", description = "Obtain information about the reward history of a specific account.")
    public List<BFAccountRewardDto> getAccountRewards(
            @PathVariable("stake_address") String stakeAddress,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAccountService.findRewards(stakeAddress, p, count, order);
    }

    @GetMapping("{stake_address}/history")
    @Operation(summary = "Account history", description = "Obtain information about the history of a specific account.")
    public List<BFAccountHistoryDto> getAccountHistory(
            @PathVariable("stake_address") String stakeAddress,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAccountService.findHistory(stakeAddress, p, count, order);
    }

    @GetMapping("{stake_address}/delegations")
    @Operation(summary = "Account delegation history", description = "Obtain information about the delegation of a specific account.")
    public List<BFAccountDelegationDto> getAccountDelegations(
            @PathVariable("stake_address") String stakeAddress,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAccountService.findDelegations(stakeAddress, p, count, order);
    }

    @GetMapping("{stake_address}/registrations")
    @Operation(summary = "Account registration history", description = "Obtain information about the registrations and de-registrations of a specific account.")
    public List<BFAccountRegistrationDto> getAccountRegistrations(
            @PathVariable("stake_address") String stakeAddress,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAccountService.findRegistrations(stakeAddress, p, count, order);
    }

    @GetMapping("{stake_address}/withdrawals")
    @Operation(summary = "Account withdrawal history", description = "Obtain information about the withdrawals of a specific account.")
    public List<BFAccountWithdrawalDto> getAccountWithdrawals(
            @PathVariable("stake_address") String stakeAddress,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAccountService.findWithdrawals(stakeAddress, p, count, order);
    }

    @GetMapping("{stake_address}/mirs")
    @Operation(summary = "Account MIR history", description = "Obtain information about the MIRs of a specific account.")
    public List<BFAccountMirDto> getAccountMirs(
            @PathVariable("stake_address") String stakeAddress,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAccountService.findMIRs(stakeAddress, p, count, order);
    }

    @GetMapping("{stake_address}/addresses")
    @Operation(summary = "Account associated addresses", description = "Obtain information about the addresses of a specific account.")
    public List<BFAccountAddressDto> getAccountAddresses(
            @PathVariable("stake_address") String stakeAddress,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAccountService.findAddresses(stakeAddress, p, count, order);
    }

    @GetMapping("{stake_address}/addresses/assets")
    @Operation(summary = "Assets associated with the account addresses", description = "Obtain information about assets associated with addresses of a specific account.")
    public List<BFAccountAddressAssetDto> getAccountAddressAssets(
            @PathVariable("stake_address") String stakeAddress,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAccountService.findAddressAssets(stakeAddress, p, count, order);
    }

    @GetMapping("{stake_address}/addresses/total")
    @Operation(summary = "Detailed information about account associated addresses", description = "Obtain summed details about all addresses associated with a given account.")
    public BFAccountAddressesTotalDto getAccountAddressesTotal(@PathVariable("stake_address") String stakeAddress) {
        return bfAccountService.getAddressesTotal(stakeAddress);
    }

    @GetMapping("{stake_address}/utxos")
    @Operation(summary = "Account associated UTXOs", description = "Obtain information about the UTXOs of a specific account.")
    public List<BFAccountUtxoDto> getAccountUtxos(
            @PathVariable("stake_address") String stakeAddress,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order) {
        int p = page - 1;
        return bfAccountService.findUtxos(stakeAddress, p, count, order);
    }

    @GetMapping("{stake_address}/transactions")
    @Operation(summary = "Account associated transactions", description = "Obtain information about the transactions of a specific account.")
    public List<BFAccountTransactionDto> getAccountTransactions(
            @PathVariable("stake_address") String stakeAddress,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") Order order,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        int p = page - 1;
        return bfAccountService.findTransactions(stakeAddress, p, count, order, from, to);
    }
}
