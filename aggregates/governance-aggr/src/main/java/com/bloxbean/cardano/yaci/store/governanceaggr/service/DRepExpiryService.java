package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovEpochActivityRepository;
import com.bloxbean.cardano.yaci.store.governanceaggr.util.DRepExpiryUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.epoch.jooq.Tables.EPOCH_PARAM;
import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.DREP_DIST;
import static com.bloxbean.cardano.yaci.store.governanceaggr.util.DRepExpiryUtil.isEpochRangeDormant;

@Service
@RequiredArgsConstructor
@Slf4j
public class DRepExpiryService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EraService eraService;
    private final GovEpochActivityRepository govEpochActivityRepository;
    private final GovEpochActivityService govEpochActivityService;
    private final DSLContext dsl;

    @Transactional
    public void calculateAndUpdateExpiryForEpoch(int epoch) {
        log.info("Calculate and update DRep expiry, epoch {}", epoch);
        long t1 = System.currentTimeMillis();
        int leftBoundaryEpoch = epoch - 1;

        Optional<CardanoEra> conwayEra = eraService.getEras()
                .stream().filter(cardanoEra -> cardanoEra.getEra().equals(Era.Conway))
                .findFirst();

        int firstEpochNoInConway = conwayEra.map(cardanoEra ->
                eraService.getEpochNo(cardanoEra.getEra(), cardanoEra.getStartSlot())).orElse(0);

        Set<Tuple<String, DrepType>> targetDReps = findDRepHashAndTypeTuples(epoch);

        if (targetDReps.isEmpty()) {
            return;
        }

        Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepRegistrationInfo> registrationMap = findRegistrationInfos(targetDReps, leftBoundaryEpoch);
        Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepInteractionInfo> lastInteractionMap = findLastInteractions(targetDReps, leftBoundaryEpoch);
        Set<Integer> dormantEpochsUntilLeftBoundaryEpoch = govEpochActivityRepository.findDormantEpochsByEpochBetween(firstEpochNoInConway, leftBoundaryEpoch);

        Integer maxDRepRegistrationEpoch = registrationMap.values().stream().map(DRepExpiryUtil.DRepRegistrationInfo::epoch)
                .sorted(Integer::compareTo).toList().getLast();

        List<DRepExpiryUtil.ProposalSubmissionInfo> proposalSubmissionInfos = findProposalWithEpochLessThanOrEqualTo(maxDRepRegistrationEpoch);

        DRepExpiryUtil.ProposalSubmissionInfo mostRecentProposal = proposalSubmissionInfos.stream()
                .max(Comparator.comparingInt(DRepExpiryUtil.ProposalSubmissionInfo::epoch)
                        .thenComparingLong(DRepExpiryUtil.ProposalSubmissionInfo::slot))
                .orElse(null);

        List<MapSqlParameterSource> batch = new ArrayList<>();

        var recentGovEpochActivityOpt = govEpochActivityService.getGovEpochActivity(leftBoundaryEpoch);

        if (recentGovEpochActivityOpt.isEmpty()) {
            log.error("GovEpochActivityEntity not found for epoch {}", leftBoundaryEpoch);
            return;
        }

        for (Tuple<String, DrepType> dRep : targetDReps) {
            var dRepRegistration = registrationMap.get(dRep);

            if (dRepRegistration == null) {
                log.error("DRep registration info not found for hash {}", dRep);
                continue;
            }

            var dRepLastInteraction = lastInteractionMap.get(dRep);

            List<DRepExpiryUtil.ProposalSubmissionInfo> proposalsUpToRegistration =
                    proposalSubmissionInfos.stream()
                            .filter(p -> p.epoch() <= dRepRegistration.epoch())
                            .toList();

            int expiry = DRepExpiryUtil.calculateDRepExpiry(
                    dRepRegistration,
                    dRepLastInteraction,
                    dormantEpochsUntilLeftBoundaryEpoch,
                    proposalsUpToRegistration,
                    firstEpochNoInConway,
                    leftBoundaryEpoch
            );

            boolean isLeftBoundaryEpochDormant = dormantEpochsUntilLeftBoundaryEpoch.contains(leftBoundaryEpoch);
            int dormantEpochCount = recentGovEpochActivityOpt.get().getDormantEpochCount();
            boolean leftBoundaryEpochHadNewProposal = false;

            // check if the left boundary epoch had a proposal
            if (isLeftBoundaryEpochDormant && mostRecentProposal != null) {
                int mostRecentProposalEpoch = mostRecentProposal.epoch();
                if (mostRecentProposalEpoch == leftBoundaryEpoch) {
                    leftBoundaryEpochHadNewProposal = true;
                }
            }

            int activeUntil = (isLeftBoundaryEpochDormant && !leftBoundaryEpochHadNewProposal) ? expiry - dormantEpochCount : expiry;

            if (dRepLastInteraction == null) {
                int dRepRegistrationEpoch = dRepRegistration.epoch();
                // check left boundary epoch is in a dormant period and drep was registered in this dormant period
                if (isEpochRangeDormant(dRepRegistrationEpoch, leftBoundaryEpoch, dormantEpochsUntilLeftBoundaryEpoch)
                        && !leftBoundaryEpochHadNewProposal) {
                    activeUntil = dRepRegistrationEpoch + dRepRegistration.dRepActivity();
                }
            }

            batch.add(new MapSqlParameterSource()
                    .addValue("drep_hash", dRep._1)
                    .addValue("drep_type", dRep._2.name())
                    .addValue("active_until", activeUntil)
                    .addValue("epoch", epoch));
        }

        String updateSql = "UPDATE drep_dist SET active_until = :active_until " +
                "WHERE drep_hash = :drep_hash and drep_type = :drep_type AND epoch = :epoch";

        jdbcTemplate.batchUpdate(updateSql, batch.toArray(new MapSqlParameterSource[0]));

        long t2 = System.currentTimeMillis();
        log.info("Updated active_until for {} DReps at epoch {}", batch.size(), epoch);
        log.info("DRep expiry calculation for epoch {} and update took {} ms", epoch, (t2 - t1));
    }

    private Set<Tuple<String, DrepType>> findDRepHashAndTypeTuples(int epoch) {
        return dsl.selectDistinct(DREP_DIST.DREP_HASH, DREP_DIST.DREP_TYPE)
                .from(DREP_DIST)
                .where(DREP_DIST.EPOCH.eq(epoch)
                        .and(DREP_DIST.DREP_TYPE.notIn("ABSTAIN", "NO_CONFIDENCE")))
                .fetchSet(record -> new Tuple<>(
                        record.get(DREP_DIST.DREP_HASH),
                        DrepType.valueOf(record.get(DREP_DIST.DREP_TYPE))
                ));
    }

    private Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepRegistrationInfo> findRegistrationInfos(Set<Tuple<String, DrepType>> drepHashAndTypes, int epoch) {
        if (drepHashAndTypes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Condition> conditions = drepHashAndTypes
                .stream()
                .map(t -> DREP_REGISTRATION.DREP_HASH.eq(t._1).and(DREP_REGISTRATION.CRED_TYPE.eq(t._2.name())))
                .collect(Collectors.toList());

        var ranked = DSL
                .select(
                        DREP_REGISTRATION.DREP_HASH,
                        DREP_REGISTRATION.CRED_TYPE,
                        DREP_REGISTRATION.SLOT,
                        DREP_REGISTRATION.EPOCH,
                        DSL.rowNumber()
                                .over()
                                .partitionBy(DREP_REGISTRATION.DREP_HASH, DREP_REGISTRATION.CRED_TYPE)
                                .orderBy(DREP_REGISTRATION.EPOCH.desc(), DREP_REGISTRATION.SLOT.desc())
                                .as("rn")
                )
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.TYPE.eq(CertificateType.REG_DREP_CERT.name())
                        .and(DSL.or(conditions))
                        .and(DREP_REGISTRATION.EPOCH.le(epoch)))
                .asTable("ranked");

        var result = dsl.select(
                        ranked.field(DREP_REGISTRATION.DREP_HASH),
                        ranked.field(DREP_REGISTRATION.CRED_TYPE),
                        ranked.field(DREP_REGISTRATION.SLOT),
                        ranked.field(DREP_REGISTRATION.EPOCH),
                        DSL.field("params->>'drep_activity'", String.class).cast(Integer.class).as("drep_activity"),
                        DSL.field("params->>'protocol_major_ver'", String.class).cast(Integer.class).as("protocol_major_ver")
                )
                .from(ranked)
                .join(EPOCH_PARAM).on(ranked.field(DREP_REGISTRATION.EPOCH).eq(EPOCH_PARAM.EPOCH))
                .where(ranked.field("rn", Integer.class).eq(1))
                .fetch();

        Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepRegistrationInfo> map = new HashMap<>();
        for (var record : result) {
            String hash = record.get(DREP_REGISTRATION.DREP_HASH);
            DrepType type = DrepType.valueOf(record.get(DREP_REGISTRATION.CRED_TYPE));
            long registrationSlot = record.get(DREP_REGISTRATION.SLOT);
            int registrationEpoch = record.get(DREP_REGISTRATION.EPOCH);
            int drepActivity = record.get("drep_activity", Integer.class);
            int protocolMajorVer = record.get("protocol_major_ver", Integer.class);

            map.put(new Tuple<>(hash, type), new DRepExpiryUtil.DRepRegistrationInfo(registrationSlot, registrationEpoch, drepActivity, protocolMajorVer));
        }

        return map;
    }

    private Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepInteractionInfo> findLastInteractions(Set<Tuple<String, DrepType>> drepHashAndTypes, int epoch) {
        if (drepHashAndTypes.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Condition> dRepRegConditions = drepHashAndTypes
                .stream()
                .map(t -> DREP_REGISTRATION.DREP_HASH.eq(t._1).and(DREP_REGISTRATION.CRED_TYPE.eq(t._2.name())))
                .collect(Collectors.toList());

        // Subquery UNION ALL updates + votes
        var updates = dsl.select(DREP_REGISTRATION.DREP_HASH.as("drep_hash"), DREP_REGISTRATION.CRED_TYPE.as("drep_type"),
                        DREP_REGISTRATION.EPOCH.as("epoch"))
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.TYPE.eq(CertificateType.UPDATE_DREP_CERT.name())
                        .and(DREP_REGISTRATION.EPOCH.le(epoch))
                        .and(DSL.or(dRepRegConditions)));

        Set<Tuple<String, VoterType>> drepAndVoterTypes = drepHashAndTypes.stream()
                .map(t -> new Tuple<>(t._1, t._2 == DrepType.ADDR_KEYHASH ? VoterType.DREP_KEY_HASH : VoterType.DREP_SCRIPT_HASH))
                .collect(Collectors.toSet());

        List<Condition> voteConditions = drepAndVoterTypes
                .stream()
                .map(t -> VOTING_PROCEDURE.VOTER_HASH.eq(t._1).and(VOTING_PROCEDURE.VOTER_TYPE.eq(t._2.name())))
                .toList();

        var votes = dsl.select(VOTING_PROCEDURE.VOTER_HASH.as("drep_hash"),
                        DSL.decode()
                                .value(VOTING_PROCEDURE.VOTER_TYPE)
                                .when(VoterType.DREP_KEY_HASH.name(), DrepType.ADDR_KEYHASH.name())
                                .when(VoterType.DREP_SCRIPT_HASH.name(), DrepType.SCRIPTHASH.name())
                                .as("drep_type"),
                        VOTING_PROCEDURE.EPOCH.as("epoch"))
                .from(VOTING_PROCEDURE)
                .where(VOTING_PROCEDURE.EPOCH.le(epoch)).and(DSL.or(voteConditions));

        var interactionSubquery = updates.unionAll(votes).asTable("sub");

        var li = DSL
                .select(
                        interactionSubquery.field("drep_hash", String.class),
                        interactionSubquery.field("drep_type", String.class),
                        DSL.max(interactionSubquery.field("epoch", Integer.class)).as("epoch")
                )
                .from(interactionSubquery)
                .groupBy(interactionSubquery.field("drep_hash", String.class),
                        interactionSubquery.field("drep_type", String.class))
                .asTable("li");

        var result = dsl.select(
                        li.field("drep_hash", String.class),
                        li.field("drep_type", String.class),
                        li.field("epoch", Integer.class),
                        DSL.field("params->>'drep_activity'", String.class).cast(Integer.class).as("drep_activity")
                )
                .from(li)
                .join(EPOCH_PARAM).on(EPOCH_PARAM.EPOCH.eq(li.field("epoch", Integer.class)))
                .fetch();

        Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepInteractionInfo> map = new HashMap<>();
        for (var record : result) {
            String hash = record.get("drep_hash", String.class);
            int interactionEpoch = record.get("epoch", Integer.class);
            int drepActivity = record.get("drep_activity", Integer.class);
            DrepType type = DrepType.valueOf(record.get("drep_type", String.class));

            map.put(new Tuple<>(hash, type), new DRepExpiryUtil.DRepInteractionInfo(interactionEpoch, drepActivity));
        }

        return map;
    }

    private List<DRepExpiryUtil.ProposalSubmissionInfo> findProposalWithEpochLessThanOrEqualTo(int epoch) {
        return dsl.select(
                        GOV_ACTION_PROPOSAL.SLOT,
                        GOV_ACTION_PROPOSAL.EPOCH,
                        DSL.field("params->>'gov_action_lifetime'", String.class).cast(Integer.class).as("gov_action_lifetime")
                )
                .from(GOV_ACTION_PROPOSAL)
                .join(EPOCH_PARAM).on(GOV_ACTION_PROPOSAL.EPOCH.eq(EPOCH_PARAM.EPOCH))
                .where(GOV_ACTION_PROPOSAL.EPOCH.le(epoch))
                .fetch(record -> new DRepExpiryUtil.ProposalSubmissionInfo(
                        record.get(GOV_ACTION_PROPOSAL.SLOT),
                        record.get(GOV_ACTION_PROPOSAL.EPOCH),
                        record.get("gov_action_lifetime", Integer.class)
                ));
    }
}
