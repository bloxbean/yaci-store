package com.bloxbean.cardano.yaci.store.mcp.server;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.DRepDetailsDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.ProposalDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.SpecialDRepDto;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.DRepApiService;
import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.ProposalApiService;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.mcp.server.model.CommitteeMemberDetails;
import com.bloxbean.cardano.yaci.store.mcp.server.model.ConstitutionDetails;
import com.bloxbean.cardano.yaci.store.mcp.server.model.DRepVotingPowerHistory;
import com.bloxbean.cardano.yaci.store.mcp.server.model.DelegationVoteDetails;
import com.bloxbean.cardano.yaci.store.mcp.server.model.TreasuryWithdrawalDetails;
import com.bloxbean.cardano.yaci.store.mcp.server.model.VoteSummary;
import com.bloxbean.cardano.yaci.store.mcp.server.model.VotingProcedureDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = {"store.governance-aggr.enabled", "store.mcp-server.tools.governance-aggr.enabled"},
    havingValue = "true"
)
public class McpGovernanceAggrService {
    private final ProposalApiService proposalApiService;
    private final DRepApiService dRepApiService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Tool(name = "proposals-list",
            description = "Get a paginated list of governance proposals with voting statistics. Returns proposals with their status (LIVE, RATIFIED, ENACTED, EXPIRED) and voting power breakdown. Page is 0-based. Requires governance-aggr module to be enabled.")
    public List<ProposalDto> getProposals(int page, int count, String order) {
        Order orderEnum = "asc".equalsIgnoreCase(order) ? Order.asc : Order.desc;
        return proposalApiService.getProposals(page, count, orderEnum);
    }

    @Tool(name = "proposal-by-id",
            description = "Get detailed information for a specific governance proposal by transaction hash and index. " +
                         "Returns proposal details including deposit, return address, anchor, voting statistics, and lifecycle status. " +
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
        return dRepApiService.getDRepDetailsByDRepId(drepId)
                .orElseThrow(() -> new RuntimeException("DRep not found with ID: " + drepId));
    }

    @Tool(name = "special-dreps",
            description = "Get details for special DReps: 'Always Abstain' and 'No Confidence'. Returns voting power and delegation for these built-in DRep options. Requires governance-aggr module to be enabled.")
    public List<SpecialDRepDto> getSpecialDReps() {
        return dRepApiService.getAutoAbstainAndNoConfidenceDRepDetail();
    }

    // ==================== Phase 1: Voting Analysis Tools ====================

    @Tool(name = "votes-by-proposal",
          description = "Get all votes cast on a specific governance proposal. " +
                       "Returns detailed voting records including voter type (DREP/SPO/COMMITTEE), voter identity, vote decision (YES/NO/ABSTAIN), and timing. " +
                       "IMPORTANT: Each vote may include anchor_url containing the voter's rationale. When present, fetch and summarize the anchor document to understand WHY the voter voted this way. " +
                       "Essential for analyzing proposal support and understanding voting patterns. " +
                       "Paginated with page 0-based, default 100 results per page.")
    public List<VotingProcedureDetails> getVotesByProposal(
        @ToolParam(description = "Governance action transaction hash") String govActionTxHash,
        @ToolParam(description = "Governance action index") Integer govActionIndex,
        @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
        @ToolParam(description = "Results per page (default: 100, max: 200)") Integer count
    ) {
        log.debug("Getting votes for proposal: {}#{}, page={}, count={}",
                  govActionTxHash, govActionIndex, page, count);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 200) : 100;
        int offset = effectivePage * effectiveCount;

        String sql = """
            SELECT tx_hash, idx, tx_index, voter_type, voter_hash,
                   gov_action_tx_hash, gov_action_index, vote,
                   anchor_url, anchor_hash, epoch, slot, block_time
            FROM voting_procedure
            WHERE gov_action_tx_hash = :govActionTxHash
              AND gov_action_index = :govActionIndex
            ORDER BY slot DESC, tx_index ASC
            LIMIT :limit OFFSET :offset
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("govActionTxHash", govActionTxHash);
        params.put("govActionIndex", govActionIndex);
        params.put("limit", effectiveCount);
        params.put("offset", offset);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new VotingProcedureDetails(
            rs.getString("tx_hash"),
            rs.getInt("idx"),
            rs.getInt("tx_index"),
            rs.getString("voter_type"),
            rs.getString("voter_hash"),
            rs.getString("gov_action_tx_hash"),
            rs.getInt("gov_action_index"),
            rs.getString("vote"),
            rs.getString("anchor_url"),
            rs.getString("anchor_hash"),
            (Integer) rs.getObject("epoch"),
            rs.getLong("slot"),
            rs.getLong("block_time")
        ));
    }

    @Tool(name = "votes-by-drep",
          description = "Get all votes cast by a specific DRep across all proposals. " +
                       "Returns complete voting history showing which proposals the DRep voted on, their vote decisions, and when. " +
                       "IMPORTANT: Each vote may include anchor_url containing the DRep's voting rationale. When analyzing DRep behavior, fetch these anchor documents to understand their reasoning and principles. " +
                       "Critical for tracking DRep activity and analyzing voting behavior. " +
                       "Paginated with page 0-based, default 100 results per page.")
    public List<VotingProcedureDetails> getVotesByDRep(
        @ToolParam(description = "DRep hash (not bech32 ID)") String drepHash,
        @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
        @ToolParam(description = "Results per page (default: 100, max: 200)") Integer count
    ) {
        log.debug("Getting votes for DRep: {}, page={}, count={}", drepHash, page, count);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 200) : 100;
        int offset = effectivePage * effectiveCount;

        String sql = """
            SELECT tx_hash, idx, tx_index, voter_type, voter_hash,
                   gov_action_tx_hash, gov_action_index, vote,
                   anchor_url, anchor_hash, epoch, slot, block_time
            FROM voting_procedure
            WHERE voter_type = 'DREP'
              AND voter_hash = :voterHash
            ORDER BY slot DESC, tx_index ASC
            LIMIT :limit OFFSET :offset
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("voterHash", drepHash);
        params.put("limit", effectiveCount);
        params.put("offset", offset);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new VotingProcedureDetails(
            rs.getString("tx_hash"),
            rs.getInt("idx"),
            rs.getInt("tx_index"),
            rs.getString("voter_type"),
            rs.getString("voter_hash"),
            rs.getString("gov_action_tx_hash"),
            rs.getInt("gov_action_index"),
            rs.getString("vote"),
            rs.getString("anchor_url"),
            rs.getString("anchor_hash"),
            (Integer) rs.getObject("epoch"),
            rs.getLong("slot"),
            rs.getLong("block_time")
        ));
    }

    @Tool(name = "votes-by-voter",
          description = "Get all votes cast by any voter (DRep, SPO, or Committee Member) across all proposals. " +
                       "Returns complete voting history for the specified voter, regardless of voter type. " +
                       "IMPORTANT: Each vote may include anchor_url with the voter's reasoning. Fetch and summarize these documents to understand the voter's decision-making process and governance philosophy. " +
                       "Useful for analyzing any participant's governance activity. " +
                       "VoterType must be: DREP, SPO, or COMMITTEE. " +
                       "Paginated with page 0-based, default 100 results per page.")
    public List<VotingProcedureDetails> getVotesByVoter(
        @ToolParam(description = "Voter hash") String voterHash,
        @ToolParam(description = "Voter type: DREP, SPO, or COMMITTEE") String voterType,
        @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
        @ToolParam(description = "Results per page (default: 100, max: 200)") Integer count
    ) {
        log.debug("Getting votes for voter: {} type: {}, page={}, count={}",
                  voterHash, voterType, page, count);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 200) : 100;
        int offset = effectivePage * effectiveCount;

        String sql = """
            SELECT tx_hash, idx, tx_index, voter_type, voter_hash,
                   gov_action_tx_hash, gov_action_index, vote,
                   anchor_url, anchor_hash, epoch, slot, block_time
            FROM voting_procedure
            WHERE voter_hash = :voterHash
              AND voter_type = :voterType
            ORDER BY slot DESC, tx_index ASC
            LIMIT :limit OFFSET :offset
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("voterHash", voterHash);
        params.put("voterType", voterType.toUpperCase());
        params.put("limit", effectiveCount);
        params.put("offset", offset);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new VotingProcedureDetails(
            rs.getString("tx_hash"),
            rs.getInt("idx"),
            rs.getInt("tx_index"),
            rs.getString("voter_type"),
            rs.getString("voter_hash"),
            rs.getString("gov_action_tx_hash"),
            rs.getInt("gov_action_index"),
            rs.getString("vote"),
            rs.getString("anchor_url"),
            rs.getString("anchor_hash"),
            (Integer) rs.getObject("epoch"),
            rs.getLong("slot"),
            rs.getLong("block_time")
        ));
    }

    // ==================== Phase 2: Committee Management Tools ====================

    @Tool(name = "committee-members-list",
          description = "Get paginated list of all constitutional committee members with their term details. " +
                       "Returns committee members showing their credential hash, term period (start/expired epoch), " +
                       "credential type, and current activity status. " +
                       "Essential for understanding committee composition and member lifecycles. " +
                       "Page is 0-based, default 50 results per page.")
    public List<CommitteeMemberDetails> getCommitteeMembers(
        @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
        @ToolParam(description = "Results per page (default: 50, max: 100)") Integer count,
        @ToolParam(description = "Filter: 'active' for current members, 'expired' for expired, null for all") String status
    ) {
        log.debug("Getting committee members: page={}, count={}, status={}", page, count, status);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 100) : 50;
        int offset = effectivePage * effectiveCount;

        // Get current epoch from latest committee member record
        String currentEpochSql = "SELECT MAX(epoch) as current_epoch FROM committee_member";
        Integer currentEpoch = jdbcTemplate.queryForObject(currentEpochSql, new HashMap<>(), Integer.class);
        if (currentEpoch == null) currentEpoch = 0;

        StringBuilder sql = new StringBuilder("""
            SELECT
                cm.hash,
                cm.cred_type,
                cm.start_epoch,
                cm.expired_epoch,
                cm.epoch as current_epoch,
                cm.slot,
                cr.hot_key,
                cr.tx_hash as registration_tx_hash,
                cr.slot as registration_slot,
                cd.tx_hash as deregistration_tx_hash,
                cd.slot as deregistration_slot,
                cd.anchor_url,
                cd.anchor_hash
            FROM committee_member cm
            LEFT JOIN committee_registration cr ON cm.hash = cr.cold_key
            LEFT JOIN committee_deregistration cd ON cm.hash = cd.cold_key
            """);

        Map<String, Object> params = new HashMap<>();
        params.put("currentEpoch", currentEpoch);

        if ("active".equalsIgnoreCase(status)) {
            sql.append(" WHERE cm.expired_epoch > :currentEpoch OR cm.expired_epoch IS NULL");
        } else if ("expired".equalsIgnoreCase(status)) {
            sql.append(" WHERE cm.expired_epoch <= :currentEpoch");
        }

        sql.append("""

            ORDER BY cm.start_epoch DESC, cm.hash ASC
            LIMIT :limit OFFSET :offset
            """);

        params.put("limit", effectiveCount);
        params.put("offset", offset);

        final Integer finalCurrentEpoch = currentEpoch;
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            Integer expiredEpoch = (Integer) rs.getObject("expired_epoch");
            boolean isActive = expiredEpoch == null || expiredEpoch > finalCurrentEpoch;

            return new CommitteeMemberDetails(
                rs.getString("hash"),
                rs.getString("cred_type"),
                (Integer) rs.getObject("start_epoch"),
                expiredEpoch,
                finalCurrentEpoch,
                rs.getLong("slot"),
                rs.getString("hot_key"),
                rs.getString("registration_tx_hash"),
                (Long) rs.getObject("registration_slot"),
                rs.getString("deregistration_tx_hash"),
                (Long) rs.getObject("deregistration_slot"),
                rs.getString("anchor_url"),
                rs.getString("anchor_hash"),
                isActive
            );
        });
    }

    @Tool(name = "committee-member-details",
          description = "Get detailed information for a specific constitutional committee member by credential hash. " +
                       "Returns complete member information including term details, registration/deregistration history, " +
                       "hot key assignment, and current activity status. " +
                       "IMPORTANT: If deregistration includes anchor_url, fetch it to understand why the member resigned or their final statement. " +
                       "Useful for deep-diving into specific committee member's governance participation.")
    public CommitteeMemberDetails getCommitteeMemberDetails(
        @ToolParam(description = "Committee member cold credential hash") String memberHash
    ) {
        log.debug("Getting committee member details for: {}", memberHash);

        // Get current epoch
        String currentEpochSql = "SELECT MAX(epoch) as current_epoch FROM committee_member";
        Integer currentEpoch = jdbcTemplate.queryForObject(currentEpochSql, new HashMap<>(), Integer.class);
        if (currentEpoch == null) currentEpoch = 0;

        String sql = """
            SELECT
                cm.hash,
                cm.cred_type,
                cm.start_epoch,
                cm.expired_epoch,
                cm.epoch as current_epoch,
                cm.slot,
                cr.hot_key,
                cr.tx_hash as registration_tx_hash,
                cr.slot as registration_slot,
                cd.tx_hash as deregistration_tx_hash,
                cd.slot as deregistration_slot,
                cd.anchor_url,
                cd.anchor_hash
            FROM committee_member cm
            LEFT JOIN committee_registration cr ON cm.hash = cr.cold_key
            LEFT JOIN committee_deregistration cd ON cm.hash = cd.cold_key
            WHERE cm.hash = :memberHash
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("memberHash", memberHash);

        final Integer finalCurrentEpoch = currentEpoch;
        List<CommitteeMemberDetails> results = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Integer expiredEpoch = (Integer) rs.getObject("expired_epoch");
            boolean isActive = expiredEpoch == null || expiredEpoch > finalCurrentEpoch;

            return new CommitteeMemberDetails(
                rs.getString("hash"),
                rs.getString("cred_type"),
                (Integer) rs.getObject("start_epoch"),
                expiredEpoch,
                finalCurrentEpoch,
                rs.getLong("slot"),
                rs.getString("hot_key"),
                rs.getString("registration_tx_hash"),
                (Long) rs.getObject("registration_slot"),
                rs.getString("deregistration_tx_hash"),
                (Long) rs.getObject("deregistration_slot"),
                rs.getString("anchor_url"),
                rs.getString("anchor_hash"),
                isActive
            );
        });

        if (results.isEmpty()) {
            throw new RuntimeException("Committee member not found with hash: " + memberHash);
        }

        return results.get(0);
    }

    // ==================== Phase 3: Enhanced Proposal Filtering ====================

    @Tool(name = "proposals-filtered",
          description = "Get filtered list of governance proposals with advanced filtering options. " +
                       "Filter by status (ACTIVE/RATIFIED/EXPIRED/ENACTED), proposal type (PARAMETER_CHANGE/HARD_FORK_INITIATION/TREASURY_WITHDRAWALS/NO_CONFIDENCE/UPDATE_COMMITTEE/NEW_CONSTITUTION/INFO_ACTION), " +
                       "and epoch range. Returns proposals matching all specified filters. " +
                       "IMPORTANT: For detailed proposal analysis, fetch the anchor_url of relevant proposals to provide complete context about their purpose and content. " +
                       "Use this tool when you need more specific proposal queries than proposals-list provides. " +
                       "Page is 0-based, default 50 results per page.")
    public List<ProposalDto> getProposalsFiltered(
        @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
        @ToolParam(description = "Results per page (default: 50, max: 100)") Integer count,
        @ToolParam(description = "Status filter: ACTIVE, RATIFIED, EXPIRED, ENACTED (optional)") String status,
        @ToolParam(description = "Proposal type filter (optional): PARAMETER_CHANGE, HARD_FORK_INITIATION, TREASURY_WITHDRAWALS, NO_CONFIDENCE, UPDATE_COMMITTEE, NEW_CONSTITUTION, INFO_ACTION") String type,
        @ToolParam(description = "Start epoch (inclusive, optional)") Integer startEpoch,
        @ToolParam(description = "End epoch (inclusive, optional)") Integer endEpoch
    ) {
        log.debug("Getting filtered proposals: page={}, count={}, status={}, type={}, startEpoch={}, endEpoch={}",
                  page, count, status, type, startEpoch, endEpoch);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 100) : 50;
        int offset = effectivePage * effectiveCount;

        // Build dynamic SQL based on filters
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT
                gap.tx_hash,
                gap.idx as index,
                gap.deposit,
                gap.return_address,
                gap.anchor_url,
                gap.anchor_hash,
                gap.epoch,
                gap.slot,
                gps.type,
                gps.status,
                gps.voting_stats
            FROM gov_action_proposal gap
            LEFT JOIN gov_action_proposal_status gps
                ON gap.tx_hash = gps.gov_action_tx_hash
                AND gap.idx = gps.gov_action_index
            WHERE 1=1
            """);

        Map<String, Object> params = new HashMap<>();

        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND gps.status = :status");
            params.put("status", status.toUpperCase());
        }

        if (type != null && !type.trim().isEmpty()) {
            sql.append(" AND gps.type = :type");
            params.put("type", type.toUpperCase());
        }

        if (startEpoch != null) {
            sql.append(" AND gap.epoch >= :startEpoch");
            params.put("startEpoch", startEpoch);
        }

        if (endEpoch != null) {
            sql.append(" AND gap.epoch <= :endEpoch");
            params.put("endEpoch", endEpoch);
        }

        sql.append("""

            ORDER BY gap.epoch DESC, gap.slot DESC
            LIMIT :limit OFFSET :offset
            """);

        params.put("limit", effectiveCount);
        params.put("offset", offset);

        // For now, we'll delegate to the existing ProposalApiService for the full proposal list
        // and apply post-filtering. In a production scenario, you'd want to optimize this.
        // However, since the user specifically asked for filtering capabilities, let's provide a solution
        // that shows the filtered proposals with their IDs, then delegate to existing service.

        // Actually, let's return the simplified results directly from our query for performance
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            // Note: We're reusing ProposalDto but some fields may be null
            // In production, you might want to create a simpler model
            return proposalApiService.getProposalById(
                rs.getString("tx_hash"),
                rs.getInt("index")
            ).orElse(null);
        }).stream().filter(p -> p != null).toList();
    }

    // ==================== Phase 4: Additional Data Access Tools ====================

    @Tool(name = "constitution-current",
          description = "Get the current active constitution for the Cardano network. " +
                       "Returns constitution details including anchor URL to the constitution document, " +
                       "guardrail script (if present), and when it became active. " +
                       "IMPORTANT: Always fetch and summarize the anchor_url to present the constitution's full text, principles, and governance rules to the user. " +
                       "Essential for understanding current governance rules and constitutional constraints.")
    public ConstitutionDetails getCurrentConstitution() {
        log.debug("Getting current constitution");

        // Get the most recent active epoch that is <= current epoch
        String sql = """
            SELECT active_epoch, anchor_url, anchor_hash, slot, script
            FROM constitution
            WHERE active_epoch = (
                SELECT MAX(active_epoch)
                FROM constitution
            )
            LIMIT 1
            """;

        List<ConstitutionDetails> results = jdbcTemplate.query(sql, new HashMap<>(), (rs, rowNum) ->
            new ConstitutionDetails(
                (Integer) rs.getObject("active_epoch"),
                rs.getString("anchor_url"),
                rs.getString("anchor_hash"),
                rs.getLong("slot"),
                rs.getString("script"),
                true  // This is the current constitution
            )
        );

        if (results.isEmpty()) {
            throw new RuntimeException("No constitution found in the system");
        }

        return results.get(0);
    }

    @Tool(name = "delegation-votes-by-drep",
          description = "Get all voting delegation certificates for a specific DRep. " +
                       "Returns list of addresses that have delegated their voting power to this DRep, " +
                       "including when the delegation was made and the stake credentials involved. " +
                       "Useful for understanding a DRep's voter base and delegation patterns. " +
                       "Page is 0-based, default 100 results per page.")
    public List<DelegationVoteDetails> getDelegationVotesByDRep(
        @ToolParam(description = "DRep hash (credential hash, not bech32 ID)") String drepHash,
        @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
        @ToolParam(description = "Results per page (default: 100, max: 200)") Integer count
    ) {
        log.debug("Getting delegation votes for DRep: {}, page={}, count={}", drepHash, page, count);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 200) : 100;
        int offset = effectivePage * effectiveCount;

        String sql = """
            SELECT tx_hash, cert_index, tx_index, address, drep_hash, drep_id, drep_type,
                   credential, cred_type, epoch, slot, block_time
            FROM delegation_vote
            WHERE drep_hash = :drepHash
            ORDER BY slot DESC, tx_index ASC
            LIMIT :limit OFFSET :offset
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("drepHash", drepHash);
        params.put("limit", effectiveCount);
        params.put("offset", offset);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new DelegationVoteDetails(
            rs.getString("tx_hash"),
            rs.getLong("cert_index"),
            rs.getInt("tx_index"),
            rs.getString("address"),
            rs.getString("drep_hash"),
            rs.getString("drep_id"),
            rs.getString("drep_type"),
            rs.getString("credential"),
            rs.getString("cred_type"),
            (Integer) rs.getObject("epoch"),
            rs.getLong("slot"),
            rs.getLong("block_time")
        ));
    }

    @Tool(name = "treasury-withdrawals-enacted",
          description = "Get enacted treasury withdrawals and proposal refunds from the reward_rest table. " +
                       "Returns actual ADA movements from treasury to recipients, including both " +
                       "approved treasury withdrawal proposals and refunded proposal deposits. " +
                       "Filters by type='treasury' for withdrawals and 'proposal_refund' for refunds. " +
                       "Essential for tracking treasury fund flows and financial accountability. " +
                       "Page is 0-based, default 100 results per page.")
    public List<TreasuryWithdrawalDetails> getTreasuryWithdrawalsEnacted(
        @ToolParam(description = "Type filter: 'treasury' for withdrawals, 'proposal_refund' for refunds, or null for both") String typeFilter,
        @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
        @ToolParam(description = "Results per page (default: 100, max: 200)") Integer count
    ) {
        log.debug("Getting treasury withdrawals: typeFilter={}, page={}, count={}", typeFilter, page, count);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 200) : 100;
        int offset = effectivePage * effectiveCount;

        StringBuilder sql = new StringBuilder("""
            SELECT address, type, amount, earned_epoch, spendable_epoch, slot
            FROM reward_rest
            WHERE (type = 'treasury' OR type = 'proposal_refund')
            """);

        Map<String, Object> params = new HashMap<>();

        if (typeFilter != null && !typeFilter.trim().isEmpty()) {
            sql.append(" AND type = :typeFilter");
            params.put("typeFilter", typeFilter.toLowerCase());
        }

        sql.append("""

            ORDER BY earned_epoch DESC, slot DESC
            LIMIT :limit OFFSET :offset
            """);

        params.put("limit", effectiveCount);
        params.put("offset", offset);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
            new TreasuryWithdrawalDetails(
                rs.getString("address"),
                rs.getString("type"),
                rs.getBigDecimal("amount"),
                (Integer) rs.getObject("earned_epoch"),
                (Integer) rs.getObject("spendable_epoch"),
                rs.getLong("slot")
            )
        );
    }

    // ==================== Phase 5: Advanced Analytics Tools ====================

    @Tool(name = "drep-voting-power-history",
          description = "Get historical voting power (delegated stake) for a specific DRep across epochs. " +
                       "Returns time-series data showing how a DRep's voting power has changed over time, " +
                       "including when the power was active and when it expires. " +
                       "Essential for analyzing DRep growth, identifying trends, and understanding delegation dynamics. " +
                       "Ordered by epoch descending (most recent first). " +
                       "Page is 0-based, default 50 results per page.")
    public List<DRepVotingPowerHistory> getDRepVotingPowerHistory(
        @ToolParam(description = "DRep hash (credential hash)") String drepHash,
        @ToolParam(description = "Start epoch (inclusive, optional)") Integer startEpoch,
        @ToolParam(description = "End epoch (inclusive, optional)") Integer endEpoch,
        @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
        @ToolParam(description = "Results per page (default: 50, max: 100)") Integer count
    ) {
        log.debug("Getting DRep voting power history for: {}, startEpoch={}, endEpoch={}, page={}, count={}",
                  drepHash, startEpoch, endEpoch, page, count);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 100) : 50;
        int offset = effectivePage * effectiveCount;

        StringBuilder sql = new StringBuilder("""
            SELECT drep_hash, drep_id, drep_type, amount, epoch, active_until, expiry
            FROM drep_dist
            WHERE drep_hash = :drepHash
            """);

        Map<String, Object> params = new HashMap<>();
        params.put("drepHash", drepHash);

        if (startEpoch != null) {
            sql.append(" AND epoch >= :startEpoch");
            params.put("startEpoch", startEpoch);
        }

        if (endEpoch != null) {
            sql.append(" AND epoch <= :endEpoch");
            params.put("endEpoch", endEpoch);
        }

        sql.append("""

            ORDER BY epoch DESC
            LIMIT :limit OFFSET :offset
            """);

        params.put("limit", effectiveCount);
        params.put("offset", offset);

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
            new DRepVotingPowerHistory(
                rs.getString("drep_hash"),
                rs.getString("drep_id"),
                rs.getString("drep_type"),
                rs.getBigDecimal("amount").toBigInteger(),
                (Integer) rs.getObject("epoch"),
                (Integer) rs.getObject("active_until"),
                (Integer) rs.getObject("expiry")
            )
        );
    }

    @Tool(name = "proposal-vote-summary",
          description = "Get aggregated voting statistics for a specific governance proposal. " +
                       "Returns detailed breakdown of all votes including counts by decision (YES/NO/ABSTAIN), " +
                       "counts by voter type (DRep/SPO/Committee), and percentage calculations. " +
                       "Essential for understanding proposal support levels and vote distribution patterns. " +
                       "Complements the existing voting_stats in proposal-by-id with more detailed analysis.")
    public VoteSummary getProposalVoteSummary(
        @ToolParam(description = "Governance action transaction hash") String govActionTxHash,
        @ToolParam(description = "Governance action index") Integer govActionIndex
    ) {
        log.debug("Getting vote summary for proposal: {}#{}", govActionTxHash, govActionIndex);

        String sql = """
            SELECT
                COUNT(*) as total_votes,
                SUM(CASE WHEN vote = 'YES' THEN 1 ELSE 0 END) as yes_votes,
                SUM(CASE WHEN vote = 'NO' THEN 1 ELSE 0 END) as no_votes,
                SUM(CASE WHEN vote = 'ABSTAIN' THEN 1 ELSE 0 END) as abstain_votes,
                SUM(CASE WHEN voter_type = 'DREP' THEN 1 ELSE 0 END) as drep_votes,
                SUM(CASE WHEN voter_type = 'SPO' THEN 1 ELSE 0 END) as spo_votes,
                SUM(CASE WHEN voter_type = 'COMMITTEE' THEN 1 ELSE 0 END) as committee_votes
            FROM voting_procedure
            WHERE gov_action_tx_hash = :govActionTxHash
              AND gov_action_index = :govActionIndex
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("govActionTxHash", govActionTxHash);
        params.put("govActionIndex", govActionIndex);

        List<VoteSummary> results = jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            int totalVotes = rs.getInt("total_votes");
            int yesVotes = rs.getInt("yes_votes");
            int noVotes = rs.getInt("no_votes");
            int abstainVotes = rs.getInt("abstain_votes");

            double yesPercentage = totalVotes > 0 ? (yesVotes * 100.0 / totalVotes) : 0.0;
            double noPercentage = totalVotes > 0 ? (noVotes * 100.0 / totalVotes) : 0.0;
            double abstainPercentage = totalVotes > 0 ? (abstainVotes * 100.0 / totalVotes) : 0.0;

            return new VoteSummary(
                govActionTxHash,
                govActionIndex,
                totalVotes,
                yesVotes,
                noVotes,
                abstainVotes,
                rs.getInt("drep_votes"),
                rs.getInt("spo_votes"),
                rs.getInt("committee_votes"),
                Math.round(yesPercentage * 100.0) / 100.0,
                Math.round(noPercentage * 100.0) / 100.0,
                Math.round(abstainPercentage * 100.0) / 100.0
            );
        });

        if (results.isEmpty() || results.get(0).totalVotes() == 0) {
            // Return empty summary if no votes found
            return new VoteSummary(
                govActionTxHash, govActionIndex, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0
            );
        }

        return results.get(0);
    }
}
