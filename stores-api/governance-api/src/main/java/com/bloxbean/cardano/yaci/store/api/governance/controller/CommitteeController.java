package com.bloxbean.cardano.yaci.store.api.governance.controller;

import com.bloxbean.cardano.yaci.store.api.governance.dto.CommitteeDto;
import com.bloxbean.cardano.yaci.store.api.governance.service.CommitteeDeRegistrationService;
import com.bloxbean.cardano.yaci.store.api.governance.service.CommitteeRegistrationService;
import com.bloxbean.cardano.yaci.store.api.governance.service.CommitteeService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeDeRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.CommitteeRegistration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Committee Service")
@RequestMapping("${apiPrefix}/governance/committees")
@ConditionalOnExpression("${store.governance.endpoints.committee.enabled:true}")
public class CommitteeController {
    private final CommitteeRegistrationService committeeRegistrationService;
    private final CommitteeDeRegistrationService committeeDeRegistrationService;
    private final CommitteeService committeeService;

    @GetMapping("/registrations")
    @Operation(description = "Get committee registrations by page number and count")
    public ResponseEntity<List<CommitteeRegistration>> getCommitteeRegistrations(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                                                 @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count,
                                                                                 @RequestParam(name = "order", defaultValue = "desc") Order order) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return ResponseEntity.ok(committeeRegistrationService.getCommitteeRegistrations(p, count, order));
    }

    @GetMapping("/deregistrations")
    @Operation(description = "Get committee de-registrations by page number and count")
    public ResponseEntity<List<CommitteeDeRegistration>> getCommitteeDeRegistrations(@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                                                     @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count,
                                                                                     @RequestParam(name = "order", defaultValue = "desc") Order order) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;
        return ResponseEntity.ok(committeeDeRegistrationService.getCommitteeDeRegistrations(p, count, order));
    }

    @GetMapping("/current")
    @Operation(description = "Get current committee info")
    public ResponseEntity<CommitteeDto> getCommitteeMembers() {
        return committeeService.getCurrentCommittee()
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Committee not found"));
    }
}
