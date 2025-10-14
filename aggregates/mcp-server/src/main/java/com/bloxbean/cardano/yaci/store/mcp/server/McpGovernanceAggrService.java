package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepDetailsDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.SpecialDRepDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.DRepApiService;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.ProposalApiService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = {"store.governance-aggr.enabled", "store.mcp-server.tools.governance-aggr.enabled"},
    havingValue = "true"
)
public class McpGovernanceAggrService {
    private final ProposalApiService proposalApiService;
    private final DRepApiService dRepApiService;

    @Tool(name = "proposals-list",
            description = "Get a paginated list of governance proposals with voting statistics. Returns proposals with their status (LIVE, RATIFIED, ENACTED, EXPIRED) and voting power breakdown. Page is 0-based. Requires governance-aggr module to be enabled.")
    public List<ProposalDto> getProposals(int page, int count, String order) {
        Order orderEnum = "asc".equalsIgnoreCase(order) ? Order.asc : Order.desc;
        return proposalApiService.getProposals(page, count, orderEnum);
    }

    @Tool(name = "proposal-by-id",
            description = "Get detailed information for a specific governance proposal by transaction hash and index. Returns proposal details including deposit, return address, anchor, voting statistics, and lifecycle status. Requires governance-aggr module to be enabled.")
    public ProposalDto getProposalById(String txHash, int index) {
        return proposalApiService.getProposalById(txHash, index)
                .orElseThrow(() -> new RuntimeException("Proposal not found with txHash: " + txHash + " and index: " + index));
    }

    @Tool(name = "dreps-list",
            description = "Get a paginated list of DReps (Delegated Representatives) with their voting power and delegation statistics. Returns DRep details including voting power, delegated stake, and activity status. Page is 0-based. Requires governance-aggr module to be enabled.")
    public List<DRepDetailsDto> getDReps(int page, int count, String order) {
        Order orderEnum = "asc".equalsIgnoreCase(order) ? Order.asc : Order.desc;
        return dRepApiService.getDReps(page, count, orderEnum);
    }

    @Tool(name = "drep-details",
            description = "Get detailed information for a specific DRep by DRep ID. Returns DRep voting power, delegation stats, and metadata. DRep ID should be in bech32 format (drep1...) or hex. Requires governance-aggr module to be enabled.")
    public DRepDetailsDto getDRepDetails(String drepId) {
        return dRepApiService.getDRepDetailsByDRepId(drepId)
                .orElseThrow(() -> new RuntimeException("DRep not found with ID: " + drepId));
    }

    @Tool(name = "special-dreps",
            description = "Get details for special DReps: 'Always Abstain' and 'No Confidence'. Returns voting power and delegation for these built-in DRep options. Requires governance-aggr module to be enabled.")
    public List<SpecialDRepDto> getSpecialDReps() {
        return dRepApiService.getAutoAbstainAndNoConfidenceDRepDetail();
    }
}
