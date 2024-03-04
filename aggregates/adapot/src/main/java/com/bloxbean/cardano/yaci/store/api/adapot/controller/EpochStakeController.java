package com.bloxbean.cardano.yaci.store.api.adapot.controller;

import com.bloxbean.cardano.yaci.store.adapot.domain.EpochStake;
import com.bloxbean.cardano.yaci.store.adapot.storage.EpochStakeStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.util.List;

@RestController("EpochStakeController")
@RequestMapping("${apiPrefix}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "EpochStake API", description = "APIs for epoch stake related data.")
public class EpochStakeController {
    private final EpochStakeStorage epochStakeStorage;

    @GetMapping("/epochs/{epoch}/total-stake")
    @Operation(description = "Get total active stake for an epoch")
    public EpochActiveStake getTotalActiveStakeByEpoch(Integer epoch) {
        return epochStakeStorage.getTotalActiveStakeByEpoch(epoch)
                .map(activeStake -> new EpochActiveStake(epoch, activeStake))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stake not found"));
    }

    @GetMapping("/epochs/{epoch}/addresses/{address}/stake")
    @Operation(description = "Get active stake for an address for an epoch")
    public EpochStake getActiveStakeByAddressAndEpoch(String address, Integer epoch) {
        return epochStakeStorage.getActiveStakeByAddressAndEpoch(address, epoch)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stake not found"));
    }

    @GetMapping("/epochs/{epoch}/pools/{poolId}/stake")
    public PoolActiveStake getActiveStakeByPoolAndEpoch(String poolId, Integer epoch) {
        return epochStakeStorage.getActiveStakeByPoolAndEpoch(poolId, epoch)
                .map(activeStake -> new PoolActiveStake(epoch, poolId, activeStake))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stake not found"));
    }

    @GetMapping("/epochs/{epoch}/stake")
    @Operation(description = "Get active stake for all pools for an epoch")
    public List<EpochStake> getActiveStakeForAllPoolsForEpoch(Integer epoch, int page , int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return epochStakeStorage.getAllActiveStakesByEpoch(epoch, p, count);
    }

    record EpochActiveStake(Integer epoch, BigInteger activeStake) {
    }

    record PoolActiveStake(Integer epoch, String poolId, BigInteger activeStake) {
    }
}
