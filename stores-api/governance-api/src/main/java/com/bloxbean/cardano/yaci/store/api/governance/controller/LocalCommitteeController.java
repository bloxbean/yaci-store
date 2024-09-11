package com.bloxbean.cardano.yaci.store.api.governance.controller;

import com.bloxbean.cardano.yaci.store.api.governance.dto.LocalCommitteeDto;
import com.bloxbean.cardano.yaci.store.api.governance.service.LocalCommitteeService;
import com.bloxbean.cardano.yaci.store.governance.service.LocalGovStateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("${apiPrefix}/governance/live/committees")
@RequiredArgsConstructor
@Tag(name = "Local Committee Service", description = "Get committee info directly from local Cardano Node through n2c local query.")
@Slf4j
@ConditionalOnBean(LocalGovStateService.class)
@ConditionalOnExpression("${store.epoch.endpoints.committee.live.enabled:true}")
public class LocalCommitteeController {
    private final LocalCommitteeService localCommitteeService;

    @GetMapping("/current")
    @Operation(description = "Get current committee info")
    public ResponseEntity<LocalCommitteeDto> getCommitteeInfo() {
        return localCommitteeService.getCurrentCommittee()
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Committee not found"));
    }
}
