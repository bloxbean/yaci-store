package com.bloxbean.cardano.yaci.store.staking.controller;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolRegistration;
import com.bloxbean.cardano.yaci.store.staking.domain.PoolRetirement;
import com.bloxbean.cardano.yaci.store.staking.service.PoolService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${apiPrefix}/pools")
@RequiredArgsConstructor
@Slf4j
public class PoolController {
    private final PoolService poolService;

    @GetMapping("/registrations")
    @Operation(description = "Get pool registerations by page number and count")
    public List<PoolRegistration> getPoolRegistrations(@RequestParam(name = "page", defaultValue = "0") int page,
                                            @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return poolService.getPoolRegistrations(p, count);
    }

    @GetMapping("/retirements")
    @Operation(description = "Get pool retirements by page number and count")
    public List<PoolRetirement> getRetirements(@RequestParam(name = "page", defaultValue = "0") int page,
                                               @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return poolService.getPoolRetirements(p, count);
    }
}
