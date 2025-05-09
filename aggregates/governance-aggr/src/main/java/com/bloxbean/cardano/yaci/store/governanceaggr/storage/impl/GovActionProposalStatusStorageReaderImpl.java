package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.yaci.core.model.governance.GovActionId;
import com.bloxbean.cardano.yaci.core.model.governance.GovActionType;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionProposalStatus;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.ProposalVotingStats;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.GovActionProposalStatusStorageReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.GOV_ACTION_PROPOSAL_STATUS;

@RequiredArgsConstructor
public class GovActionProposalStatusStorageReaderImpl implements GovActionProposalStatusStorageReader {
    private final DSLContext dsl;

    @Override
    public List<GovActionProposalStatus> findLatestStatusesForProposals(List<GovActionId> govActionIds) {
        if (govActionIds.isEmpty())
            return Collections.emptyList();

        var g = GOV_ACTION_PROPOSAL_STATUS.as("g");

        Field<String> txHashField = g.field(GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_TX_HASH);
        Field<Integer> indexField = g.field(GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_INDEX);
        Field<String> typeField = g.field(GOV_ACTION_PROPOSAL_STATUS.TYPE);
        Field<String> statusField = g.field(GOV_ACTION_PROPOSAL_STATUS.STATUS);
        Field<Integer> epochField = g.field(GOV_ACTION_PROPOSAL_STATUS.EPOCH);
        Field<?> updateDatetimeField = g.field(GOV_ACTION_PROPOSAL_STATUS.UPDATE_DATETIME);
        Field<?> votingStatsField = g.field(GOV_ACTION_PROPOSAL_STATUS.VOTING_STATS);

        Field<Integer> rowNumber = DSL.rowNumber().over()
                .partitionBy(txHashField, indexField)
                .orderBy(epochField.desc())
                .as("rn");

        Table<?> subquery = dsl
                .select(txHashField, indexField, typeField, statusField, epochField, updateDatetimeField, votingStatsField, rowNumber)
                .from(g)
                .asTable("t");

        var idSet = govActionIds.stream()
                .map(id -> DSL.row(id.getTransactionId(), id.getGov_action_index()))
                .collect(Collectors.toSet());

        var result = dsl.selectFrom(subquery)
                .where(DSL.field("rn", Integer.class).eq(1))
                .and(DSL.row(
                                DSL.field(txHashField.getUnqualifiedName(), String.class),
                                DSL.field(indexField.getUnqualifiedName(), Integer.class))
                        .in(idSet)
                )
                .fetch();

        return result.stream()
                .map(r ->
                {
                    ProposalVotingStats votingStats = null;
                    try {
                        String jsonStr = r.get(votingStatsField, String.class);
                        if (jsonStr != null) {
                            votingStats = new ObjectMapper().readValue(jsonStr, ProposalVotingStats.class);
                        }
                    } catch (Exception e) {
                        votingStats = null;
                    }
                    return GovActionProposalStatus.builder()
                        .govActionTxHash(r.get(txHashField))
                        .govActionIndex(r.get(indexField))
                        .type(GovActionType.valueOf(r.get(typeField)))
                        .status(GovActionStatus.valueOf(r.get(statusField)))
                        .epoch(r.get(epochField))
                        .votingStats(votingStats)
                        .build();
                })
                .toList();
    }
}
