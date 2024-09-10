package com.bloxbean.cardano.yaci.store.api.governance.controller;

import com.bloxbean.cardano.yaci.store.api.governance.dto.LocalDRepStakeDto;
import com.bloxbean.cardano.yaci.store.governance.service.LocalDRepDistrService;
import com.bloxbean.cardano.yaci.store.governance.service.LocalGovStateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("${apiPrefix}/local/dRep")
@RequiredArgsConstructor
@Tag(name = "Local Constitution Service", description = "Get constitution directly from local Cardano Node through n2c local query.")
@Slf4j
@ConditionalOnBean(LocalGovStateService.class)
@ConditionalOnExpression("${store.epoch.endpoints.drep.local.enabled:true}")
public class LocalDRepDistrController {
    private final LocalDRepDistrService localDRepDistrService;

    @GetMapping("/{dRepHash}/stake")
    @Operation(description = "Get dRep hash distribution")
    public ResponseEntity<LocalDRepStakeDto> getCommitteeInfo(@PathVariable String dRepHash, @RequestParam(name = "epoch") Integer epoch) {
        return localDRepDistrService.getLocalDRepDistrByDRepHashAndEpoch(dRepHash, epoch)
                .map(localDRepDistr -> ResponseEntity.ok(LocalDRepStakeDto.builder()
                        .drepHash(dRepHash)
                        .epoch(epoch)
                        .amount(localDRepDistr.getAmount())
                        .build()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DRep stake not found"));
    }
}
