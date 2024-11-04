package com.bloxbean.cardano.yaci.store.api.governance.controller.local;

import com.bloxbean.cardano.yaci.store.api.governance.dto.local.LocalDRepStakeDto;
import com.bloxbean.cardano.yaci.store.governance.service.LocalDRepDistServiceReader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("${apiPrefix}/governance/live/dreps")
@RequiredArgsConstructor
@Tag(name = "Local DRep Service", description = "Get dRep stake distribution from local Cardano Node.")
@Slf4j
@ConditionalOnBean(LocalDRepDistServiceReader.class)
@ConditionalOnExpression("${store.governance.endpoints.drep.live.enabled:true}")
public class LocalDRepDistrController {
    private final LocalDRepDistServiceReader localDRepDistServiceReader;

    @GetMapping("/{dRepHash}/stake")
    @Operation(description = "Get dRep stake distribution")
    public ResponseEntity<LocalDRepStakeDto> getDRepStakeDistr(@PathVariable String dRepHash) {
        return localDRepDistServiceReader.getLatestDRepDistrByDRepHashAndEpoch(dRepHash)
                .map(localDRepDistr -> ResponseEntity.ok(LocalDRepStakeDto.builder()
                        .drepHash(localDRepDistr.getDrepHash())
                        .drepType(localDRepDistr.getDrepType())
                        .epoch(localDRepDistr.getEpoch())
                        .amount(localDRepDistr.getAmount())
                        .build()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DRep stake not found"));
    }
}
