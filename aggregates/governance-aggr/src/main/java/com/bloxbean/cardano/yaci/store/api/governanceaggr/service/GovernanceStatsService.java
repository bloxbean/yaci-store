package com.bloxbean.cardano.yaci.store.api.governanceaggr.service;

import com.bloxbean.cardano.yaci.store.api.governanceaggr.dto.GovernanceStatsDto;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.DREP_DIST;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.GOV_ACTION_PROPOSAL_STATUS;

@Service
@RequiredArgsConstructor
public class GovernanceStatsService {
    private final DSLContext dsl;
    private final BlockStorage blockStorage;

    public Optional<GovernanceStatsDto> getGovernanceStats(Integer epoch) {
        Integer targetEpoch = resolveEpoch(epoch);
        if (targetEpoch == null) {
            return Optional.empty();
        }

        return Optional.of(GovernanceStatsDto.builder()
                .epoch(targetEpoch)
                .drepStats(getDRepStats(targetEpoch))
                .proposalStats(getProposalStats(targetEpoch))
                .committeeStats(getCommitteeStats(targetEpoch))
                .voteStats(getVoteStats(targetEpoch))
                .build());
    }

    private Integer resolveEpoch(Integer epoch) {
        if (epoch != null) {
            return epoch;
        }
        return blockStorage.findRecentBlock().map(Block::getEpochNumber).orElse(null);
    }

    private GovernanceStatsDto.DRepStatsDto getDRepStats(int epoch) {
        // Find the latest drep_dist epoch <= target epoch
        Integer maxDistEpoch = dsl
                .select(DSL.max(DREP_DIST.EPOCH))
                .from(DREP_DIST)
                .where(DREP_DIST.EPOCH.le(epoch))
                .fetchOneInto(Integer.class);

        if (maxDistEpoch == null) {
            // Fallback: count from drep table without dist data
            return getDRepStatsFromDRepTable(epoch);
        }

        // Get latest status per DRep from drep table
        var drepLatest = dsl.select(
                        DREP.DREP_ID,
                        DREP.DREP_HASH,
                        DREP.STATUS,
                        DSL.rowNumber().over()
                                .partitionBy(DREP.DREP_ID)
                                .orderBy(DREP.SLOT.desc(), DREP.TX_INDEX.desc(), DREP.CERT_INDEX.desc())
                                .as("rn")
                )
                .from(DREP)
                .where(DREP.EPOCH.le(epoch))
                .asTable("drep_latest");

        var latestDreps = dsl.select(
                        drepLatest.field("drep_id", String.class),
                        drepLatest.field("drep_hash", String.class),
                        drepLatest.field("status", String.class)
                )
                .from(drepLatest)
                .where(DSL.field("rn", Integer.class).eq(1))
                .asTable("latest");

        // Join with drep_dist for voting power and active_until
        var joined = dsl.select(
                        latestDreps.field("status", String.class).as("drep_status"),
                        DREP_DIST.ACTIVE_UNTIL,
                        DREP_DIST.AMOUNT
                )
                .from(latestDreps)
                .leftJoin(DREP_DIST).on(
                        DREP_DIST.DREP_HASH.eq(latestDreps.field("drep_hash", String.class))
                                .and(DREP_DIST.DREP_ID.eq(latestDreps.field("drep_id", String.class)))
                                .and(DREP_DIST.EPOCH.eq(maxDistEpoch))
                )
                .fetch();

        int total = 0, active = 0, inactive = 0, retired = 0;
        BigInteger totalVotingPower = BigInteger.ZERO;

        for (var r : joined) {
            total++;
            String status = r.get("drep_status", String.class);
            Integer activeUntil = r.get(DREP_DIST.ACTIVE_UNTIL);
            BigInteger amount = r.get(DREP_DIST.AMOUNT) != null
                    ? BigInteger.valueOf(r.get(DREP_DIST.AMOUNT))
                    : BigInteger.ZERO;

            if ("RETIRED".equalsIgnoreCase(status)) {
                retired++;
            } else if (activeUntil != null && activeUntil < epoch) {
                inactive++;
            } else {
                active++;
                totalVotingPower = totalVotingPower.add(amount);
            }
        }

        return GovernanceStatsDto.DRepStatsDto.builder()
                .totalDreps(total)
                .activeDreps(active)
                .inactiveDreps(inactive)
                .retiredDreps(retired)
                .totalVotingPower(totalVotingPower)
                .build();
    }

    private GovernanceStatsDto.DRepStatsDto getDRepStatsFromDRepTable(int epoch) {
        var drepLatest = dsl.select(
                        DREP.DREP_ID,
                        DREP.STATUS,
                        DSL.rowNumber().over()
                                .partitionBy(DREP.DREP_ID)
                                .orderBy(DREP.SLOT.desc(), DREP.TX_INDEX.desc(), DREP.CERT_INDEX.desc())
                                .as("rn")
                )
                .from(DREP)
                .where(DREP.EPOCH.le(epoch))
                .asTable("drep_latest");

        var result = dsl.select(
                        DSL.count().as("total"),
                        DSL.count().filterWhere(DSL.field("status", String.class).ne("RETIRED")).as("non_retired"),
                        DSL.count().filterWhere(DSL.field("status", String.class).eq("RETIRED")).as("retired")
                )
                .from(drepLatest)
                .where(DSL.field("rn", Integer.class).eq(1))
                .fetchOne();

        int total = result != null ? result.get("total", Integer.class) : 0;
        int nonRetired = result != null ? result.get("non_retired", Integer.class) : 0;
        int retiredCount = result != null ? result.get("retired", Integer.class) : 0;

        return GovernanceStatsDto.DRepStatsDto.builder()
                .totalDreps(total)
                .activeDreps(nonRetired)
                .inactiveDreps(0)
                .retiredDreps(retiredCount)
                .totalVotingPower(BigInteger.ZERO)
                .build();
    }

    private GovernanceStatsDto.ProposalStatsDto getProposalStats(int epoch) {
        var g = GOV_ACTION_PROPOSAL_STATUS;

        var result = dsl.select(
                        g.STATUS,
                        DSL.count().as("cnt")
                )
                .from(g)
                .where(g.EPOCH.eq(epoch))
                .groupBy(g.STATUS)
                .fetch();

        int active = 0, ratified = 0, expired = 0;
        for (var r : result) {
            String status = r.get(g.STATUS);
            int count = r.get("cnt", Integer.class);
            switch (status) {
                case "ACTIVE" -> active = count;
                case "RATIFIED" -> ratified = count;
                case "EXPIRED" -> expired = count;
            }
        }

        // Count proposals submitted at this epoch that don't have a status entry yet
        var p = GOV_ACTION_PROPOSAL;
        Integer newlySubmitted = dsl.selectCount()
                .from(p)
                .where(p.EPOCH.eq(epoch))
                .fetchOneInto(Integer.class);

        if (newlySubmitted != null) {
            active += newlySubmitted;
        }

        return GovernanceStatsDto.ProposalStatsDto.builder()
                .activeProposals(active)
                .ratifiedProposals(ratified)
                .expiredProposals(expired)
                .build();
    }

    private GovernanceStatsDto.CommitteeStatsDto getCommitteeStats(int epoch) {
        var c = COMMITTEE_MEMBER;
        var members = dsl.selectFrom(c)
                .where(c.EPOCH.eq(
                        dsl.select(DSL.max(c.EPOCH))
                                .from(c)
                                .where(c.EPOCH.le(epoch))
                ))
                .fetch();

        int total = members.size();
        int activeCount = 0;
        int expiredCount = 0;

        for (var m : members) {
            Integer expiredEpoch = m.get(c.EXPIRED_EPOCH);
            if (expiredEpoch != null && expiredEpoch <= epoch) {
                expiredCount++;
            } else {
                activeCount++;
            }
        }

        return GovernanceStatsDto.CommitteeStatsDto.builder()
                .totalMembers(total)
                .activeMembers(activeCount)
                .expiredMembers(expiredCount)
                .build();
    }

    private GovernanceStatsDto.VoteStatsDto getVoteStats(int epoch) {
        var v = VOTING_PROCEDURE;

        var result = dsl.select(
                        v.VOTER_TYPE,
                        DSL.count().as("cnt")
                )
                .from(v)
                .where(v.EPOCH.eq(epoch))
                .groupBy(v.VOTER_TYPE)
                .fetch();

        int drepVotes = 0, spoVotes = 0, committeeVotes = 0;
        for (var r : result) {
            String voterType = r.get(v.VOTER_TYPE);
            int count = r.get("cnt", Integer.class);
            switch (voterType) {
                case "DREP_KEY_HASH", "DREP_SCRIPT_HASH" -> drepVotes += count;
                case "CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH",
                     "CONSTITUTIONAL_COMMITTEE_HOT_SCRIPT_HASH" -> committeeVotes += count;
                case "STAKING_POOL_KEY_HASH" -> spoVotes += count;
            }
        }

        return GovernanceStatsDto.VoteStatsDto.builder()
                .totalVotesCurrentEpoch(drepVotes + spoVotes + committeeVotes)
                .drepVotes(drepVotes)
                .spoVotes(spoVotes)
                .committeeVotes(committeeVotes)
                .build();
    }
}
