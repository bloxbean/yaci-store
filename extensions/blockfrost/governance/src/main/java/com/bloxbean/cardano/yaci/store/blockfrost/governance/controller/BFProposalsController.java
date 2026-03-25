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
@Tag(name = "Blockfrost Governance - Proposals")
@RequestMapping("${blockfrost.apiPrefix}/governance/proposals")
@ConditionalOnExpression("${store.extensions.blockfrost.governance.enabled:false}")
public class BFProposalsController {

    private final BFGovernanceService governanceService;

    // ── By tx_hash + cert_index ────────────────────────────────────────────

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Proposals", description = "Return the information about Proposals")
    public List<BFProposalDto> getProposals(
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        int p = page - 1;
        return governanceService.getProposals(p, count, order);
    }

    @GetMapping(value = "/{tx_hash}/{cert_index}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Specific proposal", description = "Proposal information.")
    public BFProposalDto getProposal(
            @PathVariable("tx_hash") String txHash,
            @PathVariable("cert_index") int certIndex) {
        return governanceService.getProposal(txHash, certIndex);
    }

    @GetMapping(value = "/{tx_hash}/{cert_index}/parameters", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Specific parameters proposal", description = "Parameters proposal details.")
    public BFProposalParametersDto getProposalParameters(
            @PathVariable("tx_hash") String txHash,
            @PathVariable("cert_index") int certIndex) {
        return governanceService.getProposalParameters(txHash, certIndex);
    }

    @GetMapping(value = "/{tx_hash}/{cert_index}/withdrawals", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Specific withdrawals proposal", description = "Withdrawal proposal details.")
    public List<BFProposalWithdrawalDto> getProposalWithdrawals(
            @PathVariable("tx_hash") String txHash,
            @PathVariable("cert_index") int certIndex) {
        return governanceService.getProposalWithdrawals(txHash, certIndex);
    }

    @GetMapping(value = "/{tx_hash}/{cert_index}/votes", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Proposal votes", description = "History of Proposal votes.")
    public List<BFProposalVoteDto> getProposalVotes(
            @PathVariable("tx_hash") String txHash,
            @PathVariable("cert_index") int certIndex,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        int p = page - 1;
        return governanceService.getProposalVotes(txHash, certIndex, p, count, order);
    }

    @GetMapping(value = "/{tx_hash}/{cert_index}/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Specific proposal metadata", description = "Proposal metadata information.")
    public BFProposalMetadataDto getProposalMetadata(
            @PathVariable("tx_hash") String txHash,
            @PathVariable("cert_index") int certIndex) {
        return governanceService.getProposalMetadata(txHash, certIndex);
    }

    // ── By gov_action_id (CIP-0129 bech32) ────────────────────────────────

    @GetMapping(value = "/{gov_action_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "GovActionID proposal",
            description = "Proposal information by CIP-0129 governance action ID.")
    public BFProposalDto getProposalByGovActionId(
            @PathVariable("gov_action_id") String govActionId) {
        return governanceService.getProposalByGovActionId(govActionId);
    }

    @GetMapping(value = "/{gov_action_id}/parameters", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "GovActionID Proposal parameters", description = "Parameters proposal details.")
    public BFProposalParametersDto getProposalParametersByGovActionId(
            @PathVariable("gov_action_id") String govActionId) {
        return governanceService.getProposalParametersByGovActionId(govActionId);
    }

    @GetMapping(value = "/{gov_action_id}/withdrawals", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "GovActionID Proposal withdrawals", description = "Withdrawal proposal details.")
    public List<BFProposalWithdrawalDto> getProposalWithdrawalsByGovActionId(
            @PathVariable("gov_action_id") String govActionId) {
        return governanceService.getProposalWithdrawalsByGovActionId(govActionId);
    }

    @GetMapping(value = "/{gov_action_id}/votes", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "GovActionID Proposal votes",
            description = "History of Proposal votes by governance action ID.")
    public List<BFProposalVoteDto> getProposalVotesByGovActionId(
            @PathVariable("gov_action_id") String govActionId,
            @RequestParam(required = false, defaultValue = "1") @Min(1) int page,
            @RequestParam(required = false, defaultValue = "100") @Min(1) @Max(100) int count,
            @RequestParam(required = false, defaultValue = "asc") String order) {
        int p = page - 1;
        return governanceService.getProposalVotesByGovActionId(govActionId, p, count, order);
    }

    @GetMapping(value = "/{gov_action_id}/metadata", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "GovActionID proposal metadata",
            description = "Proposal metadata information by governance action ID.")
    public BFProposalMetadataDto getProposalMetadataByGovActionId(
            @PathVariable("gov_action_id") String govActionId) {
        return governanceService.getProposalMetadataByGovActionId(govActionId);
    }
}
