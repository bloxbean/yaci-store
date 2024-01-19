package com.bloxbean.cardano.yaci.store.api.governance.controller;

import com.bloxbean.cardano.yaci.store.api.governance.service.CommitteeDeRegistrationService;
import com.bloxbean.cardano.yaci.store.api.governance.service.CommitteeRegistrationService;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeDeRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Committee Service")
@RequestMapping("${apiPrefix}/committees")
@ConditionalOnExpression("${store.governance.endpoints.committee.enabled:true}")
public class CommitteeController {
    private final CommitteeRegistrationService committeeRegistrationService;
    private final CommitteeDeRegistrationService committeeDeRegistrationService;

    @GetMapping("/registrations")
    @Operation(description = "Get committee registrations by page number and count")
    public ResponseEntity<List<CommitteeRegistration>> getCommitteeRegistrations(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                                                @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return ResponseEntity.ok(committeeRegistrationService.getCommitteeRegistrations(p, count));
    }

    @GetMapping("/deregistrations")
    @Operation(description = "Get committee de-registrations by page number and count")
    public ResponseEntity<List<CommitteeDeRegistration>> getCommitteeDeRegistrations(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                                     @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return ResponseEntity.ok(committeeDeRegistrationService.getCommitteeDeRegistrations(p, count));
    }

}
