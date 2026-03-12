package com.bloxbean.cardano.yaci.store.blockfrost.pools.controller;

import com.bloxbean.cardano.yaci.store.blockfrost.pools.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.service.BFPoolsService;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.util.BFPoolIdUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Blockfrost Pools")
@RequestMapping("${blockfrost.apiPrefix}/pools")
@ConditionalOnExpression("${store.extensions.blockfrost.pools.enabled:false}")
public class BFPoolsController {

    private final BFPoolsService bfPoolsService;

    @PostConstruct
    public void postConstruct() {
        log.info("Blockfrost PoolsController initialized >>>");
    }

    @GetMapping
    @Operation(summary = "List of registered stake pools")
    public List<String> getPools(
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        return bfPoolsService.getPools(page, count, order);
    }

    @GetMapping("/extended")
    @Operation(summary = "List of registered stake pools with additional information")
    public List<BFPoolListItemDto> getExtendedPools(
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        return bfPoolsService.getExtendedPools(page, count, order);
    }

    @GetMapping("/retired")
    @Operation(summary = "List of retired stake pools")
    public List<BFPoolRetireItemDto> getRetiredPools(
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        return bfPoolsService.getRetiredPools(page, count, order);
    }

    @GetMapping("/retiring")
    @Operation(summary = "List of retiring stake pools")
    public List<BFPoolRetireItemDto> getRetiringPools(
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        return bfPoolsService.getRetiringPools(page, count, order);
    }

    @GetMapping("/{poolId}")
    @Operation(summary = "Specific stake pool")
    public BFPoolDto getPool(@PathVariable String poolId) {
        validatePoolId(poolId);
        return bfPoolsService.getPool(poolId);
    }

    @GetMapping("/{poolId}/metadata")
    @Operation(summary = "Stake pool metadata")
    public Object getPoolMetadata(@PathVariable String poolId) {
        validatePoolId(poolId);
        return bfPoolsService.getPoolMetadata(poolId);
    }

    @GetMapping("/{poolId}/relays")
    @Operation(summary = "Stake pool relays")
    public List<BFPoolRelayDto> getPoolRelays(@PathVariable String poolId) {
        validatePoolId(poolId);
        return bfPoolsService.getPoolRelays(poolId);
    }

    @GetMapping("/{poolId}/blocks")
    @Operation(summary = "Stake pool blocks")
    public List<String> getPoolBlocks(
            @PathVariable String poolId,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        validatePoolId(poolId);
        return bfPoolsService.getPoolBlocks(poolId, page, count, order);
    }

    @GetMapping("/{poolId}/updates")
    @Operation(summary = "Stake pool certificate updates")
    public List<BFPoolUpdateDto> getPoolUpdates(
            @PathVariable String poolId,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        validatePoolId(poolId);
        return bfPoolsService.getPoolUpdates(poolId, page, count, order);
    }

    @GetMapping("/{poolId}/votes")
    @Operation(summary = "Stake pool votes")
    public List<BFPoolVoteDto> getPoolVotes(
            @PathVariable String poolId,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        validatePoolId(poolId);
        return bfPoolsService.getPoolVotes(poolId, page, count, order);
    }

    @GetMapping("/{poolId}/history")
    @Operation(summary = "Stake pool history",
            description = "History of stake pool parameters over epochs. " +
                    "Fields active_stake, active_size, delegators_count and rewards are null when the adapot aggregate is not enabled.")
    public List<BFPoolHistoryDto> getPoolHistory(
            @PathVariable String poolId,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        validatePoolId(poolId);
        return bfPoolsService.getPoolHistory(poolId, page, count, order);
    }

    @GetMapping("/{poolId}/delegators")
    @Operation(summary = "Stake pool delegators",
            description = "List of current delegators to a stake pool. " +
                    "Field live_stake is null when the adapot aggregate is not enabled.")
    public List<BFPoolDelegatorDto> getPoolDelegators(
            @PathVariable String poolId,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        validatePoolId(poolId);
        return bfPoolsService.getPoolDelegators(poolId, page, count, order);
    }

    private void validatePoolId(String poolId) {
        if (!BFPoolIdUtil.isValid(poolId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or malformed pool ID.");
        }
    }
}
