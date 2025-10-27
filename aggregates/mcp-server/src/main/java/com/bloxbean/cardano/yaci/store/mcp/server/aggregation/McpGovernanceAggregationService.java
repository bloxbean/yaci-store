package com.bloxbean.cardano.yaci.store.mcp.server.aggregation;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.service.ProposalApiService;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.CommitteeMemberStorageImpl;
import com.bloxbean.cardano.yaci.store.mcp.server.model.*;
import com.bloxbean.cardano.yaci.store.mcp.server.util.McpModelConverter;
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
        havingValue = "true",
        matchIfMissing = true
)
public class McpGovernanceAggregationService {
    private final ProposalApiService proposalApiService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CommitteeMemberStorageImpl committeeMemberStorage;

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
            description = "Get filtered list of governance proposals by status and type. " +
                    "IMPORTANT: Proposal status snapshots are stored per epoch in gov_action_proposal_status table. " +
                    "For current active/live proposals, use statusEpoch=0 (auto-detects current epoch). " +
                    "Filter by status (ACTIVE/RATIFIED/EXPIRED/ENACTED) and proposal type. " +
                    "Returns proposals with complete voting statistics. " +
                    "Voting stats include both lovelace and ADA values for clarity. " +
                    "Use this when you need to find proposals by their current or historical status. " +
                    "For finding proposals by submission date, use proposals-by-submission-epoch instead. " +
                    "ALL PARAMETERS ARE OPTIONAL - pass empty string \"\" for string params you don't want to filter by, or 0 for numeric params. " +
                    "Page is 0-based, default 50 results per page.")
    public List<ProposalMcp> getProposalsFiltered(
            @ToolParam(description = "Status filter: ACTIVE (open for voting), RATIFIED (approved), EXPIRED (rejected/timed out), ENACTED (executed), or empty string \"\" for no status filter (default: \"\")") String status,
            @ToolParam(description = "Proposal type filter: PARAMETER_CHANGE, HARD_FORK_INITIATION, TREASURY_WITHDRAWALS, NO_CONFIDENCE, UPDATE_COMMITTEE, NEW_CONSTITUTION, INFO_ACTION, or empty string \"\" for no type filter (default: \"\")") String type,
            @ToolParam(description = "Status snapshot epoch: which epoch's proposal status to query (default: 0 = current epoch). Set to specific epoch to see historical status.") Integer statusEpoch,
            @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
            @ToolParam(description = "Results per page (default: 50, max: 100)") Integer count
    ) {
        log.debug("Getting filtered proposals: page={}, count={}, status={}, type={}, statusEpoch={}",
                page, count, status, type, statusEpoch);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 100) : 50;
        int offset = effectivePage * effectiveCount;

        // Determine the status snapshot epoch to query
        Integer effectiveStatusEpoch = null;
        if (statusEpoch == null || statusEpoch == 0) {
            // Get current epoch from gov_action_proposal_status
            String currentEpochSql = "SELECT MAX(epoch) as current_epoch FROM gov_action_proposal_status";
            effectiveStatusEpoch = jdbcTemplate.queryForObject(currentEpochSql, new HashMap<>(), Integer.class);
        } else {
            effectiveStatusEpoch = statusEpoch;
        }

        // Build dynamic SQL based on filters
        // Filter by specific epoch to get unique proposals (one status record per proposal)
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT
                gap.tx_hash,
                gap.idx as index,
                gap.epoch,
                gap.slot
            FROM gov_action_proposal gap
            INNER JOIN gov_action_proposal_status gps
                ON gap.tx_hash = gps.gov_action_tx_hash
                AND gap.idx = gps.gov_action_index
            WHERE gps.epoch = :statusEpoch
            """);

        Map<String, Object> params = new HashMap<>();
        params.put("statusEpoch", effectiveStatusEpoch);

        if (status != null && !status.trim().isEmpty()) {
            sql.append(" AND gps.status = :status");
            params.put("status", status.toUpperCase());
        }

        if (type != null && !type.trim().isEmpty()) {
            sql.append(" AND gps.type = :type");
            params.put("type", type.toUpperCase());
        }

        sql.append("""

            ORDER BY gap.epoch DESC, gap.slot DESC
            LIMIT :limit OFFSET :offset
            """);

        params.put("limit", effectiveCount);
        params.put("offset", offset);

        // Fetch full proposal details with voting stats and convert to MCP models
        // Since we filter by epoch, we get only one status record per proposal (no duplicates)
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
                proposalApiService.getProposalById(
                        rs.getString("tx_hash"),
                        rs.getInt("index")
                ).map(McpModelConverter::toProposalMcp)
                .orElse(null)
        ).stream().filter(p -> p != null).toList();
    }

    @Tool(name = "proposals-by-submission-epoch",
            description = "Get proposals by when they were originally submitted to the chain. " +
                    "Useful for finding proposals submitted in a specific time period. " +
                    "Unlike proposals-filtered which filters by current/historical status, " +
                    "this tool filters by the proposal's original submission epoch (gap.epoch). " +
                    "Returns proposals with voting stats in both lovelace and ADA for clarity. " +
                    "Example: Find all ACTIVE treasury withdrawals submitted in last 5 epochs. " +
                    "Page is 0-based, default 50 results per page.")
    public List<ProposalMcp> getProposalsBySubmissionEpoch(
            @ToolParam(description = "Start epoch (inclusive), proposals submitted from this epoch onwards") Integer startEpoch,
            @ToolParam(description = "End epoch (inclusive), proposals submitted up to this epoch") Integer endEpoch,
            @ToolParam(description = "Optional status filter for current status: ACTIVE, RATIFIED, EXPIRED, ENACTED, or empty string \"\" for all statuses (default: \"\")") String currentStatus,
            @ToolParam(description = "Optional type filter: PARAMETER_CHANGE, HARD_FORK_INITIATION, TREASURY_WITHDRAWALS, etc., or empty string \"\" for all types (default: \"\")") String type,
            @ToolParam(description = "Page number (0-based, default: 0)") Integer page,
            @ToolParam(description = "Results per page (default: 50, max: 100)") Integer count
    ) {
        log.debug("Getting proposals by submission epoch: startEpoch={}, endEpoch={}, currentStatus={}, type={}, page={}, count={}",
                startEpoch, endEpoch, currentStatus, type, page, count);

        int effectivePage = (page != null && page >= 0) ? page : 0;
        int effectiveCount = (count != null && count > 0) ? Math.min(count, 100) : 50;
        int offset = effectivePage * effectiveCount;

        // Get current epoch for status filtering
        String currentEpochSql = "SELECT MAX(epoch) as current_epoch FROM gov_action_proposal_status";
        Integer currentEpoch = jdbcTemplate.queryForObject(currentEpochSql, new HashMap<>(), Integer.class);

        // Build SQL to filter by submission epoch
        StringBuilder sql = new StringBuilder("""
            SELECT DISTINCT
                gap.tx_hash,
                gap.idx as index,
                gap.epoch,
                gap.slot
            FROM gov_action_proposal gap
            """);

        // Only join status table if we need to filter by current status or type
        boolean needsStatusJoin = (currentStatus != null && !currentStatus.trim().isEmpty())
                || (type != null && !type.trim().isEmpty());

        if (needsStatusJoin) {
            sql.append("""
                INNER JOIN gov_action_proposal_status gps
                    ON gap.tx_hash = gps.gov_action_tx_hash
                    AND gap.idx = gps.gov_action_index
                    AND gps.epoch = :currentEpoch
                """);
        }

        sql.append(" WHERE 1=1");

        Map<String, Object> params = new HashMap<>();
        if (needsStatusJoin) {
            params.put("currentEpoch", currentEpoch);
        }

        // Filter by submission epoch range
        if (startEpoch != null && startEpoch > 0) {
            sql.append(" AND gap.epoch >= :startEpoch");
            params.put("startEpoch", startEpoch);
        }

        if (endEpoch != null && endEpoch > 0) {
            sql.append(" AND gap.epoch <= :endEpoch");
            params.put("endEpoch", endEpoch);
        }

        // Filter by current status if provided
        if (currentStatus != null && !currentStatus.trim().isEmpty()) {
            sql.append(" AND gps.status = :status");
            params.put("status", currentStatus.toUpperCase());
        }

        // Filter by type if provided
        if (type != null && !type.trim().isEmpty()) {
            sql.append(" AND gps.type = :type");
            params.put("type", type.toUpperCase());
        }

        sql.append("""

            ORDER BY gap.epoch DESC, gap.slot DESC
            LIMIT :limit OFFSET :offset
            """);

        params.put("limit", effectiveCount);
        params.put("offset", offset);

        // Fetch full proposal details and convert to MCP models
        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) ->
                proposalApiService.getProposalById(
                        rs.getString("tx_hash"),
                        rs.getInt("index")
                ).map(McpModelConverter::toProposalMcp)
                .orElse(null)
        ).stream().filter(p -> p != null).toList();
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
        long startTime = System.currentTimeMillis();
        log.info("Starting vote summary for proposal: {}#{}", govActionTxHash, govActionIndex);

        // Optimized query using COUNT(*) FILTER for better performance
        // This leverages the composite index on (gov_action_tx_hash, gov_action_index)
        String sql = """
            SELECT
                COUNT(*) as total_votes,
                COUNT(*) FILTER (WHERE vote = 'YES') as yes_votes,
                COUNT(*) FILTER (WHERE vote = 'NO') as no_votes,
                COUNT(*) FILTER (WHERE vote = 'ABSTAIN') as abstain_votes,
                COUNT(*) FILTER (WHERE voter_type = 'DREP') as drep_votes,
                COUNT(*) FILTER (WHERE voter_type = 'SPO') as spo_votes,
                COUNT(*) FILTER (WHERE voter_type = 'COMMITTEE') as committee_votes
            FROM voting_procedure
            WHERE gov_action_tx_hash = :govActionTxHash
              AND gov_action_index = :govActionIndex
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("govActionTxHash", govActionTxHash);
        params.put("govActionIndex", govActionIndex);

        log.info("Executing query for proposal: {}#{}", govActionTxHash, govActionIndex);

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

        long queryTime = System.currentTimeMillis() - startTime;
        log.info("Vote summary query completed for proposal: {}#{} in {}ms",
                 govActionTxHash, govActionIndex, queryTime);

        if (results.isEmpty() || results.get(0).totalVotes() == 0) {
            // Return empty summary if no votes found
            log.info("No votes found for proposal: {}#{}", govActionTxHash, govActionIndex);
            return new VoteSummary(
                    govActionTxHash, govActionIndex, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0
            );
        }

        VoteSummary summary = results.get(0);
        log.info("Returning vote summary for proposal: {}#{} - {} total votes in {}ms",
                 govActionTxHash, govActionIndex, summary.totalVotes(), queryTime);
        return summary;
    }

    @Tool(name = "governance-eligibility-by-epoch",
            description = "Get comprehensive statistics on eligible governance participants (DReps, SPOs, Constitutional Committee) at a specific epoch. " +
                    "Returns total counts of eligible voters for each participant category. " +
                    "CRITICAL for calculating historical governance participation rates: use these baseline numbers with proposal-vote-summary to compute participation percentages. " +
                    "Example: proposal-vote-summary shows 18 DRep votes, governance-eligibility-by-epoch shows 27 eligible DReps at the proposal's expiry epoch → participation rate = 18/27 = 66.7%. " +
                    "Works for any epoch - use proposal's expiry_epoch or enacted_epoch to analyze historical participation. " +
                    "DRep eligibility is based on active_until >= epoch and excludes predefined options (NO_CONFIDENCE/ABSTAIN). " +
                    "SPO eligibility is based on having delegated stake in epoch_stake. " +
                    "CC member eligibility is based on their term boundaries (start_epoch <= epoch < expired_epoch).")
    public GovernanceEligibility getGovernanceEligibilityByEpoch(
            @ToolParam(description = "Epoch number for which to calculate eligible voters") Integer epoch
    ) {
        log.debug("Getting governance eligibility for epoch: {}", epoch);

        if (epoch == null || epoch < 0) {
            throw new IllegalArgumentException("Epoch must be a non-negative integer");
        }

        // Single optimized CTE query to get all eligibility counts
        String sql = """
            WITH drep_stats AS (
              SELECT
                COUNT(DISTINCT drep_hash) FILTER (WHERE active_until >= :epoch
                                                    AND drep_type NOT IN ('NO_CONFIDENCE', 'ABSTAIN')) as eligible_dreps
              FROM drep_dist
              WHERE epoch = :epoch
            ),
            spo_stats AS (
              SELECT
                COUNT(DISTINCT pool_id) as eligible_spos
              FROM epoch_stake
              WHERE active_epoch = :epoch
            )
            SELECT
              :epoch as epoch,
              d.eligible_dreps,
              s.eligible_spos              
            FROM drep_stats d, spo_stats s
            """;

        Map<String, Object> params = new HashMap<>();
        params.put("epoch", epoch);

        long ccMembers = committeeMemberStorage.getActiveCommitteeMembersDetailsByEpoch(epoch)
                .stream().count();

        List<GovernanceEligibility> results = jdbcTemplate.query(sql, params, (rs, rowNum) ->
            new GovernanceEligibility(
                rs.getInt("epoch"),
                rs.getInt("eligible_dreps"),
                rs.getInt("eligible_spos"),
                (int) ccMembers
            )
        );

        if (results.isEmpty()) {
            log.warn("No governance eligibility data found for epoch: {}", epoch);
            // Return zeros if no data found (epoch might be in the future or before governance started)
            return new GovernanceEligibility(epoch, 0, 0, 0);
        }

        GovernanceEligibility eligibility = results.get(0);
        log.debug("Governance eligibility for epoch {}: {} DReps, {} SPOs, {} CC members",
                epoch, eligibility.eligibleDReps(), eligibility.eligibleSPOs(), eligibility.eligibleCCMembers());

        return eligibility;
    }
}
