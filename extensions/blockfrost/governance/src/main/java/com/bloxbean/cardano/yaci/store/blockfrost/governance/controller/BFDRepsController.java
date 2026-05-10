package com.bloxbean.cardano.yaci.store.blockfrost.governance.controller;

import com.bloxbean.cardano.yaci.store.blockfrost.governance.dto.*;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.service.BFGovernanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Blockfrost Governance - DReps")
@RequestMapping("${blockfrost.apiPrefix}/governance/dreps")
@ConditionalOnExpression("${store.extensions.blockfrost.governance.enabled:false}")
public class BFDRepsController {

    private final BFGovernanceService governanceService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Delegate Representatives (DReps)",
            description = "Return the information about Delegate Representatives (DReps)")
    public List<BFDRepListItemDto> getDReps(
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        int p = page - 1;
        return governanceService.getDReps(p, count, order);
    }

    @GetMapping(value = "/{drep_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Specific DRep", description = "DRep information.")
    public BFDRepDto getDRep(@PathVariable("drep_id") String drepId) {
        return governanceService.getDRep(drepId);
    }

    @GetMapping(value = "/{drep_id}/delegators", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "DRep delegators", description = "List of DRep delegators.")
    public List<BFDRepDelegatorDto> getDRepDelegators(
            @PathVariable("drep_id") String drepId,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        int p = page - 1;
        return governanceService.getDRepDelegators(drepId, p, count, order);
    }

    @GetMapping(value = "/{drep_id}/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "DRep metadata", description = "DRep metadata information.")
    public BFDRepMetadataDto getDRepMetadata(@PathVariable("drep_id") String drepId) {
        return governanceService.getDRepMetadata(drepId);
    }

    @GetMapping(value = "/{drep_id}/updates", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "DRep updates", description = "List of certificate updates to the DRep.")
    public List<BFDRepUpdateDto> getDRepUpdates(
            @PathVariable("drep_id") String drepId,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        int p = page - 1;
        return governanceService.getDRepUpdates(drepId, p, count, order);
    }

    @GetMapping(value = "/{drep_id}/votes", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "DRep votes", description = "History of DRep votes.")
    public List<BFDRepVoteDto> getDRepVotes(
            @PathVariable("drep_id") String drepId,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        int p = page - 1;
        return governanceService.getDRepVotes(drepId, p, count, order);
    }
}
