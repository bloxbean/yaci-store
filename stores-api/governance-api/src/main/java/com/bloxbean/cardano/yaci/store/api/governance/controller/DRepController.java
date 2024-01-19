package com.bloxbean.cardano.yaci.store.api.governance.controller;

import com.bloxbean.cardano.yaci.store.api.governance.service.DRepRegistrationService;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
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
@Tag(name = "DRep Service")
@RequestMapping("${apiPrefix}/dreps")
@ConditionalOnExpression("${store.governance.endpoints.drep.enabled:true}")
public class DRepController {
    private final DRepRegistrationService dRepRegistrationService;

    @GetMapping("/registrations")
    @Operation(description = "Get dRep registrations by page number and count")
    public List<DRepRegistration> getDRepRegistrations(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                       @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return dRepRegistrationService.getRegistrations(p, count);
    }

    @GetMapping("/deregistrations")
    @Operation(description = "Get dRep de-registrations by page number and count")
    public List<DRepRegistration> getDRepDeRegistrations(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                         @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return dRepRegistrationService.getDeRegistrations(p, count);
    }

    @GetMapping("/updates")
    @Operation(description = "Get dRep updates by page number and count")
    public List<DRepRegistration> getDRepUpdates(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return dRepRegistrationService.getUpdates(p, count);
    }
}
