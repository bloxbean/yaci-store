package com.bloxbean.cardano.yaci.store.api.staking.controller;

import com.bloxbean.cardano.yaci.store.api.staking.service.StakeService;
import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${apiPrefix}/stake")
@RequiredArgsConstructor
@Slf4j
public class StakeController {
    private final StakeService stakeService;

    @GetMapping("/registrations")
    @Operation(description = "Get stake address registerations by page number and count")
    public List<StakeRegistrationDetail> getStakeRegistrations(@RequestParam(name = "page", defaultValue = "0") int page,
                                                               @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return stakeService.getStakeRegistrations(p, count);
    }

    @GetMapping("/deregistrations")
    @Operation(description = "Get stake de-registrations by page number and count")
    public List<StakeRegistrationDetail> getStakeDeregistrations(@RequestParam(name = "page", defaultValue = "0") int page,
                                               @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return stakeService.getStakeDeregistrations(p, count);
    }

    @GetMapping("/delegations")
    @Operation(description = "Get stake delegations by page number and count")
    public List<Delegation> getStakeDelegations(@RequestParam(name = "page", defaultValue = "0") int page,
                                                    @RequestParam(name = "count", defaultValue = "10") int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return stakeService.getStakeDelegations(p, count);
    }
}
