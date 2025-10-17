package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.client.governance.GovId;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepDetailsDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.SpecialDRepDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.DRepApiService;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.ProposalApiService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.governance-aggr.enabled", "store.mcp-server.tools.governance-aggr.enabled"},
    havingValue = "true"
)
public class McpGovernanceService {
    private final ProposalApiService proposalApiService;
    private final DRepApiService dRepApiService;

    @Tool(name = "proposals-list",
            description = "Get a paginated list of governance proposals with voting statistics. " +
                         "Returns proposals with their status (LIVE, RATIFIED, ENACTED, EXPIRED) and voting power breakdown. " +
                         "CRITICAL: All voting power values in voting_stats are in LOVELACE (1 ADA = 1,000,000 lovelace). " +
                         "Always convert lovelace to ADA by dividing by 1,000,000 when presenting to users. " +
                         "Page is 0-based. Requires governance-aggr module to be enabled.")
    public List<ProposalDto> getProposals(int page, int count, String order) {
        Order orderEnum = "asc".equalsIgnoreCase(order) ? Order.asc : Order.desc;
        return proposalApiService.getProposals(page, count, orderEnum);
    }

    @Tool(name = "proposal-by-id",
            description = "Get detailed information for a specific governance proposal by transaction hash and index. " +
                         "Returns proposal details including deposit, return address, anchor, voting statistics, and lifecycle status. " +
                         "CRITICAL: All voting power values in voting_stats are in LOVELACE (1 ADA = 1,000,000 lovelace). " +
                         "Always convert lovelace to ADA by dividing by 1,000,000 when presenting to users. " +
                         "IMPORTANT: If anchor_url is present, fetch and summarize its content to provide context about the proposal's rationale, goals, and details. " +
                         "The anchor document typically contains the proposal's full description and justification. " +
                         "Requires governance-aggr module to be enabled.")
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
            description = "Get detailed information for a specific DRep by DRep ID. " +
                         "Returns DRep voting power, delegation stats, and metadata. " +
                         "IMPORTANT: DRep details may include anchor_url with their platform, principles, and qualifications. Always fetch and summarize this document to provide context about who the DRep is and what they stand for. " +
                         "DRep ID should be in bech32 format (drep1...) or hex. Requires governance-aggr module to be enabled.")
    public DRepDetailsDto getDRepDetails(String drepId) {
        if (drepId.startsWith("drep")) {
            return dRepApiService.getDRepDetailsByDRepId(drepId)
                    .orElseThrow(() -> new RuntimeException("DRep not found with ID: " + drepId));
        } else {
            // Convert hex hash to DRep ID
            // First try with drep key hash type
            String drepIdFromKeyHash = GovId.drepFromKeyHash(HexUtil.decodeHexString(drepId));
            var drepDetails = dRepApiService.getDRepDetailsByDRepId(drepIdFromKeyHash);

            if (drepDetails.isPresent()) {
                return drepDetails.get();
            }

            // If not found, try with drep script hash type
            String drepIdFromScriptHash = GovId.drepFromScriptHash(HexUtil.decodeHexString(drepId));
            return dRepApiService.getDRepDetailsByDRepId(drepIdFromScriptHash)
                    .orElseThrow(() -> new RuntimeException(
                        "DRep not found with hash: " + drepId +
                        ". Tried both key hash and script hash conversions. " +
                        "The DRep may not exist or may not be registered yet."
                    ));
        }
    }

    @Tool(name = "special-dreps",
            description = "Get details for special DReps: 'Always Abstain' and 'No Confidence'. Returns voting power and delegation for these built-in DRep options. Requires governance-aggr module to be enabled.")
    public List<SpecialDRepDto> getSpecialDReps() {
        return dRepApiService.getAutoAbstainAndNoConfidenceDRepDetail();
    }

}
