package com.bloxbean.cardano.yaci.store.api.staking.controller;

import com.bloxbean.cardano.yaci.store.api.staking.service.PoolService;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Pool Service")
@RequestMapping("${apiPrefix}/pools")
@ConditionalOnExpression("${store.staking.endpoints.pool.enabled:true}")
public class PoolController {
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
}
