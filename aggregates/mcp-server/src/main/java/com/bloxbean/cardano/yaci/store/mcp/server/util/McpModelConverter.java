package com.bloxbean.cardano.yaci.store.mcp.server.util;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepDetailsDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.bloxbean.cardano.yaci.store.mcp.server.model.DRepDetailsMcp;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ProposalMcp;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ProposalVotingStatsMcp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Utility class for converting core governance DTOs to MCP-optimized models.
 *
 * MCP models include both lovelace (raw blockchain values) and ADA (human-readable)
 * for all monetary fields to prevent LLM misinterpretation.
 */
public class McpModelConverter {

    /**
     * Converts core ProposalDto to MCP-optimized ProposalMcp.
     *
     * @param dto core proposal DTO from governance-aggr module
     * @return MCP proposal with dual lovelace/ADA values
     */
    public static ProposalMcp toProposalMcp(ProposalDto dto) {
        if (dto == null) {
            return null;
        }

        return ProposalMcp.from(dto);
    }

    /**
     * Converts core DRepDetailsDto to MCP-optimized DRepDetailsMcp.
     *
     * @param dto core DRep details DTO from governance-aggr module
     * @return MCP DRep details with dual lovelace/ADA values
     */
    public static DRepDetailsMcp toDRepDetailsMcp(DRepDetailsDto dto) {
        if (dto == null) {
            return null;
        }

        return DRepDetailsMcp.from(dto);
    }

    /**
     * Converts core ProposalVotingStats to MCP-optimized ProposalVotingStatsMcp.
     *
     * @param stats core voting stats from governance-aggr module
     * @return MCP voting stats with dual lovelace/ADA values
     */
    public static ProposalVotingStatsMcp toVotingStatsMcp(ProposalVotingStats stats) {
        if (stats == null) {
            return null;
        }

        return ProposalVotingStatsMcp.from(stats);
    }
}
