package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VotingAggrService {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional
    public List<VotingProcedure> getVotesBySPO(int epoch, List<GovActionId> govActionIds) {
        if (govActionIds == null || govActionIds.isEmpty()) {
            return List.of();
        }

        String govActionPairsCondition = govActionIds.stream()
                .map(id -> "('" + id.getTransactionId() + "', " + id.getGov_action_index() + ")")
                .collect(Collectors.joining(", "));

        String sql = """
            WITH vote_with_max_slot AS (
                SELECT
                    vp.voter_hash,
                    vp.voter_type,
                    vp.gov_action_tx_hash,
                    vp.gov_action_index,
                    MAX(vp.slot) AS max_slot
                FROM voting_procedure vp
                WHERE vp.epoch <= :epoch
                  AND (vp.gov_action_tx_hash, vp.gov_action_index) IN (%s)
                GROUP BY
                    vp.voter_hash,
                    vp.voter_type,
                    vp.gov_action_tx_hash,
                    vp.gov_action_index
            )
            SELECT vp.*
            FROM voting_procedure vp
            JOIN vote_with_max_slot v
              ON vp.voter_hash = v.voter_hash
             AND vp.gov_action_tx_hash = v.gov_action_tx_hash
             AND vp.gov_action_index = v.gov_action_index
             AND vp.slot = v.max_slot
            WHERE vp.voter_type = 'STAKING_POOL_KEY_HASH'
              AND EXISTS (
                  SELECT 1
                  FROM epoch_stake es
                  WHERE es.pool_id = vp.voter_hash
                    AND es.epoch = :epoch
              );
            """.formatted(govActionPairsCondition);

        Map<String, Object> params = Map.of("epoch", epoch);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> VotingProcedure.builder()
                .id(UUID.fromString(rs.getString("id")))
                .txHash(rs.getString("tx_hash"))
                .index(rs.getLong("idx"))
                .slot(rs.getLong("slot"))
                .voterType(VoterType.STAKING_POOL_KEY_HASH)
                .voterHash(rs.getString("voter_hash"))
                .govActionTxHash(rs.getString("gov_action_tx_hash"))
                .govActionIndex(rs.getInt("gov_action_index"))
                .vote(Vote.valueOf(rs.getString("vote")))
                .anchorUrl(rs.getString("anchor_url"))
                .anchorHash(rs.getString("anchor_hash"))
                .epoch(rs.getInt("epoch"))
                .build());
    }

    @Transactional
    public List<VotingProcedure> getVotesByCommittee(int epoch, List<GovActionId> govActionIds) {
        if (govActionIds == null || govActionIds.isEmpty()) {
            return List.of();
        }

        String govActionPairsCondition = govActionIds.stream()
                .map(id -> "('" + id.getTransactionId() + "', " + id.getGov_action_index() + ")")
                .collect(Collectors.joining(", ")); // ('tx_hash1', 0), ('tx_hash2', 1), ...

        String sql = """
                WITH vote_with_max_slot AS (
                    SELECT
                        vp.voter_hash,
                        vp.voter_type,
                        vp.gov_action_tx_hash,
                        vp.gov_action_index,
                        MAX(vp.slot) AS max_slot
                    FROM voting_procedure vp
                    WHERE vp.epoch <= :epoch
                      AND (vp.gov_action_tx_hash, vp.gov_action_index) IN (%s)
                    GROUP BY
                        vp.voter_hash,
                        vp.voter_type,
                        vp.gov_action_tx_hash,
                        vp.gov_action_index
                )
                SELECT vp.*
                FROM voting_procedure vp
                JOIN vote_with_max_slot v
                  ON vp.voter_hash = v.voter_hash
                 AND vp.gov_action_tx_hash = v.gov_action_tx_hash
                 AND vp.gov_action_index = v.gov_action_index
                 AND vp.slot = v.max_slot
                WHERE vp.voter_type IN ('CONSTITUTIONAL_COMMITTEE_HOT_SCRIPT_HASH', 'CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH')
                  AND NOT EXISTS (
                      SELECT 1
                      FROM committee_deregistration cd
                      WHERE cd.cold_key = vp.voter_hash
                        AND cd.epoch <= :epoch
                  );
                """.formatted(govActionPairsCondition);

        Map<String, Object> params = Map.of("epoch", epoch);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> VotingProcedure.builder()
                .id(UUID.fromString(rs.getString("id")))
                .txHash(rs.getString("tx_hash"))
                .index(rs.getLong("idx"))
                .slot(rs.getLong("slot"))
                .voterType(VoterType.valueOf(rs.getString("voter_type")))
                .voterHash(rs.getString("voter_hash"))
                .govActionTxHash(rs.getString("gov_action_tx_hash"))
                .govActionIndex(rs.getInt("gov_action_index"))
                .vote(rs.getString("vote") != null ? Vote.valueOf(rs.getString("vote")) : null)
                .anchorUrl(rs.getString("anchor_url"))
                .anchorHash(rs.getString("anchor_hash"))
                .epoch(rs.getInt("epoch"))
                .build());
    }

    @Transactional
    public List<VotingProcedure> getVotesByDRep(int epoch, List<GovActionId> govActionIds) {
        if (govActionIds == null || govActionIds.isEmpty()) {
            return List.of();
        }

        String govActionPairsCondition = govActionIds.stream()
                .map(id -> "('" + id.getTransactionId() + "', " + id.getGov_action_index() + ")")
                .collect(Collectors.joining(", "));

        String sql = """
            WITH vote_with_max_slot AS (
                SELECT
                    vp.voter_hash,
                    vp.voter_type,
                    vp.gov_action_tx_hash,
                    vp.gov_action_index,
                    MAX(vp.slot) AS max_slot
                FROM voting_procedure vp
                WHERE vp.epoch <= :epoch
                  AND (vp.gov_action_tx_hash, vp.gov_action_index) IN (%s)
                GROUP BY
                    vp.voter_hash,
                    vp.voter_type,
                    vp.gov_action_tx_hash,
                    vp.gov_action_index
            )
            SELECT vp.*
            FROM voting_procedure vp
            JOIN vote_with_max_slot v
              ON vp.voter_hash = v.voter_hash
             AND vp.gov_action_tx_hash = v.gov_action_tx_hash
             AND vp.gov_action_index = v.gov_action_index
             AND vp.slot = v.max_slot
            WHERE vp.voter_type IN ('DREP_KEY_HASH', 'DREP_SCRIPT_HASH')
              AND EXISTS (
                  SELECT 1
                  FROM drep_dist dd
                  WHERE dd.drep_hash = vp.voter_hash
                    AND dd.epoch = :epoch
              );
            """.formatted(govActionPairsCondition);

        Map<String, Object> params = Map.of("epoch", epoch);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> VotingProcedure.builder()
                .id(UUID.fromString(rs.getString("id")))
                .txHash(rs.getString("tx_hash"))
                .index(rs.getLong("index"))
                .slot(rs.getLong("slot"))
                .voterType(VoterType.valueOf(rs.getString("voter_type")))
                .voterHash(rs.getString("voter_hash"))
                .govActionTxHash(rs.getString("gov_action_tx_hash"))
                .govActionIndex(rs.getInt("gov_action_index"))
                .vote(Vote.valueOf(rs.getString("vote")))
                .anchorUrl(rs.getString("anchor_url"))
                .anchorHash(rs.getString("anchor_hash"))
                .epoch(rs.getInt("epoch"))
                .build());
    }
}
