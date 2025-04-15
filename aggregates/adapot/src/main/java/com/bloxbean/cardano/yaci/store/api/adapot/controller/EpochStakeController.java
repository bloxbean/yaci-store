package com.bloxbean.cardano.yaci.store.api.adapot.controller;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorageReader;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.ControllerPageUtil.adjustPage;

@RestController("EpochStakeController")
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EpochStake API", description = "APIs for epoch stake related data.")
public class EpochStakeController {
    private final EpochStakeStorageReader epochStakeStorage;

    @GetMapping("/epochs/{epoch}/total-stake")
    @Operation(description = "Get total active stake for an epoch")
    public EpochActiveStake getTotalActiveStakeByEpoch(@PathVariable Integer epoch) {
        return epochStakeStorage.getTotalActiveStakeByEpoch(epoch)
                .map(activeStake -> new EpochActiveStake(epoch, activeStake))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stake not found"));
    }

    @GetMapping("/epochs/{epoch}/accounts/{address}/stake")
    @Operation(description = "Get active stake for an account for an epoch")
    public AccountActiveStake getActiveStakeByAddressAndEpoch(@PathVariable String address, @PathVariable Integer epoch) {
        return epochStakeStorage.getActiveStakeByAddressAndEpoch(address, epoch)
                .map(epochStake -> AccountActiveStake.toDto(epochStake))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stake not found"));
    }

    @GetMapping("/epochs/{epoch}/pools/{poolHash}/stake")
    @Operation(description = "Get active stake for a pool hash for an epoch")
    public PoolActiveStake getActiveStakeByPoolAndEpoch(@PathVariable String poolHash, @PathVariable Integer epoch) {
        if (poolHash != null && poolHash.startsWith(PoolUtil.POOL_ID_PREFIX)) {
            poolHash = HexUtil.encodeHexString(Bech32.decode(poolHash).data);
        }

        final String _poolHash = poolHash;
        return epochStakeStorage.getActiveStakeByPoolAndEpoch(poolHash, epoch)
                .map(activeStake -> new PoolActiveStake(epoch, _poolHash, getBech32PoolId(_poolHash), activeStake))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stake not found"));
    }

    @GetMapping("/epochs/{epoch}/stake")
    @Operation(description = "Get active stake for all pools for an epoch")
    public List<AccountActiveStake> getActiveStakeForAllPoolsForEpoch(@PathVariable Integer epoch,
                                                                      @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                                                      @RequestParam(required = false, defaultValue = "0") @Min(0) int page) {
        int p = adjustPage(page);
        return epochStakeStorage.getAllActiveStakesByEpoch(epoch, p, count)
                .stream().map(epochStake -> AccountActiveStake.toDto(epochStake))
                .toList();
    }

    @GetMapping("/epochs/{epoch}/pools/{poolHash}/delegators")
    @Operation(description = "Retrieve active stakes of pool delegators for a specific pool hash during a given epoch. Supports bech32 pool ID.")
    public List<AccountActiveStake> getPoolDelegatorsActiveStakes(@PathVariable Integer epoch, @PathVariable String poolHash,
                                        @RequestParam(required = false, defaultValue = "0") @Min(0) int page,
                                        @RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count
                                        ) {
        int p = adjustPage(page);
        if (poolHash != null && poolHash.startsWith(PoolUtil.POOL_ID_PREFIX)) {
            poolHash = HexUtil.encodeHexString(Bech32.decode(poolHash).data);
        }

        return epochStakeStorage.getAllActiveStakesByEpochAndPool(epoch, poolHash, p, count)
                .stream().map(epochStake -> AccountActiveStake.toDto(epochStake))
                .toList();
    }

    /**
    //TODO -- Temporary endpoint
    @PostMapping("/calculate-rewards/epoch/{epoch}")
    public void calculateRewardsForEpoch(Integer epoch) {
        epochRewardCalculationService.fetchRewardCalcInputs(epoch);
    }
    **/

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record EpochActiveStake(Integer epoch, BigInteger activeStake) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record PoolActiveStake(Integer epoch, String poolHash, String poolId, BigInteger activeStake) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record AccountActiveStake(String address, Integer epoch, BigInteger amount, String poolHash, String poolId, Integer delegationEpoch) {
        public static AccountActiveStake toDto(EpochStake epochStake) {
            String poolBech32Id = getBech32PoolId(epochStake.getPoolId());

            return new AccountActiveStake(epochStake.getAddress(),
                    epochStake.getActiveEpoch(),
                    epochStake.getAmount(),
                    epochStake.getPoolId(),
                    poolBech32Id,
                    epochStake.getDelegationEpoch());
        }
    }

    private static String getBech32PoolId(String poolHash) {
        String poolBech32Id = null;
        try {
            if (poolHash != null) {
                poolBech32Id = PoolUtil.getBech32PoolId(poolHash);
            }
        } catch (Exception e) {}
        return poolBech32Id;
    }

}
