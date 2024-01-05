package com.bloxbean.cardano.yaci.store.api.governance.controller;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.api.governance.service.GovActionProposalService;
import com.bloxbean.cardano.yaci.store.api.governance.service.VotingProcedureService;
import com.bloxbean.cardano.yaci.store.governance.domain.GovActionProposal;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "GovActionProposal Service")
@RequestMapping("${apiPrefix}/gov-action-proposal")
@ConditionalOnExpression("${store.governance.endpoints.govActionProposal.enabled:true}")
public class GovActionProposalController {
    private final GovActionProposalService govActionProposalService;
    private final VotingProcedureService votingProcedureService;

    @GetMapping("/{txHash}")
    @Operation(description = "Get governance action proposal list by transaction hash")
    public ResponseEntity<List<GovActionProposal>> getGovActionProposalByTx(@PathVariable @Pattern(regexp = "^[0-9a-fA-F]{64}$") String txHash) {
        return ResponseEntity.ok(govActionProposalService.getGovActionProposalByTx(txHash));
    }

    @GetMapping("/gov-action-type/{govActionType}")
    @Operation(description = "Get governance action proposal list by governance action type")
    public ResponseEntity<List<GovActionProposal>> getGovActionProposalByGovActionType(
            @Parameter(description = "Governance action type", required = true, example = "PARAMETER_CHANGE_ACTION")
            @PathVariable String govActionType,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        GovActionType type;
        try {
            type = GovActionType.valueOf(govActionType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Governance action type is not valid");
        }

        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        return ResponseEntity.ok(govActionProposalService.getGovActionProposalByGovActionType(type, p, count));
    }

    @GetMapping("/return-address/{address}")
    @Operation(description = "Get governance action proposal list by return address")
    public ResponseEntity<List<GovActionProposal>> getGovActionProposalByReturnAddress(@PathVariable String address,
                                                                                       @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                                                       @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        return ResponseEntity.ok(govActionProposalService.getGovActionProposalByReturnAddress(address, p, count));
    }

    @GetMapping("/{txHash}/voting-procedures")
    @Operation(description = "Get voting procedure list by transaction hash of governance action proposal")
    public ResponseEntity<List<VotingProcedure>> getVotingProceduresByGovActionProposalTx(@PathVariable @Pattern(regexp = "^[0-9a-fA-F]{64}$") String txHash,
                                                                                          @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
                                                                                          @RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(100) int count) {
        //TODO -- Fix pagination index
        int p = page;
        if (p > 0)
            p = p - 1;

        return ResponseEntity.ok(votingProcedureService.getVotingProcedureByGovActionProposalTx(txHash, page, count));
    }

    @GetMapping("/{txHash}/{indexInTx}/voting-procedures")
    @Operation(description = "Get voting procedure list for a governance action proposal")
    public ResponseEntity<List<VotingProcedure>> getVotingProceduresForGovActionProposal(@PathVariable @Pattern(regexp = "^[0-9a-fA-F]{64}$") String txHash,
                                                                                         @PathVariable @Min(0) int indexInTx) {
        return ResponseEntity.ok(votingProcedureService.getVotingProcedureByGovActionProposalTxAndGovActionProposalIndex(txHash, indexInTx));
    }
}
