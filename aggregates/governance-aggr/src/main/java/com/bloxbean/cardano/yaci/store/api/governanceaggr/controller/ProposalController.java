package com.bloxbean.cardano.yaci.store.api.governanceaggr.controller;


import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.ProposalApiService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.common.util.ControllerPageUtil.adjustPage;

@RestController("ProposalController")
@RequestMapping("${apiPrefix}/governance-state")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Proposal API", description = "APIs for proposal related data.")
public class ProposalController {
    private final ProposalApiService proposalApiService;

    @GetMapping("/proposals")
    public ResponseEntity<List<ProposalDto>> getProposals(@RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) int count,
                                                          @RequestParam(required = false, defaultValue = "0") @Min(0) int page, @RequestParam(required = false, defaultValue = "desc") Order order) {
        int p = adjustPage(page);

        List<ProposalDto> proposals = proposalApiService.getProposals(p, count, order);
        return ResponseEntity.ok(proposals);
    }

    @GetMapping("/proposals/{txHash}/{index}")
    public ResponseEntity<ProposalDto> getProposalById(@PathVariable String txHash,
                                                       @PathVariable int index) {

        return proposalApiService.getProposalById(txHash, index)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/proposals/gov-action-id/{govActionId}")
    @Operation(description = "Get proposal by CIP-129 bech32 governance action ID",
               summary = "Get proposal by CIP-129 gov_action_id")
    public ResponseEntity<ProposalDto> getProposalByGovActionId(
            @Parameter(description = "CIP-129 governance action ID in bech32 format", required = true, example = "gov_action1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq")
            @PathVariable String govActionId) {

        return proposalApiService.getProposalByGovActionId(govActionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
