package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.LatestVotingProcedure;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.LatestVotingProcedureStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.LatestVotingProcedureMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.LatestVotingProcedureRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.LATEST_VOTING_PROCEDURE;
import static org.jooq.impl.DSL.excluded;

@RequiredArgsConstructor
public class LatestVotingProcedureStorageImpl implements LatestVotingProcedureStorage {
    private final LatestVotingProcedureRepository latestVotingProcedureRepository;
    private final LatestVotingProcedureMapper latestVotingProcedureMapper;
    private final DSLContext dsl;

    @Override
    public void saveAll(List<LatestVotingProcedure> latestVotingProcedure) {
        latestVotingProcedureRepository.saveAll(latestVotingProcedure.stream()
                .map(latestVotingProcedureMapper::toLatestVotingProcedureEntity).collect(Collectors.toList()));
    }

    @Override
    public void saveOrUpdate(Collection<LatestVotingProcedure> latestVotingProcedures) {
        // insert on conflict do update
        var inserts = latestVotingProcedures.stream().map(latestVotingProcedure ->
                dsl.insertInto(LATEST_VOTING_PROCEDURE)
                        .set(LATEST_VOTING_PROCEDURE.ID, latestVotingProcedure.getId())
                        .set(LATEST_VOTING_PROCEDURE.TX_HASH, latestVotingProcedure.getTxHash())
                        .set(LATEST_VOTING_PROCEDURE.IDX, latestVotingProcedure.getIndex())
                        .set(LATEST_VOTING_PROCEDURE.SLOT, latestVotingProcedure.getSlot())
                        .set(LATEST_VOTING_PROCEDURE.VOTER_TYPE, latestVotingProcedure.getVoterType().name())
                        .set(LATEST_VOTING_PROCEDURE.VOTER_HASH, latestVotingProcedure.getVoterHash())
                        .set(LATEST_VOTING_PROCEDURE.GOV_ACTION_TX_HASH, latestVotingProcedure.getGovActionTxHash())
                        .set(LATEST_VOTING_PROCEDURE.GOV_ACTION_INDEX, latestVotingProcedure.getGovActionIndex())
                        .set(LATEST_VOTING_PROCEDURE.VOTE, latestVotingProcedure.getVote().name())
                        .set(LATEST_VOTING_PROCEDURE.ANCHOR_URL, latestVotingProcedure.getAnchorUrl())
                        .set(LATEST_VOTING_PROCEDURE.ANCHOR_HASH, latestVotingProcedure.getAnchorHash())
                        .set(LATEST_VOTING_PROCEDURE.EPOCH, latestVotingProcedure.getEpoch())
                        .set(LATEST_VOTING_PROCEDURE.REPEAT_VOTE, latestVotingProcedure.getRepeatVote())
                        .onConflict(LATEST_VOTING_PROCEDURE.VOTER_HASH,
                                LATEST_VOTING_PROCEDURE.GOV_ACTION_TX_HASH,
                                LATEST_VOTING_PROCEDURE.GOV_ACTION_INDEX)
                        .doUpdate()
                        .set(LATEST_VOTING_PROCEDURE.IDX, excluded(LATEST_VOTING_PROCEDURE.IDX))
                        .set(LATEST_VOTING_PROCEDURE.TX_HASH, excluded(LATEST_VOTING_PROCEDURE.TX_HASH))
                        .set(LATEST_VOTING_PROCEDURE.VOTER_TYPE, excluded(LATEST_VOTING_PROCEDURE.VOTER_TYPE))
                        .set(LATEST_VOTING_PROCEDURE.SLOT, excluded(LATEST_VOTING_PROCEDURE.SLOT))
                        .set(LATEST_VOTING_PROCEDURE.VOTE_IN_PREV_AGGR_SLOT, LATEST_VOTING_PROCEDURE.VOTE)
                        .set(LATEST_VOTING_PROCEDURE.VOTE, excluded(LATEST_VOTING_PROCEDURE.VOTE))
                        .set(LATEST_VOTING_PROCEDURE.ANCHOR_URL, excluded(LATEST_VOTING_PROCEDURE.ANCHOR_URL))
                        .set(LATEST_VOTING_PROCEDURE.ANCHOR_HASH, excluded(LATEST_VOTING_PROCEDURE.ANCHOR_HASH))
                        .set(LATEST_VOTING_PROCEDURE.EPOCH, excluded(LATEST_VOTING_PROCEDURE.EPOCH))
                        .set(LATEST_VOTING_PROCEDURE.REPEAT_VOTE, Boolean.TRUE))
                .collect(Collectors.toList());

        dsl.batch(inserts).execute();
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return latestVotingProcedureRepository.deleteBySlotGreaterThan(slot);
    }
}
