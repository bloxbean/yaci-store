package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovEpochActivityRepository;
import com.bloxbean.cardano.yaci.store.governanceaggr.util.DRepExpiryUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.mvel2.ast.Proto;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ObjectMapper objectMapper;

    @Transactional
    public void calculateAndUpdateExpiryForEpoch(int epoch) {
        log.info("Calculate and update DRep expiry, epoch {}", epoch);
        long t1 = System.currentTimeMillis();
        int leftBoundaryEpoch = epoch - 1;

        // in devnet, conway or later era can start from epoch 0
        Optional<CardanoEra> conwayEraOrLater = eraService.getEras()
                .stream().filter(cardanoEra -> cardanoEra.getEra().getValue() >= Era.Conway.getValue())
                .findFirst();

        int firstEpochNoInConwayOrLater = conwayEraOrLater.map(cardanoEra ->
                eraService.getEpochNo(cardanoEra.getEra(), cardanoEra.getStartSlot())).orElse(0);

        Set<Tuple<String, DrepType>> targetDReps = findDRepHashAndTypeTuples(epoch);

        if (targetDReps.isEmpty()) {
            return;
        }

        int batchSize = 200;

        List<Set<Tuple<String, DrepType>>> targetDRepsBatches = partitionSet(targetDReps, batchSize);

        Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepRegistrationInfo> registrationMap = new ConcurrentHashMap<>();
        Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepInteractionInfo> lastInteractionMap = new ConcurrentHashMap<>();

        targetDRepsBatches.parallelStream().forEach(batch -> {
            Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepRegistrationInfo> batchRegistrationMap =
                    findRegistrationInfos(batch, leftBoundaryEpoch);
            Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepInteractionInfo> batchInteractionMap =
                    findLastInteractions(batch, leftBoundaryEpoch);

            registrationMap.putAll(batchRegistrationMap);
            lastInteractionMap.putAll(batchInteractionMap);
        });

        Set<Integer> dormantEpochsToLeftBoundaryEpoch = govEpochActivityRepository.findDormantEpochsInEpochRange(firstEpochNoInConwayOrLater, leftBoundaryEpoch);

        Integer maxDRepRegistrationEpoch = registrationMap.values().stream().map(DRepExpiryUtil.DRepRegistrationInfo::epoch)
                .sorted(Integer::compareTo).toList().getLast();

        List<DRepExpiryUtil.ProposalSubmissionInfo> proposalSubmissionInfos = findProposalWithEpochLessThanOrEqualTo(maxDRepRegistrationEpoch);

        List<DRepExpiryUtil.ProposalSubmissionInfo> sortedProposals = proposalSubmissionInfos.stream()
                .sorted(Comparator.comparingLong(DRepExpiryUtil.ProposalSubmissionInfo::slot)
                        .reversed())
                .toList();

        DRepExpiryUtil.ProposalSubmissionInfo mostRecentProposal = proposalSubmissionInfos.stream()
                .max(Comparator.comparingLong(DRepExpiryUtil.ProposalSubmissionInfo::slot))
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

            // Find the latest proposal up to this DRep's registration
            DRepExpiryUtil.ProposalSubmissionInfo latestProposalUpToRegistration = null;
            if (dRepRegistration.protocolMajorVersion() == 9) {
                int regEpoch = dRepRegistration.epoch();
                long regSlot = dRepRegistration.slot();

                // Find the latest proposal that was submitted up to this DRep's registration
                latestProposalUpToRegistration = sortedProposals.stream()
                        .filter(p -> p.epoch() < regEpoch ||
                                (p.epoch() == regEpoch && p.slot() <= regSlot))
                        .findFirst()
                        .orElse(null);
            }

            int expiry = DRepExpiryUtil.calculateDRepExpiry(
                    dRepRegistration,
                    dRepLastInteraction,
                    dormantEpochsToLeftBoundaryEpoch,
                    latestProposalUpToRegistration,
                    firstEpochNoInConwayOrLater,
                    leftBoundaryEpoch
            );

            /* Calculate active_until value, now keep it for testing, should drop active_until column later */
            boolean isLeftBoundaryEpochDormant = dormantEpochsToLeftBoundaryEpoch.contains(leftBoundaryEpoch);
            int dormantEpochCount = recentGovEpochActivityOpt.get().getDormantEpochCount();
            boolean leftBoundaryEpochHadNewProposal = false;

            // check if the left boundary epoch had a proposal
            if (isLeftBoundaryEpochDormant && mostRecentProposal != null) {
                int mostRecentProposalEpoch = mostRecentProposal.epoch();
                if (mostRecentProposalEpoch == leftBoundaryEpoch) {
                    leftBoundaryEpochHadNewProposal = true;
                }
            }

            int activeUntil = expiry;

            /* if the left boundary epoch is dormant and there was no new proposal (dormant period is ongoing), drep is not inactive,
             we should set activeUntil to expiry - dormantEpochCount <=> do not change the expiry
             the active_until value is only updated after the dormant period ends. */
            if (isLeftBoundaryEpochDormant && !leftBoundaryEpochHadNewProposal && expiry >= leftBoundaryEpoch) { // TODO: expiry >= leftBoundaryEpoch or expiry > leftBoundaryEpoch?
                activeUntil = expiry - dormantEpochCount;
            }

            // continue adjusting the active_until value, if the drep was registered or last interacted in a dormant period
            if (dRepLastInteraction == null) {
                int dRepRegistrationEpoch = dRepRegistration.epoch();
                // check left boundary epoch is in a dormant period and drep was registered in this dormant period
                if (isEpochRangeDormant(dRepRegistrationEpoch, leftBoundaryEpoch, dormantEpochsToLeftBoundaryEpoch)
                        && !leftBoundaryEpochHadNewProposal) {
                    activeUntil = dRepRegistrationEpoch + dRepRegistration.dRepActivity();
                }
            } else {
                // case: drep is updated in dormant period
                if (isEpochRangeDormant(dRepLastInteraction.epoch(), leftBoundaryEpoch, dormantEpochsToLeftBoundaryEpoch)
                        && !leftBoundaryEpochHadNewProposal) {
                    activeUntil = dRepLastInteraction.epoch() + dRepLastInteraction.dRepActivity();
                }
            }

            /* active_until calculation ends */

            batch.add(new MapSqlParameterSource()
                    .addValue("drep_hash", dRep._1)
                    .addValue("drep_type", dRep._2.name())
                    .addValue("active_until", activeUntil)
                    .addValue("expiry", expiry)
                    .addValue("epoch", epoch));
        }

        String updateSql = "UPDATE drep_dist SET active_until = :active_until, expiry = :expiry " +
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

        Field<String> drepActivityField = null;
        Field<String> protocolMajorVerField = null;

        SQLDialect dialect = dsl.dialect();

        if (dialect.family() == SQLDialect.POSTGRES) {
            drepActivityField = DSL.field("params->>'drep_activity'", String.class);
            protocolMajorVerField = DSL.field("params->>'protocol_major_ver'", String.class);
        } else if (dialect.family() == SQLDialect.MYSQL) {
            drepActivityField = DSL.function("JSON_EXTRACT", SQLDataType.VARCHAR, DSL.field("params"), DSL.inline("$.drep_activity"));
            protocolMajorVerField = DSL.function("JSON_EXTRACT", SQLDataType.VARCHAR, DSL.field("params"), DSL.inline("$.protocol_major_ver"));
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
                                .orderBy(DREP_REGISTRATION.SLOT.desc())
                                .as("rn")
                )
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.TYPE.eq(CertificateType.REG_DREP_CERT.name())
                        .and(DSL.or(conditions))
                        .and(DREP_REGISTRATION.EPOCH.le(epoch)))
                .asTable("ranked");

        var selectFields = dsl.select(ranked.field(DREP_REGISTRATION.DREP_HASH),
                ranked.field(DREP_REGISTRATION.CRED_TYPE),
                ranked.field(DREP_REGISTRATION.SLOT),
                ranked.field(DREP_REGISTRATION.EPOCH));

        if (dialect.family() == SQLDialect.POSTGRES || dialect.family() == SQLDialect.MYSQL) {
            selectFields.select(
                    drepActivityField.cast(Integer.class).as("drep_activity"),
                    protocolMajorVerField.cast(Integer.class).as("protocol_major_ver"));
        } else {
            selectFields.select(EPOCH_PARAM.PARAMS);
        }

        var result = selectFields
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
            int drepActivity;
            int protocolMajorVer;

            if (dialect.family() == SQLDialect.H2) {
                var paramsJson = record.get(EPOCH_PARAM.PARAMS).data();
                var protocolParam = objectMapper.convertValue(paramsJson, ProtocolParams.class);
                drepActivity = protocolParam.getDrepActivity();
                protocolMajorVer = protocolParam.getProtocolMajorVer();
            } else {
                drepActivity = record.get("drep_activity", Integer.class);
                protocolMajorVer = record.get("protocol_major_ver", Integer.class);
            }

            map.put(new Tuple<>(hash, type), new DRepExpiryUtil.DRepRegistrationInfo(registrationSlot, registrationEpoch, drepActivity, protocolMajorVer));
        }

        return map;
    }

    private Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepInteractionInfo> findLastInteractions(Set<Tuple<String, DrepType>> drepHashAndTypes, int epoch) {
        if (drepHashAndTypes.isEmpty()) {
            return Collections.emptyMap();
        }

        Field<String> drepActivityField;
        SQLDialect dialect = dsl.dialect();

        if (dialect.family() == SQLDialect.POSTGRES) {
            drepActivityField = DSL.field("params->>'drep_activity'", String.class);
        } else if (dialect.family() == SQLDialect.MYSQL) {
            drepActivityField = DSL.function("JSON_EXTRACT", SQLDataType.VARCHAR, DSL.field("params"), DSL.inline("$.drep_activity"));
        } else {
            drepActivityField = DSL.field("JSON_VALUE(params, '$.drep_activity')", String.class);
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
                        drepActivityField.cast(Integer.class).as("drep_activity")
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
        Field<String> govActionLifetimeField;
        SQLDialect dialect = dsl.dialect();

        if (dialect.family() == SQLDialect.POSTGRES) {
            govActionLifetimeField = DSL.field("params->>'gov_action_lifetime'", String.class);
        } else if (dialect.family() == SQLDialect.MYSQL) {
            govActionLifetimeField = DSL.function("JSON_EXTRACT", SQLDataType.VARCHAR, DSL.field("params"), DSL.inline("$.gov_action_lifetime"));
        } else {
            govActionLifetimeField = DSL.field("JSON_VALUE(params, '$.gov_action_lifetime')", String.class);
        }

        return dsl.select(
                        GOV_ACTION_PROPOSAL.SLOT,
                        GOV_ACTION_PROPOSAL.EPOCH,
                        govActionLifetimeField.cast(Integer.class).as("gov_action_lifetime")
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

    private List<Set<Tuple<String, DrepType>>> partitionSet(Set<Tuple<String, DrepType>> drepSet, int batchSize) {
        List<Set<Tuple<String, DrepType>>> batches = new ArrayList<>();
        List<Tuple<String, DrepType>> list = new ArrayList<>(drepSet);

        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            Set<Tuple<String, DrepType>> batch = new HashSet<>(list.subList(i, end));
            batches.add(batch);
        }

        return batches;
    }
}
