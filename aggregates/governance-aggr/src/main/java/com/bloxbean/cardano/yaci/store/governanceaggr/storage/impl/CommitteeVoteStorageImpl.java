package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl;

import com.bloxbean.cardano.yaci.store.governanceaggr.domain.CommitteeVote;
import com.bloxbean.cardano.yaci.store.governanceaggr.domain.GovActionId;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.CommitteeVoteStorage;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.mapper.CommitteeVoteMapper;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model.CommitteeVoteEntity;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.CommitteeVoteRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.data.util.Pair;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.COMMITTEE_VOTE;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.select;

@RequiredArgsConstructor
public class CommitteeVoteStorageImpl implements CommitteeVoteStorage {
    private final CommitteeVoteRepository committeeVoteRepository;
    private final CommitteeVoteMapper committeeVoteMapper;
    private final DSLContext dsl;

    @Override
    public void saveAll(List<CommitteeVote> committeeVotes) {
        committeeVoteRepository.saveAll(committeeVotes.stream().map(committeeVoteMapper::toCommitteeVotesEntity)
                .collect(Collectors.toList()));
    }

    @Override
    public int deleteBySlotGreaterThan(long slot) {
        return committeeVoteRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public List<CommitteeVote> findByGovActionIdsWithMaxSlot(Collection<GovActionId> govActionIds) {
        var cv = COMMITTEE_VOTE.as("cv");
        var c = COMMITTEE_VOTE.as("c");

        var maxCV = select(c.GOV_ACTION_TX_HASH, c.GOV_ACTION_INDEX, max(c.SLOT).as("maxSlot"))
                .from(c)
                .groupBy(c.GOV_ACTION_TX_HASH, c.GOV_ACTION_INDEX)
                .asTable("maxCV");
        Condition condition = null;

        for (var govActionId : govActionIds) {
            String govActionTxHash = govActionId.getGovActionTxHash();
            Integer govActionIndex = govActionId.getGovActionIndex();
            Condition pairCondition = (cv.GOV_ACTION_TX_HASH.eq(govActionTxHash))
                    .and(cv.GOV_ACTION_INDEX.eq(govActionIndex));
            condition = condition == null ? pairCondition : condition.or(pairCondition);
        }

        return dsl.select(cv.fields())
                .from(cv.join(maxCV)
                        .on(cv.GOV_ACTION_TX_HASH.eq(maxCV.field(c.GOV_ACTION_TX_HASH.getName(), String.class)))
                        .and(cv.GOV_ACTION_INDEX.eq(maxCV.field(c.GOV_ACTION_INDEX.getName(), Integer.class)))
                        .and(cv.SLOT.eq(maxCV.field("maxSlot", Long.class))))
                .where(condition)
                .fetchInto(CommitteeVoteEntity.class).stream().map(committeeVoteMapper::toCommitteeVotes).collect(Collectors.toList());
    }

    @Override
    public List<CommitteeVote> findByGovActionIdAndVoterHashWithMaxSlot(Collection<Pair<GovActionId, String>> govActionIdVoterHashPairs) {
        var cv = COMMITTEE_VOTE.as("cv");
        var c = COMMITTEE_VOTE.as("c");

        var maxCV = select(c.GOV_ACTION_TX_HASH, c.GOV_ACTION_INDEX, c.VOTER_HASH, max(c.SLOT).as("maxSlot"))
                .from(c)
                .groupBy(c.GOV_ACTION_TX_HASH, c.GOV_ACTION_INDEX, c.VOTER_HASH)
                .asTable("maxCV");
        Condition condition = null;

        for (var element : govActionIdVoterHashPairs) {
            String govActionTxHash = element.getFirst().getGovActionTxHash();
            Integer govActionIndex = element.getFirst().getGovActionIndex();
            String voterHash = element.getSecond();
            Condition whereCondition = (cv.GOV_ACTION_TX_HASH.eq(govActionTxHash))
                    .and(cv.GOV_ACTION_INDEX.eq(govActionIndex))
                    .and(cv.VOTER_HASH.eq(voterHash));
            condition = condition == null ? whereCondition : condition.or(whereCondition);
        }

        return dsl.select(cv.fields())
                .from(cv.join(maxCV)
                        .on(cv.GOV_ACTION_TX_HASH.eq(maxCV.field(c.GOV_ACTION_TX_HASH.getName(), String.class)))
                        .and(cv.GOV_ACTION_INDEX.eq(maxCV.field(c.GOV_ACTION_INDEX.getName(), Integer.class)))
                        .and(cv.VOTER_HASH.eq(maxCV.field(c.VOTER_HASH.getName(), String.class)))
                        .and(cv.SLOT.eq(maxCV.field("maxSlot", Long.class))))
                .where(condition)
                .fetchInto(CommitteeVoteEntity.class).stream().map(committeeVoteMapper::toCommitteeVotes).collect(Collectors.toList());
    }
}
