package com.bloxbean.cardano.yaci.store.api.staking.controller;

import com.bloxbean.cardano.yaci.store.api.staking.service.StakeService;
import com.bloxbean.cardano.yaci.store.staking.domain.Delegation;
import com.bloxbean.cardano.yaci.store.staking.domain.StakeRegistrationDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Account Service")
@RequestMapping("${apiPrefix}/stake")
@ConditionalOnExpression("${store.staking.endpoints.account.enabled:true}")
public class StakeController {

    private final StakeService stakeService;

    @GetMapping("/registrations")
    @Operation(description = "Get stake address registrations by page number and count")
    public List<StakeRegistrationDetail> getStakeRegistrations(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                               @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return stakeService.getStakeRegistrations(p, count);
    }

    @GetMapping("/deregistrations")
    @Operation(description = "Get stake de-registrations by page number and count")
    public List<StakeRegistrationDetail> getStakeDeRegistrations(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                                 @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return stakeService.getStakeDeregistrations(p, count);
    }

    @GetMapping("/delegations")
    @Operation(description = "Get stake delegations by page number and count")
    public List<Delegation> getStakeDelegations(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return stakeService.getStakeDelegations(p, count);
    }

    @GetMapping("/addresses/{epoch}")
    @Operation(description = "Get registered stake addresses by epoch")
    public List<String> getRegisteredStakeAddresses(@PathVariable Integer epoch, @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return stakeService.getRegisteredStakeAddresses(epoch,p, count);
    }
}
