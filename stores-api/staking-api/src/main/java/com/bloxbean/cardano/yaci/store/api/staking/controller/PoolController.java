package com.bloxbean.cardano.yaci.store.api.staking.controller;

import com.bloxbean.cardano.yaci.store.api.staking.dto.PoolDetailsDto;
import com.bloxbean.cardano.yaci.store.api.staking.service.PoolService;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.PoolUtil.POOL_ID_PREFIX;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Pool Service")
@RequestMapping("${apiPrefix}/pools")
@ConditionalOnExpression("${store.staking.endpoints.pool.enabled:true}")
public class PoolController {
    private final StoreProperties storeProperties;
    private final PoolService poolService;

    @GetMapping("/registrations")
    @Operation(description = "Get pool registrations by page number and count")
    public List<PoolRegistration> getPoolRegistrations(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                            @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return poolService.getPoolRegistrations(p, count);
    }

    @GetMapping("/retirements")
    @Operation(description = "Get pool retirements by page number and count")
    public List<PoolRetirement> getRetirements(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                               @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return poolService.getPoolRetirements(p, count);
    }

    @GetMapping("/retiring/{epoch}")
    @Operation(description = "Get retiring pool ids for the given epoch")
    public List<PoolRetirement> getRetiringPoolIds(@PathVariable int epoch) {
        return poolService.getRetiringPoolIds(epoch);
    }

    @GetMapping("/pools/{poolId}/epochs/{epoch}")
    @Operation(description = "Get pool details for the given pool hash or bech32 pool id and active epoch")
    public ResponseEntity<PoolDetailsDto> getPoolDetails(@PathVariable(name = "poolId") String poolId, @PathVariable(name = "epoch") int epoch) {

        String poolHash = poolId;
        if (poolId != null && poolId.startsWith(POOL_ID_PREFIX)) {
            poolHash = PoolUtil.getPoolHash(poolId);
        }

        return poolService.getPoolDetails(poolHash, epoch)
                .map(details -> PoolDetailsDto.toDto(details, storeProperties.isMainnet()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
