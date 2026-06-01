package com.bloxbean.cardano.yaci.store.governanceaggr.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import com.bloxbean.cardano.yaci.store.common.util.ListUtil;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.domain.CardanoEra;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.repository.GovEpochActivityRepository;
import com.bloxbean.cardano.yaci.store.governanceaggr.util.DRepExpiryUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.yaci.store.epoch.jooq.Tables.EPOCH_PARAM;
import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.DREP_DIST;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.GOV_ACTION_PROPOSAL_STATUS;

@Service
@RequiredArgsConstructor
@Slf4j
public class DRepExpiryService {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EraService eraService;
    private final GovEpochActivityRepository govEpochActivityRepository;
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

        List<Set<Tuple<String, DrepType>>> targetDRepsBatches = ListUtil.partitionSet(targetDReps, batchSize);

        Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepRegistrationInfo> registrationMap = new ConcurrentHashMap<>();
        Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepInteractionInfo> lastInteractionMap = new ConcurrentHashMap<>();
        Map<Tuple<String, DrepType>, List<DRepExpiryUtil.DRepInteractionInfo>> interactionEventsMap = new ConcurrentHashMap<>();

        targetDRepsBatches.parallelStream().forEach(batch -> {
            Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepRegistrationInfo> batchRegistrationMap =
                    findRegistrationInfos(batch, leftBoundaryEpoch);
            Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepInteractionInfo> batchInteractionMap =
                    findLastInteractions(batch, leftBoundaryEpoch);
            Map<Tuple<String, DrepType>, List<DRepExpiryUtil.DRepInteractionInfo>> batchInteractionEventsMap =
                    findInteractionInfos(batch, leftBoundaryEpoch);

            registrationMap.putAll(batchRegistrationMap);
            lastInteractionMap.putAll(batchInteractionMap);
            interactionEventsMap.putAll(batchInteractionEventsMap);
        });

        Set<Integer> dormantEpochsToLeftBoundaryEpoch = govEpochActivityRepository.findDormantEpochsInEpochRange(firstEpochNoInConwayOrLater, leftBoundaryEpoch);
        Set<Integer> nonDormantProposalEpochsToLeftBoundaryEpoch = findNonDormantProposalStatusEpochs(firstEpochNoInConwayOrLater, leftBoundaryEpoch);

        List<DRepExpiryUtil.ProposalSubmissionInfo> proposalSubmissionInfos = findProposalWithEpochLessThanOrEqualTo(leftBoundaryEpoch);

        List<DRepExpiryUtil.ProposalSubmissionInfo> sortedProposals = proposalSubmissionInfos.stream()
                .sorted(Comparator.comparingLong(DRepExpiryUtil.ProposalSubmissionInfo::slot)
                        .reversed())
                .toList();

        List<MapSqlParameterSource> batch = new ArrayList<>();

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

            int activeUntil = DRepExpiryUtil.calculateDRepActiveUntil(
                    dRepRegistration,
                    interactionEventsMap.getOrDefault(dRep, Collections.emptyList()),
                    proposalSubmissionInfos,
                    nonDormantProposalEpochsToLeftBoundaryEpoch,
                    firstEpochNoInConwayOrLater,
                    leftBoundaryEpoch
            );

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
                        DREP_REGISTRATION.TX_INDEX,
                        DREP_REGISTRATION.CERT_INDEX,
                        DSL.rowNumber()
                                .over()
                                .partitionBy(DREP_REGISTRATION.DREP_HASH, DREP_REGISTRATION.CRED_TYPE)
                                .orderBy(DREP_REGISTRATION.SLOT.desc(),
                                        DREP_REGISTRATION.TX_INDEX.desc(),
                                        DREP_REGISTRATION.CERT_INDEX.desc())
                                .as("rn")
                )
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.TYPE.eq(CertificateType.REG_DREP_CERT.name())
                        .and(DSL.or(conditions))
                        .and(DREP_REGISTRATION.EPOCH.le(epoch)))
                .asTable("ranked");

        Result<?> result;
        if (dialect.family() == SQLDialect.POSTGRES || dialect.family() == SQLDialect.MYSQL) {
            result = dsl.select(
                    ranked.field(DREP_REGISTRATION.DREP_HASH),
                    ranked.field(DREP_REGISTRATION.CRED_TYPE),
                    ranked.field(DREP_REGISTRATION.SLOT),
                    ranked.field(DREP_REGISTRATION.EPOCH),
                    ranked.field(DREP_REGISTRATION.TX_INDEX),
                    ranked.field(DREP_REGISTRATION.CERT_INDEX),
                    drepActivityField.cast(Integer.class).as("drep_activity"),
                    protocolMajorVerField.cast(Integer.class).as("protocol_major_ver")
            )
            .from(ranked)
            .join(EPOCH_PARAM).on(ranked.field(DREP_REGISTRATION.EPOCH).eq(EPOCH_PARAM.EPOCH))
            .where(ranked.field("rn", Integer.class).eq(1))
            .fetch();
        } else {
            result = dsl.select(
                    ranked.field(DREP_REGISTRATION.DREP_HASH),
                    ranked.field(DREP_REGISTRATION.CRED_TYPE),
                    ranked.field(DREP_REGISTRATION.SLOT),
                    ranked.field(DREP_REGISTRATION.EPOCH),
                    ranked.field(DREP_REGISTRATION.TX_INDEX),
                    ranked.field(DREP_REGISTRATION.CERT_INDEX),
                    EPOCH_PARAM.PARAMS
            )
            .from(ranked)
            .join(EPOCH_PARAM).on(ranked.field(DREP_REGISTRATION.EPOCH).eq(EPOCH_PARAM.EPOCH))
            .where(ranked.field("rn", Integer.class).eq(1))
            .fetch();
        }

        Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepRegistrationInfo> map = new HashMap<>();
        for (Record record : result) {
            String hash = record.get(DREP_REGISTRATION.DREP_HASH);
            DrepType type = DrepType.valueOf(record.get(DREP_REGISTRATION.CRED_TYPE));
            long registrationSlot = record.get(DREP_REGISTRATION.SLOT);
            int registrationEpoch = record.get(DREP_REGISTRATION.EPOCH);
            int txIndex = record.get(DREP_REGISTRATION.TX_INDEX);
            int certIndex = record.get(DREP_REGISTRATION.CERT_INDEX);
            int drepActivity = 0;
            int protocolMajorVer = 0;

            if (dialect.family() == SQLDialect.H2) {
                var paramsJson = record.get(EPOCH_PARAM.PARAMS).data();
                ProtocolParams protocolParam;
                try {
                    protocolParam = objectMapper.readValue(paramsJson, ProtocolParams.class);
                    drepActivity = protocolParam.getDrepActivity();
                    protocolMajorVer = protocolParam.getProtocolMajorVer();
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse protocol params JSON: {}", paramsJson, e);
                }
            } else {
                drepActivity = record.get("drep_activity", Integer.class);
                protocolMajorVer = record.get("protocol_major_ver", Integer.class);
            }

            map.put(new Tuple<>(hash, type), new DRepExpiryUtil.DRepRegistrationInfo(registrationSlot, registrationEpoch, drepActivity, protocolMajorVer, txIndex, certIndex));
        }

        return map;
    }

    private Map<Tuple<String, DrepType>, List<DRepExpiryUtil.DRepInteractionInfo>> findInteractionInfos(Set<Tuple<String, DrepType>> drepHashAndTypes, int epoch) {
        if (drepHashAndTypes.isEmpty()) {
            return Collections.emptyMap();
        }

        Field<String> drepActivityField = null;
        SQLDialect dialect = dsl.dialect();

        if (dialect.family() == SQLDialect.POSTGRES) {
            drepActivityField = DSL.field("params->>'drep_activity'", String.class);
        } else if (dialect.family() == SQLDialect.MYSQL) {
            drepActivityField = DSL.function("JSON_EXTRACT", SQLDataType.VARCHAR, DSL.field("params"), DSL.inline("$.drep_activity"));
        }

        List<Condition> dRepRegConditions = drepHashAndTypes
                .stream()
                .map(t -> DREP_REGISTRATION.DREP_HASH.eq(t._1).and(DREP_REGISTRATION.CRED_TYPE.eq(t._2.name())))
                .collect(Collectors.toList());

        var updates = dsl.select(DREP_REGISTRATION.DREP_HASH.as("drep_hash"), DREP_REGISTRATION.CRED_TYPE.as("drep_type"),
                        DREP_REGISTRATION.EPOCH.as("epoch"), DREP_REGISTRATION.SLOT.as("slot"),
                        DREP_REGISTRATION.TX_INDEX.as("tx_index"), DREP_REGISTRATION.CERT_INDEX.as("event_index"))
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
                        VOTING_PROCEDURE.EPOCH.as("epoch"), VOTING_PROCEDURE.SLOT.as("slot"),
                        VOTING_PROCEDURE.TX_INDEX.as("tx_index"), VOTING_PROCEDURE.IDX.as("event_index"))
                .from(VOTING_PROCEDURE)
                .where(VOTING_PROCEDURE.EPOCH.le(epoch)).and(DSL.or(voteConditions));

        var interactionSubquery = updates.unionAll(votes).asTable("sub");

        Result<?> result;
        if (dialect.family() == SQLDialect.POSTGRES || dialect.family() == SQLDialect.MYSQL) {
            result = dsl.select(
                    interactionSubquery.field("drep_hash", String.class),
                    interactionSubquery.field("drep_type", String.class),
                    interactionSubquery.field("epoch", Integer.class),
                    interactionSubquery.field("slot", Long.class),
                    interactionSubquery.field("tx_index", Integer.class),
                    interactionSubquery.field("event_index", Integer.class),
                    drepActivityField.cast(Integer.class).as("drep_activity")
            )
            .from(interactionSubquery)
            .join(EPOCH_PARAM).on(EPOCH_PARAM.EPOCH.eq(interactionSubquery.field("epoch", Integer.class)))
            .fetch();
        } else {
            result = dsl.select(
                    interactionSubquery.field("drep_hash", String.class),
                    interactionSubquery.field("drep_type", String.class),
                    interactionSubquery.field("epoch", Integer.class),
                    interactionSubquery.field("slot", Long.class),
                    interactionSubquery.field("tx_index", Integer.class),
                    interactionSubquery.field("event_index", Integer.class),
                    EPOCH_PARAM.PARAMS
            )
            .from(interactionSubquery)
            .join(EPOCH_PARAM).on(EPOCH_PARAM.EPOCH.eq(interactionSubquery.field("epoch", Integer.class)))
            .fetch();
        }

        Map<Tuple<String, DrepType>, List<DRepExpiryUtil.DRepInteractionInfo>> map = new HashMap<>();
        for (Record record : result) {
            String hash = record.get("drep_hash", String.class);
            DrepType type = DrepType.valueOf(record.get("drep_type", String.class));
            int interactionEpoch = record.get("epoch", Integer.class);
            long slot = record.get("slot", Long.class);
            int txIndex = record.get("tx_index", Integer.class);
            int eventIndex = record.get("event_index", Integer.class);
            int drepActivity = 0;

            if (dialect.family() == SQLDialect.H2) {
                var paramsJson = record.get(EPOCH_PARAM.PARAMS).data();
                ProtocolParams protocolParam;
                try {
                    protocolParam = objectMapper.readValue(paramsJson, ProtocolParams.class);
                    drepActivity = protocolParam.getDrepActivity();
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse protocol params JSON: {}", paramsJson, e);
                }
            } else {
                drepActivity = record.get("drep_activity", Integer.class);
            }

            var key = new Tuple<>(hash, type);
            map.computeIfAbsent(key, unused -> new ArrayList<>())
                    .add(new DRepExpiryUtil.DRepInteractionInfo(interactionEpoch, drepActivity, slot, txIndex, eventIndex));
        }

        return map;
    }

    private Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepInteractionInfo> findLastInteractions(Set<Tuple<String, DrepType>> drepHashAndTypes, int epoch) {
        if (drepHashAndTypes.isEmpty()) {
            return Collections.emptyMap();
        }

        Field<String> drepActivityField = null;
        SQLDialect dialect = dsl.dialect();

        if (dialect.family() == SQLDialect.POSTGRES) {
            drepActivityField = DSL.field("params->>'drep_activity'", String.class);
        } else if (dialect.family() == SQLDialect.MYSQL) {
            drepActivityField = DSL.function("JSON_EXTRACT", SQLDataType.VARCHAR, DSL.field("params"), DSL.inline("$.drep_activity"));
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

        Result<?> result;
        if (dialect.family() == SQLDialect.POSTGRES || dialect.family() == SQLDialect.MYSQL) {
            result = dsl.select(
                    li.field("drep_hash", String.class),
                    li.field("drep_type", String.class),
                    li.field("epoch", Integer.class),
                    drepActivityField.cast(Integer.class).as("drep_activity")
            )
            .from(li)
            .join(EPOCH_PARAM).on(EPOCH_PARAM.EPOCH.eq(li.field("epoch", Integer.class)))
            .fetch();
        } else {
            result = dsl.select(
                    li.field("drep_hash", String.class),
                    li.field("drep_type", String.class),
                    li.field("epoch", Integer.class),
                    EPOCH_PARAM.PARAMS
            )
            .from(li)
            .join(EPOCH_PARAM).on(EPOCH_PARAM.EPOCH.eq(li.field("epoch", Integer.class)))
            .fetch();
        }

        Map<Tuple<String, DrepType>, DRepExpiryUtil.DRepInteractionInfo> map = new HashMap<>();
        for (Record record : result) {
            String hash = record.get("drep_hash", String.class);
            int interactionEpoch = record.get("epoch", Integer.class);
            DrepType type = DrepType.valueOf(record.get("drep_type", String.class));
            int drepActivity = 0;

            if (dialect.family() == SQLDialect.H2) {
                var paramsJson = record.get(EPOCH_PARAM.PARAMS).data();
                ProtocolParams protocolParam;
                try {
                    protocolParam = objectMapper.readValue(paramsJson, ProtocolParams.class);
                    drepActivity = protocolParam.getDrepActivity();
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse protocol params JSON: {}", paramsJson, e);
                }
            } else {
                drepActivity = record.get("drep_activity", Integer.class);
            }

            map.put(new Tuple<>(hash, type), new DRepExpiryUtil.DRepInteractionInfo(interactionEpoch, drepActivity));
        }

        return map;
    }

    private Set<Integer> findNonDormantProposalStatusEpochs(int fromEpoch, int toEpoch) {
        if (fromEpoch > toEpoch) {
            return Collections.emptySet();
        }

        Field<String> govActionLifetimeField = null;
        SQLDialect dialect = dsl.dialect();

        if (dialect.family() == SQLDialect.POSTGRES) {
            govActionLifetimeField = DSL.field("params->>'gov_action_lifetime'", String.class);
        } else if (dialect.family() == SQLDialect.MYSQL) {
            govActionLifetimeField = DSL.function("JSON_EXTRACT", SQLDataType.VARCHAR, DSL.field("params"), DSL.inline("$.gov_action_lifetime"));
        }

        if (dialect.family() == SQLDialect.POSTGRES || dialect.family() == SQLDialect.MYSQL) {
            Field<Integer> expiresAfter = DSL.field("{0} + {1}", Integer.class,
                    GOV_ACTION_PROPOSAL.EPOCH,
                    govActionLifetimeField.cast(Integer.class));

            return dsl.selectDistinct(GOV_ACTION_PROPOSAL_STATUS.EPOCH)
                    .from(GOV_ACTION_PROPOSAL_STATUS)
                    .join(GOV_ACTION_PROPOSAL).on(GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_TX_HASH.eq(GOV_ACTION_PROPOSAL.TX_HASH)
                            .and(GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_INDEX.eq(GOV_ACTION_PROPOSAL.IDX)))
                    .join(EPOCH_PARAM).on(GOV_ACTION_PROPOSAL.EPOCH.eq(EPOCH_PARAM.EPOCH))
                    .where(nonDormantProposalStatusCondition(fromEpoch, toEpoch)
                            .and(GOV_ACTION_PROPOSAL_STATUS.EPOCH.le(expiresAfter)))
                    .fetchSet(GOV_ACTION_PROPOSAL_STATUS.EPOCH);
        }

        Result<?> result = dsl.select(
                        GOV_ACTION_PROPOSAL_STATUS.EPOCH.as("status_epoch"),
                        GOV_ACTION_PROPOSAL.EPOCH.as("proposal_epoch"),
                        EPOCH_PARAM.PARAMS
                )
                .from(GOV_ACTION_PROPOSAL_STATUS)
                .join(GOV_ACTION_PROPOSAL).on(GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_TX_HASH.eq(GOV_ACTION_PROPOSAL.TX_HASH)
                        .and(GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_INDEX.eq(GOV_ACTION_PROPOSAL.IDX)))
                .join(EPOCH_PARAM).on(GOV_ACTION_PROPOSAL.EPOCH.eq(EPOCH_PARAM.EPOCH))
                .where(nonDormantProposalStatusCondition(fromEpoch, toEpoch))
                .fetch();

        Set<Integer> nonDormantEpochs = new HashSet<>();
        for (Record record : result) {
            int statusEpoch = record.get("status_epoch", Integer.class);
            int proposalEpoch = record.get("proposal_epoch", Integer.class);

            var paramsJson = record.get(EPOCH_PARAM.PARAMS).data();
            try {
                ProtocolParams protocolParam = objectMapper.readValue(paramsJson, ProtocolParams.class);
                if (statusEpoch <= proposalEpoch + protocolParam.getGovActionLifetime()) {
                    nonDormantEpochs.add(statusEpoch);
                }
            } catch (JsonProcessingException e) {
                log.error("Failed to parse protocol params JSON: {}", paramsJson, e);
            }
        }

        return nonDormantEpochs;
    }

    private Condition nonDormantProposalStatusCondition(int fromEpoch, int toEpoch) {
        return GOV_ACTION_PROPOSAL_STATUS.STATUS.in(
                        GovActionStatus.ACTIVE.name(),
                        GovActionStatus.RATIFIED.name())
                .and(GOV_ACTION_PROPOSAL_STATUS.EPOCH.ge(fromEpoch))
                .and(GOV_ACTION_PROPOSAL_STATUS.EPOCH.le(toEpoch));
    }

    private List<DRepExpiryUtil.ProposalSubmissionInfo> findProposalWithEpochLessThanOrEqualTo(int epoch) {
        Field<String> govActionLifetimeField = null;
        SQLDialect dialect = dsl.dialect();

        if (dialect.family() == SQLDialect.POSTGRES) {
            govActionLifetimeField = DSL.field("params->>'gov_action_lifetime'", String.class);
        } else if (dialect.family() == SQLDialect.MYSQL) {
            govActionLifetimeField = DSL.function("JSON_EXTRACT", SQLDataType.VARCHAR, DSL.field("params"), DSL.inline("$.gov_action_lifetime"));
        }

        Result<?> result;
        if (dialect.family() == SQLDialect.POSTGRES || dialect.family() == SQLDialect.MYSQL) {
            result = dsl.select(
                    GOV_ACTION_PROPOSAL.SLOT,
                    GOV_ACTION_PROPOSAL.EPOCH,
                    GOV_ACTION_PROPOSAL.TX_INDEX,
                    GOV_ACTION_PROPOSAL.IDX,
                    govActionLifetimeField.cast(Integer.class).as("gov_action_lifetime")
            )
            .from(GOV_ACTION_PROPOSAL)
            .join(EPOCH_PARAM).on(GOV_ACTION_PROPOSAL.EPOCH.eq(EPOCH_PARAM.EPOCH))
            .where(GOV_ACTION_PROPOSAL.EPOCH.le(epoch))
            .fetch();
        } else {
            result = dsl.select(
                    GOV_ACTION_PROPOSAL.SLOT,
                    GOV_ACTION_PROPOSAL.EPOCH,
                    GOV_ACTION_PROPOSAL.TX_INDEX,
                    GOV_ACTION_PROPOSAL.IDX,
                    EPOCH_PARAM.PARAMS
            )
            .from(GOV_ACTION_PROPOSAL)
            .join(EPOCH_PARAM).on(GOV_ACTION_PROPOSAL.EPOCH.eq(EPOCH_PARAM.EPOCH))
            .where(GOV_ACTION_PROPOSAL.EPOCH.le(epoch))
            .fetch();
        }

        List<DRepExpiryUtil.ProposalSubmissionInfo> proposalInfos = new ArrayList<>();
        for (Record record : result) {
            long slot = record.get(GOV_ACTION_PROPOSAL.SLOT);
            int proposalEpoch = record.get(GOV_ACTION_PROPOSAL.EPOCH);
            int txIndex = record.get(GOV_ACTION_PROPOSAL.TX_INDEX);
            int index = record.get(GOV_ACTION_PROPOSAL.IDX);
            int govActionLifetime = 0;

            if (dialect.family() == SQLDialect.H2) {
                var paramsJson = record.get(EPOCH_PARAM.PARAMS).data();
                ProtocolParams protocolParam;
                try {
                    protocolParam = objectMapper.readValue(paramsJson, ProtocolParams.class);
                    govActionLifetime = protocolParam.getGovActionLifetime();
                } catch (JsonProcessingException e) {
                    log.error("Failed to parse protocol params JSON: {}", paramsJson, e);
                }
            } else {
                govActionLifetime = record.get("gov_action_lifetime", Integer.class);
            }

            proposalInfos.add(new DRepExpiryUtil.ProposalSubmissionInfo(slot, proposalEpoch, govActionLifetime, txIndex, index));
        }

        return proposalInfos;
    }
}
