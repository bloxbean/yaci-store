package com.bloxbean.cardano.yaci.store.api.governanceaggr.controller;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.GovernanceStatsDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.GovernanceStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("GovernanceStatsController")
@RequestMapping("${apiPrefix}/governance-state")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Governance Stats API", description = "APIs for governance statistics and summary data.")
public class GovernanceStatsController {
    private final GovernanceStatsService governanceStatsService;

    @GetMapping("/stats")
    @Operation(description = "Get governance statistics summary including DRep, proposal, committee, and voting stats. " +
            "Defaults to current epoch if no epoch parameter is provided.",
            summary = "Get governance statistics summary")
    public ResponseEntity<GovernanceStatsDto> getGovernanceStats(
            @Parameter(description = "Epoch number. If not provided, uses the current epoch.")
            @RequestParam(required = false) Integer epoch) {

        return governanceStatsService.getGovernanceStats(epoch)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
