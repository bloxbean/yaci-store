package com.bloxbean.cardano.yaci.store.api.adapot.controller;

import com.bloxbean.cardano.yaci.store.api.adapot.dto.*;
import com.bloxbean.cardano.yaci.store.api.adapot.service.RewardApiService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.ControllerPageUtil.adjustPage;

@RestController("RewardController")
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reward API", description = "APIs for adapot related data.")
public class RewardController {

    private final RewardApiService accountRewardApiService;

    //-- Account Rewards
    @GetMapping("/accounts/{address}/rewards")
    @Operation(summary = "Get account pool rewards", description = "Retrieve rewards for a specific account by address with pagination and sorting.")
    public ResponseEntity<List<AddressPoolRewardDto>> getAccountRewards(@PathVariable String address, @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                                                        @RequestParam(required = false, defaultValue = "0") @Min(0) int page, @RequestParam(required = false, defaultValue = "desc") Order order) {
        int p = adjustPage(page);
        return ResponseEntity.ok(accountRewardApiService.getPoolRewards(address, p, count, order));
    }

    @GetMapping("/accounts/{address}/epochs/{epoch}/rewards")
    @Operation(summary = "Get account epoch pool rewards", description = "Retrieve rewards for a specific account and epoch by address and spendable epoch.")
    public ResponseEntity<List<AddressPoolRewardDto>> getAccountEpochRewards(@PathVariable String address, @PathVariable Integer epoch) {
        return ResponseEntity.ok(accountRewardApiService.getPoolRewardsBySpendableEpoch(address, epoch));
    }

    @GetMapping("/accounts/{address}/reward_rest")
    @Operation(summary = "Get account reward rest", description = "Retrieve non-pool rewards by address.")
    public ResponseEntity<List<AddressRewardRestDto>> getAccountRewardRest(@PathVariable String address, @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                                                           @RequestParam(required = false, defaultValue = "0") @Min(0) int page, @RequestParam(required = false, defaultValue = "desc") Order order) {
        int p = adjustPage(page);
        return ResponseEntity.ok(accountRewardApiService.getRewardRest(address, p, count, order));
    }

    @Operation(summary = "Get account epoch reward rest", description = "Retrieve non-pool rewards by address and spendable epoch.")
    @GetMapping("/accounts/{address}/epochs/{epoch}/reward_rest")
    public ResponseEntity<List<AddressRewardRestDto>> getAccountEpochRewardRest(@PathVariable String address, @PathVariable Integer epoch) {
        return ResponseEntity.ok(accountRewardApiService.getRewardRestBySpendableEpoch(address, epoch));
    }

    //-- Unclaimed Rewards
    @Operation(summary = "Get unclaimed reward rest for an epoch (Spendable Epoch)", description = "Retrieve unclaimed rewards by spendable epoch with pagination and sorting.")
    @GetMapping("/epochs/{epoch}/unclaimed_reward_rest")
    public ResponseEntity<?> getUnclaimedRewardRest(@PathVariable Integer epoch, @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                               @RequestParam(required = false, defaultValue = "0") @Min(0) int page, @RequestParam(required = false, defaultValue = "desc") Order order) {
        int p = adjustPage(page);
        return ResponseEntity.ok(accountRewardApiService.getUnclaimedRewardRestBySpendableEpoch(epoch, p, count, order));
    }

    @Operation(summary = "Get all rewards by epoch", description = "Retrieve rewards for a specific epoch with pagination.")
    @GetMapping("/epochs/{epoch}/rewards")
    public ResponseEntity<List<RewardDto>> getEpochRewards(@PathVariable Integer epoch, @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                                           @RequestParam(required = false, defaultValue = "0") @Min(0) int page) {
        int p = adjustPage(page);
        return ResponseEntity.ok(accountRewardApiService.getPoolRewards(epoch, p, count));
    }

    @Operation(summary = "Get all reward rest by epoch", description = "Retrieve all non-pool rewards for a specific epoch with pagination.")
    @GetMapping("/epochs/{epoch}/reward_rest")
    public ResponseEntity<List<RewardRestDto>> getEpochRewardRest(@PathVariable Integer epoch, @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                                                  @RequestParam(required = false, defaultValue = "0") @Min(0) int page) {
        int p = adjustPage(page);
        return ResponseEntity.ok(accountRewardApiService.getRewardRest(epoch, p, count));
    }

    //-- Rewards for a pool
    @Operation(summary = "Get all rewards for a pool by epoch (spendable epoch)", description = "Retrieve rewards for a specific pool hash or bech32 poolId by spendable epoch with pagination.")
    @GetMapping("/pools/{poolHash}/epochs/{epoch}/rewards")
    public ResponseEntity<List<PoolAddressRewardDto>> getPoolRewards(@PathVariable Integer epoch, @PathVariable String poolHash, @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                                                     @RequestParam(required = false, defaultValue = "0") @Min(0) int page) {
        int p = adjustPage(page);
        return ResponseEntity.ok(accountRewardApiService.getPoolRewardsByPoolHashAndSpendableEpoch(poolHash, epoch, p, count));
    }

    @Operation(summary = "Get available rewards for an address", description = "Retrieve unwithdrawn rewards for a specific address with pagination.")
    @GetMapping("/accounts/{address}/rewards/available")
    public ResponseEntity<List<RewardInfoDto>> getUnwithdrawnRewardsByAddress(@PathVariable String address, @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                                                              @RequestParam(required = false, defaultValue = "0") @Min(0) int page) {
        int p = adjustPage(page);
        return ResponseEntity.ok(accountRewardApiService.getUnwithdrawnRewardsByAddresses(List.of(address), p, count));
    }

    @Operation(summary = "Get available rewards for addresses", description = "Retrieve unwithdrawn rewards for a list of addresses with pagination.")
    @PostMapping("/rewards/available")
    public ResponseEntity<List<RewardInfoDto>> getUnwithdrawnRewards(@RequestBody List<String> addresses, @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                                                      @RequestParam(required = false, defaultValue = "0") @Min(0) int page) {
        int p = adjustPage(page);
        return ResponseEntity.ok(accountRewardApiService.getUnwithdrawnRewardsByAddresses(addresses, p, count));
    }

}
