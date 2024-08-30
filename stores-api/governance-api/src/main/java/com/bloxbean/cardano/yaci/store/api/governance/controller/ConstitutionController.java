package com.bloxbean.cardano.yaci.store.api.governance.controller;

import com.bloxbean.cardano.yaci.store.api.governance.dto.ConstitutionDto;
import com.bloxbean.cardano.yaci.store.api.governance.service.ConstitutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Constitution Service")
@RequestMapping("${apiPrefix}/governance/constitution")
@ConditionalOnExpression("${store.governance.endpoints.constitution.enabled:true}")
public class ConstitutionController {
    private final ConstitutionService constitutionService;

    @GetMapping
    @Operation(description = "Get current constitution")
    public ResponseEntity<ConstitutionDto> getCurrentConstitution() {

        return constitutionService.findCurrentConstitution()
                .map(constitution -> ResponseEntity.ok(
                        ConstitutionDto.builder()
                                .activeEpoch(constitution.getActiveEpoch())
                                .anchorHash(constitution.getAnchorHash())
                                .anchorUrl(constitution.getAnchorUrl())
                                .script(constitution.getScript())
                                .build()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Constitution not found"));
    }
}
