package com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.model.certs.CertificateType;
import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import com.bloxbean.cardano.yaci.core.model.governance.Vote;
import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.BlockfrostDialectUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.BFGovernanceStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRepDelegator;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRep;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFProposal;
import com.bloxbean.cardano.yaci.store.common.domain.GovActionStatus;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.SortField;
import org.jooq.SortOrder;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.STAKE_ADDRESS_BALANCE;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.ADAPOT_JOBS;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.EPOCH_STAKE;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.INSTANT_REWARD;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD_REST;
import static com.bloxbean.cardano.yaci.store.blocks.jooq.Tables.BLOCK;
import static com.bloxbean.cardano.yaci.store.epoch.jooq.Tables.EPOCH_PARAM;
import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.DREP_DIST;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.GOV_ACTION_PROPOSAL_STATUS;
import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.STAKE_REGISTRATION;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.WITHDRAWAL;

@Slf4j
@Component
@RequiredArgsConstructor
public class BFGovernanceStorageReaderImpl implements BFGovernanceStorageReader {

    private static final String ALWAYS_ABSTAIN_DREP_ID = "drep_always_abstain";
    private static final String ALWAYS_NO_CONFIDENCE_DREP_ID = "drep_always_no_confidence";

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    private int offset(int page, int count) {
        return page * count;
    }

    private JsonNode jsonNodeFromJSON(JSON json) {
        if (json == null) return null;
        try {
            return objectMapper.readTree(json.data());
        } catch (Exception e) {
            log.warn("Failed to parse JSON field: {}", e.getMessage());
            return null;
        }
    }

    private int fetchCurrentEpoch() {
        try {
            Integer epoch = dsl.select(EPOCH_PARAM.EPOCH)
                    .from(EPOCH_PARAM)
                    .orderBy(EPOCH_PARAM.EPOCH.desc())
                    .limit(1)
                    .fetchOne(EPOCH_PARAM.EPOCH);
            return epoch != null ? epoch : 0;
        } catch (Exception e) {
            log.debug("Could not fetch current epoch: {}", e.getMessage());
            return 0;
        }
    }

    private int fetchDRepActivity() {
        try {
            var record = dsl.select(EPOCH_PARAM.PARAMS)
                    .from(EPOCH_PARAM)
                    .orderBy(EPOCH_PARAM.EPOCH.desc())
                    .limit(1)
                    .fetchOne();
            if (record == null) return 0;
            JSON paramsJson = record.get(EPOCH_PARAM.PARAMS);
            if (paramsJson == null) return 0;
            JsonNode params = objectMapper.readTree(paramsJson.data());
            JsonNode drepActivity = params.get("drep_activity");
            return drepActivity != null ? drepActivity.asInt(0) : 0;
        } catch (Exception e) {
            log.debug("Could not fetch drep_activity from epoch_param: {}", e.getMessage());
            return 0;
        }
    }

    private int fetchGovActionLifetime() {
        try {
            var record = dsl.select(EPOCH_PARAM.PARAMS)
                    .from(EPOCH_PARAM)
                    .orderBy(EPOCH_PARAM.EPOCH.desc())
                    .limit(1)
                    .fetchOne();
            if (record == null) return 0;
            JSON paramsJson = record.get(EPOCH_PARAM.PARAMS);
            if (paramsJson == null) return 0;
            JsonNode params = objectMapper.readTree(paramsJson.data());
            JsonNode govActionLifetime = params.get("gov_action_lifetime");
            return govActionLifetime != null ? govActionLifetime.asInt(0) : 0;
        } catch (Exception e) {
            log.debug("Could not fetch gov_action_lifetime from epoch_param: {}", e.getMessage());
            return 0;
        }
    }

    private record ProposalKey(String txHash, int index) {
    }

    private record DRepAnchor(String url, String hash) {
    }

    private Map<ProposalKey, EnumMap<GovActionStatus, Integer>> fetchProposalStatusEpochs(List<ProposalKey> proposalKeys) {
        Map<ProposalKey, EnumMap<GovActionStatus, Integer>> result = new HashMap<>();
        if (proposalKeys.isEmpty()) {
            return result;
        }

        try {
            var statusEpoch = DSL.min(GOV_ACTION_PROPOSAL_STATUS.EPOCH).as("status_epoch");
            var keyRows = proposalKeys.stream()
                    .map(key -> DSL.row(key.txHash(), key.index()))
                    .toList();

            dsl.select(
                            GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_TX_HASH,
                            GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_INDEX,
                            GOV_ACTION_PROPOSAL_STATUS.STATUS,
                            statusEpoch
                    )
                    .from(GOV_ACTION_PROPOSAL_STATUS)
                    .where(DSL.row(
                                    GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_TX_HASH,
                                    GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_INDEX
                            ).in(keyRows))
                    .and(GOV_ACTION_PROPOSAL_STATUS.STATUS.in(
                            GovActionStatus.RATIFIED.name(),
                            GovActionStatus.EXPIRED.name()
                    ))
                    .groupBy(
                            GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_TX_HASH,
                            GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_INDEX,
                            GOV_ACTION_PROPOSAL_STATUS.STATUS
                    )
                    .fetch()
                    .forEach(r -> {
                        ProposalKey key = new ProposalKey(
                                r.get(GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_TX_HASH),
                                r.get(GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_INDEX)
                        );
                        GovActionStatus status = GovActionStatus.valueOf(r.get(GOV_ACTION_PROPOSAL_STATUS.STATUS));
                        result.computeIfAbsent(key, ignored -> new EnumMap<>(GovActionStatus.class))
                                .put(status, r.get(statusEpoch));
                    });
        } catch (Exception e) {
            log.warn("Could not fetch proposal status epochs: {}", e.getMessage());
        }
        return result;
    }

    /** Get current epoch from the block table. */
    private Integer getCurrentEpochFromBlock() {
        try {
            return buildCurrentEpochQuery().fetchOne(0, Integer.class);
        } catch (Exception e) {
            log.warn("Could not get current epoch: {}", e.getMessage());
            return null;
        }
    }

    org.jooq.Select<org.jooq.Record1<Integer>> buildCurrentEpochQuery() {
        return dsl.select(DSL.max(BLOCK.EPOCH)).from(BLOCK);
    }

    private List<BFProposal> enrichProposalRows(List<? extends Record> records, int govActionLifetime) {
        if (records.isEmpty()) {
            return List.of();
        }

        List<ProposalKey> proposalKeys = records.stream()
                .map(this::toProposalKey)
                .toList();
        Integer currentEpoch = getCurrentEpochFromBlock();
        Map<ProposalKey, EnumMap<GovActionStatus, Integer>> statusEpochs = fetchProposalStatusEpochs(proposalKeys);

        return records.stream()
                .map(record -> {
                    ProposalKey key = toProposalKey(record);
                    Map<GovActionStatus, Integer> proposalStatusEpochs = statusEpochs.getOrDefault(
                            key,
                            new EnumMap<>(GovActionStatus.class)
                    );
                    return toProposalRow(record, govActionLifetime, proposalStatusEpochs, currentEpoch);
                })
                .toList();
    }

    private ProposalKey toProposalKey(Record record) {
        return new ProposalKey(
                record.get(GOV_ACTION_PROPOSAL.TX_HASH),
                record.get(GOV_ACTION_PROPOSAL.IDX)
        );
    }

    private BFProposal toProposalRow(Record r,
                                     int govActionLifetime,
                                     Map<GovActionStatus, Integer> statusEpochs,
                                     Integer currentEpoch) {
        String txHash = r.get(GOV_ACTION_PROPOSAL.TX_HASH);
        int index = r.get(GOV_ACTION_PROPOSAL.IDX);
        Integer proposalEpoch = r.get(GOV_ACTION_PROPOSAL.EPOCH);
        Integer ratifiedEpoch = statusEpochs.get(GovActionStatus.RATIFIED);
        Integer enactedEpoch = ratifiedEpoch != null && currentEpoch != null && currentEpoch > ratifiedEpoch
                ? ratifiedEpoch + 1
                : null;
        return BFProposal.builder()
                .txHash(txHash)
                .index(index)
                .type(r.get(GOV_ACTION_PROPOSAL.TYPE))
                .details(jsonNodeFromJSON(r.get(GOV_ACTION_PROPOSAL.DETAILS)))
                .deposit(r.get(GOV_ACTION_PROPOSAL.DEPOSIT))
                .returnAddress(r.get(GOV_ACTION_PROPOSAL.RETURN_ADDRESS))
                .anchorUrl(r.get(GOV_ACTION_PROPOSAL.ANCHOR_URL))
                .anchorHash(r.get(GOV_ACTION_PROPOSAL.ANCHOR_HASH))
                .epoch(proposalEpoch)
                .ratifiedEpoch(ratifiedEpoch)
                .enactedEpoch(enactedEpoch)
                .expiredEpoch(statusEpochs.get(GovActionStatus.EXPIRED))
                .govActionLifetime(govActionLifetime)
                .build();
    }

    private DRepRegistration toDRepRegistrationDomain(Record r) {
        // Map the String type from DB to CertificateType enum
        String typeStr = r.get(DREP_REGISTRATION.TYPE);
        CertificateType certType = null;
        if (typeStr != null) {
            try {
                certType = CertificateType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                log.debug("Unknown DRep registration type: {}", typeStr);
            }
        }
        return DRepRegistration.builder()
                .txHash(r.get(DREP_REGISTRATION.TX_HASH))
                .certIndex(r.get(DREP_REGISTRATION.CERT_INDEX) != null
                        ? r.get(DREP_REGISTRATION.CERT_INDEX).longValue() : 0L)
                .type(certType)
                .drepHash(r.get(DREP_REGISTRATION.DREP_HASH))
                .drepId(r.get(DREP_REGISTRATION.DREP_ID))
                .anchorUrl(r.get(DREP_REGISTRATION.ANCHOR_URL))
                .anchorHash(r.get(DREP_REGISTRATION.ANCHOR_HASH))
                .deposit(r.get(DREP_REGISTRATION.DEPOSIT) != null
                        ? BigInteger.valueOf(r.get(DREP_REGISTRATION.DEPOSIT)) : null)
                .slot(r.get(DREP_REGISTRATION.SLOT))
                .epoch(r.get(DREP_REGISTRATION.EPOCH))
                .build();
    }

    private VotingProcedure toVotingProcedureDomain(Record r) {
        String voterTypeStr = r.get(VOTING_PROCEDURE.VOTER_TYPE);
        VoterType voterType = null;
        if (voterTypeStr != null) {
            try { voterType = VoterType.valueOf(voterTypeStr); }
            catch (IllegalArgumentException ignored) {}
        }
        String voteStr = r.get(VOTING_PROCEDURE.VOTE);
        Vote vote = null;
        if (voteStr != null) {
            try { vote = Vote.valueOf(voteStr); }
            catch (IllegalArgumentException ignored) {}
        }
        return VotingProcedure.builder()
                .txHash(r.get(VOTING_PROCEDURE.TX_HASH))
                .index(r.get(VOTING_PROCEDURE.IDX))
                .txIndex(r.get(VOTING_PROCEDURE.TX_INDEX))
                .slot(r.get(VOTING_PROCEDURE.SLOT))
                .voterHash(r.get(VOTING_PROCEDURE.VOTER_HASH))
                .voterType(voterType)
                .vote(vote)
                .govActionTxHash(r.get(VOTING_PROCEDURE.GOV_ACTION_TX_HASH))
                .govActionIndex(r.get(VOTING_PROCEDURE.GOV_ACTION_INDEX))
                .epoch(r.get(VOTING_PROCEDURE.EPOCH))
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // DRep queries
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public List<BFDRep> findAllDReps(int page, int count, Order order) {
        // A DRep has one row per lifecycle certificate. Select its current state using the
        // complete chain position so multiple certificates in the same slot remain deterministic.
        var latestRanked = dsl.select(
                        DREP.DREP_HASH.as("latest_hash"),
                        DREP.DREP_ID.as("latest_id"),
                        DREP.STATUS.as("latest_status"),
                        DREP.EPOCH.as("latest_epoch"),
                        DREP.REGISTRATION_SLOT.as("registration_slot"),
                        DREP.SLOT.as("latest_slot"),
                        DREP.TX_INDEX.as("latest_tx_index"),
                        DREP.CERT_INDEX.as("latest_cert_index"),
                        DSL.rowNumber().over(DSL.partitionBy(DREP.DREP_HASH)
                                .orderBy(DREP.SLOT.desc(), DREP.TX_INDEX.desc(), DREP.CERT_INDEX.desc()))
                                .as("rn")
                )
                .from(DREP)
                .where(DREP.DREP_ID.isNotNull())
                .asTable("latest_ranked");

        var latest = dsl.select(latestRanked.fields())
                .from(latestRanked)
                .where(latestRanked.field("rn", Integer.class).eq(1))
                .asTable("latest_drep");

        // Blockfrost's default order follows the DRep's first appearance rather than its latest
        // update. The initial registration coordinates are the equivalent ordering key in Yaci.
        var firstRegistrationRanked = dsl.select(
                        DREP_REGISTRATION.DREP_HASH.as("first_hash"),
                        DREP_REGISTRATION.SLOT.as("first_slot"),
                        DREP_REGISTRATION.TX_INDEX.as("first_tx_index"),
                        DREP_REGISTRATION.CERT_INDEX.as("first_cert_index"),
                        DSL.rowNumber().over(DSL.partitionBy(DREP_REGISTRATION.DREP_HASH)
                                .orderBy(DREP_REGISTRATION.SLOT.asc(),
                                        DREP_REGISTRATION.TX_INDEX.asc(),
                                        DREP_REGISTRATION.CERT_INDEX.asc()))
                                .as("rn")
                )
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.TYPE.eq(CertificateType.REG_DREP_CERT.name()))
                .asTable("first_registration_ranked");

        var firstRegistration = dsl.select(firstRegistrationRanked.fields())
                .from(firstRegistrationRanked)
                .where(firstRegistrationRanked.field("rn", Integer.class).eq(1))
                .asTable("first_registration");

        var latestHash = latest.field("latest_hash", String.class);
        var latestSlot = latest.field("latest_slot", Long.class);
        var orderSlot = DSL.coalesce(
                firstRegistration.field("first_slot", Long.class),
                latest.field("registration_slot", Long.class),
                latestSlot
        );
        var orderTxIndex = DSL.coalesce(
                firstRegistration.field("first_tx_index", Integer.class),
                latest.field("latest_tx_index", Integer.class)
        );
        var orderCertIndex = DSL.coalesce(
                firstRegistration.field("first_cert_index", Integer.class),
                latest.field("latest_cert_index", Integer.class)
        );
        var regularDreps = dsl.select(
                        latestHash.as("drep_hash"),
                        latest.field("latest_id", String.class).as("drep_id"),
                        latest.field("latest_status", String.class).as("status"),
                        latest.field("latest_epoch", Integer.class).as("last_active_epoch"),
                        orderSlot.as("order_slot"),
                        orderTxIndex.as("order_tx_index"),
                        orderCertIndex.as("order_cert_index")
                )
                .from(latest)
                .leftJoin(firstRegistration)
                .on(firstRegistration.field("first_hash", String.class).eq(latestHash));

        // The two protocol-defined DReps have no registration row. Their first delegation is the
        // chain event that establishes their relative position among normally registered DReps.
        var specialRanked = dsl.select(
                        DELEGATION_VOTE.DREP_TYPE.as("special_type"),
                        DELEGATION_VOTE.SLOT.as("special_slot"),
                        DELEGATION_VOTE.TX_INDEX.as("special_tx_index"),
                        DELEGATION_VOTE.CERT_INDEX.as("special_cert_index"),
                        DSL.rowNumber().over(DSL.partitionBy(DELEGATION_VOTE.DREP_TYPE)
                                .orderBy(DELEGATION_VOTE.SLOT.asc(),
                                        DELEGATION_VOTE.TX_INDEX.asc(),
                                        DELEGATION_VOTE.CERT_INDEX.asc()))
                                .as("rn")
                )
                .from(DELEGATION_VOTE)
                .where(DELEGATION_VOTE.DREP_TYPE.in(
                        DrepType.ABSTAIN.name(),
                        DrepType.NO_CONFIDENCE.name()
                ))
                .asTable("special_ranked");

        var specialType = specialRanked.field("special_type", String.class);
        var specialDreps = dsl.select(
                        DSL.inline("").as("drep_hash"),
                        DSL.when(specialType.eq(DrepType.ABSTAIN.name()), ALWAYS_ABSTAIN_DREP_ID)
                                .otherwise(ALWAYS_NO_CONFIDENCE_DREP_ID)
                                .as("drep_id"),
                        DSL.inline("REGISTERED").as("status"),
                        DSL.val((Integer) null, Integer.class).as("last_active_epoch"),
                        specialRanked.field("special_slot", Long.class).as("order_slot"),
                        specialRanked.field("special_tx_index", Integer.class).as("order_tx_index"),
                        specialRanked.field("special_cert_index", Integer.class).as("order_cert_index")
                )
                .from(specialRanked)
                .where(specialRanked.field("rn", Integer.class).eq(1));

        var allDreps = regularDreps.unionAll(specialDreps).asTable("all_dreps");
        var allDrepHash = allDreps.field("drep_hash", String.class);
        var allDrepId = allDreps.field("drep_id", String.class);
        SortOrder sortOrder = order == Order.desc ? SortOrder.DESC : SortOrder.ASC;

        // Apply pagination only after regular and protocol-defined DReps share one stable order.
        // Use the DRep ID as a final deterministic tie-breaker when chain coordinates are identical.
        List<BFDRep> dReps = dsl.select(
                        allDrepHash,
                        allDrepId,
                        allDreps.field("status", String.class),
                        allDreps.field("last_active_epoch", Integer.class)
                )
                .from(allDreps)
                .orderBy(
                        allDreps.field("order_slot", Long.class).sort(sortOrder),
                        allDreps.field("order_tx_index", Integer.class).sort(sortOrder),
                        allDreps.field("order_cert_index", Integer.class).sort(sortOrder),
                        allDrepId.sort(sortOrder)
                )
                .limit(count)
                .offset(offset(page, count))
                .fetch(r -> BFDRep.builder()
                        .drepHash(r.get(allDrepHash))
                        .drepId(r.get(allDrepId))
                        .status(r.get(allDreps.field("status", String.class)))
                        .epoch(r.get(allDreps.field("last_active_epoch", Integer.class)))
                        .build());

        enrichDRepList(dReps);
        return dReps;
    }

    private void enrichDRepList(List<BFDRep> dReps) {
        if (dReps.isEmpty()) return;

        List<String> drepHashes = dReps.stream()
                .map(BFDRep::getDrepHash)
                .filter(drepHash -> drepHash != null && !drepHash.isBlank())
                .toList();
        Map<String, Long> amounts = drepHashes.isEmpty() ? Map.of() : fetchLatestDRepAmounts(drepHashes);
        Map<String, Boolean> scriptFlags = drepHashes.isEmpty() ? Map.of() : fetchLatestDRepScriptFlags(drepHashes);
        Map<String, DRepAnchor> anchors = drepHashes.isEmpty() ? Map.of() : fetchLatestDRepAnchors(drepHashes);
        Map<String, Integer> lastVoteEpochs = drepHashes.isEmpty() ? Map.of() : fetchLastDRepVoteEpochs(drepHashes);
        Map<String, Long> specialAmounts = fetchLatestSpecialDRepAmounts();
        int currentEpoch = fetchCurrentEpoch();
        int drepActivity = fetchDRepActivity();

        dReps.forEach(dRep -> {
            String drepHash = dRep.getDrepHash();
            dRep.setAmount(isSpecialDRep(dRep)
                    ? specialAmounts.getOrDefault(dRep.getDrepId(), 0L)
                    : amounts.getOrDefault(drepHash, 0L));
            dRep.setHasScript(scriptFlags.getOrDefault(drepHash, false));

            Integer lastVoteEpoch = lastVoteEpochs.get(drepHash);
            if (lastVoteEpoch != null && (dRep.getEpoch() == null || lastVoteEpoch > dRep.getEpoch())) {
                dRep.setEpoch(lastVoteEpoch);
            }

            boolean retired = "RETIRED".equalsIgnoreCase(dRep.getStatus());
            dRep.setExpired(!retired
                    && dRep.getEpoch() != null
                    && drepActivity > 0
                    && currentEpoch > 0
                    && currentEpoch - dRep.getEpoch() > drepActivity);

            DRepAnchor anchor = anchors.get(drepHash);
            if (anchor != null) {
                dRep.setAnchorUrl(anchor.url());
                dRep.setAnchorHash(anchor.hash());
            }
        });
    }

    private boolean isSpecialDRep(BFDRep dRep) {
        return ALWAYS_ABSTAIN_DREP_ID.equals(dRep.getDrepId())
                || ALWAYS_NO_CONFIDENCE_DREP_ID.equals(dRep.getDrepId());
    }

    private Map<String, Long> fetchLatestSpecialDRepAmounts() {
        Integer latestDistEpoch = dsl.select(DSL.max(DREP_DIST.EPOCH))
                .from(DREP_DIST)
                .fetchOne(0, Integer.class);
        if (latestDistEpoch == null) return Map.of();

        Map<String, Long> result = new HashMap<>();
        dsl.select(DREP_DIST.DREP_TYPE, DREP_DIST.AMOUNT)
                .from(DREP_DIST)
                .where(DREP_DIST.EPOCH.eq(latestDistEpoch))
                .and(DREP_DIST.DREP_TYPE.in(DrepType.ABSTAIN.name(), DrepType.NO_CONFIDENCE.name()))
                .fetch()
                .forEach(record -> {
                    String drepId = DrepType.ABSTAIN.name().equals(record.get(DREP_DIST.DREP_TYPE))
                            ? ALWAYS_ABSTAIN_DREP_ID
                            : ALWAYS_NO_CONFIDENCE_DREP_ID;
                    result.put(drepId, record.get(DREP_DIST.AMOUNT));
                });
        return result;
    }

    private Map<String, Long> fetchLatestDRepAmounts(List<String> drepHashes) {
        Integer latestLocalEpoch = dsl.select(DSL.max(LOCAL_DREP_DIST.EPOCH))
                .from(LOCAL_DREP_DIST)
                .fetchOne(0, Integer.class);

        if (latestLocalEpoch != null) {
            return dsl.select(LOCAL_DREP_DIST.DREP_HASH, LOCAL_DREP_DIST.AMOUNT)
                    .from(LOCAL_DREP_DIST)
                    .where(LOCAL_DREP_DIST.DREP_HASH.in(drepHashes))
                    .and(LOCAL_DREP_DIST.EPOCH.eq(latestLocalEpoch))
                    .fetchMap(LOCAL_DREP_DIST.DREP_HASH, LOCAL_DREP_DIST.AMOUNT);
        }

        Integer latestDistEpoch = dsl.select(DSL.max(DREP_DIST.EPOCH))
                .from(DREP_DIST)
                .fetchOne(0, Integer.class);
        if (latestDistEpoch == null) return Map.of();

        Map<String, Long> amounts = new HashMap<>();
        dsl.select(DREP_DIST.DREP_HASH, DREP_DIST.AMOUNT)
                .from(DREP_DIST)
                .where(DREP_DIST.DREP_HASH.in(drepHashes))
                .and(DREP_DIST.EPOCH.eq(latestDistEpoch))
                .fetch()
                .forEach(record -> amounts.put(record.get(DREP_DIST.DREP_HASH), record.get(DREP_DIST.AMOUNT)));
        return amounts;
    }

    private Map<String, Boolean> fetchLatestDRepScriptFlags(List<String> drepHashes) {
        var ranked = dsl.select(
                        DREP_REGISTRATION.DREP_HASH,
                        DREP_REGISTRATION.CRED_TYPE,
                        DSL.rowNumber().over(DSL.partitionBy(DREP_REGISTRATION.DREP_HASH)
                                .orderBy(DREP_REGISTRATION.SLOT.desc(),
                                        DREP_REGISTRATION.TX_INDEX.desc(),
                                        DREP_REGISTRATION.CERT_INDEX.desc()))
                                .as("rn")
                )
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.DREP_HASH.in(drepHashes))
                .asTable("latest_registration_ranked");

        Map<String, Boolean> result = new HashMap<>();
        dsl.select(ranked.field(DREP_REGISTRATION.DREP_HASH), ranked.field(DREP_REGISTRATION.CRED_TYPE))
                .from(ranked)
                .where(ranked.field("rn", Integer.class).eq(1))
                .fetch()
                .forEach(record -> result.put(
                        record.get(ranked.field(DREP_REGISTRATION.DREP_HASH)),
                        "SCRIPTHASH".equalsIgnoreCase(record.get(ranked.field(DREP_REGISTRATION.CRED_TYPE)))
                ));
        return result;
    }

    private Map<String, DRepAnchor> fetchLatestDRepAnchors(List<String> drepHashes) {
        var ranked = dsl.select(
                        DREP_REGISTRATION.DREP_HASH,
                        DREP_REGISTRATION.ANCHOR_URL,
                        DREP_REGISTRATION.ANCHOR_HASH,
                        DSL.rowNumber().over(DSL.partitionBy(DREP_REGISTRATION.DREP_HASH)
                                .orderBy(DREP_REGISTRATION.SLOT.desc(),
                                        DREP_REGISTRATION.TX_INDEX.desc(),
                                        DREP_REGISTRATION.CERT_INDEX.desc()))
                                .as("rn")
                )
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.DREP_HASH.in(drepHashes))
                .and(DREP_REGISTRATION.ANCHOR_URL.isNotNull())
                .asTable("latest_anchor_ranked");

        Map<String, DRepAnchor> result = new HashMap<>();
        dsl.select(
                        ranked.field(DREP_REGISTRATION.DREP_HASH),
                        ranked.field(DREP_REGISTRATION.ANCHOR_URL),
                        ranked.field(DREP_REGISTRATION.ANCHOR_HASH)
                )
                .from(ranked)
                .where(ranked.field("rn", Integer.class).eq(1))
                .fetch()
                .forEach(record -> result.put(
                        record.get(ranked.field(DREP_REGISTRATION.DREP_HASH)),
                        new DRepAnchor(
                                record.get(ranked.field(DREP_REGISTRATION.ANCHOR_URL)),
                                record.get(ranked.field(DREP_REGISTRATION.ANCHOR_HASH))
                        )
                ));
        return result;
    }

    private Map<String, Integer> fetchLastDRepVoteEpochs(List<String> drepHashes) {
        var lastVoteEpoch = DSL.max(VOTING_PROCEDURE.EPOCH).as("last_vote_epoch");
        return dsl.select(VOTING_PROCEDURE.VOTER_HASH, lastVoteEpoch)
                .from(VOTING_PROCEDURE)
                .where(VOTING_PROCEDURE.VOTER_HASH.in(drepHashes))
                .and(VOTING_PROCEDURE.VOTER_TYPE.in("DREP_KEY_HASH", "DREP_SCRIPT_HASH"))
                .groupBy(VOTING_PROCEDURE.VOTER_HASH)
                .fetchMap(VOTING_PROCEDURE.VOTER_HASH, lastVoteEpoch);
    }

    @Override
    public Optional<BFDRep> findDRepByHash(String drepHex) {
        var record = dsl.select(DREP.DREP_ID, DREP.DREP_HASH, DREP.STATUS, DREP.EPOCH)
                .from(DREP)
                .where(DREP.DREP_HASH.eq(drepHex))
                .orderBy(DREP.SLOT.desc())
                .limit(1)
                .fetchOne();
        if (record == null) return Optional.empty();

        boolean hasScript = dsl.select(DREP_REGISTRATION.CRED_TYPE)
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.DREP_HASH.eq(drepHex))
                .orderBy(DREP_REGISTRATION.SLOT.desc())
                .limit(1)
                .fetchOptional()
                .map(r -> "SCRIPTHASH".equalsIgnoreCase(r.get(DREP_REGISTRATION.CRED_TYPE)))
                .orElse(false);

        Integer firstRegistrationEpoch = dsl.select(DREP_REGISTRATION.EPOCH)
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.DREP_HASH.eq(drepHex))
                .and(DREP_REGISTRATION.TYPE.eq(CertificateType.REG_DREP_CERT.name()))
                .orderBy(DREP_REGISTRATION.SLOT.asc())
                .limit(1)
                .fetchOptional(DREP_REGISTRATION.EPOCH)
                .orElse(null);

        // Try local_drep_dist first (from local node), fall back to drep_dist (from epoch state)
        Long amount = dsl.select(LOCAL_DREP_DIST.AMOUNT)
                .from(LOCAL_DREP_DIST)
                .where(LOCAL_DREP_DIST.DREP_HASH.eq(drepHex))
                .orderBy(LOCAL_DREP_DIST.EPOCH.desc())
                .limit(1)
                .fetchOptional(LOCAL_DREP_DIST.AMOUNT)
                .orElseGet(() ->
                        dsl.select(DREP_DIST.AMOUNT)
                                .from(DREP_DIST)
                                .where(DREP_DIST.DREP_HASH.eq(drepHex))
                                .orderBy(DREP_DIST.EPOCH.desc())
                                .limit(1)
                                .fetchOptional(DREP_DIST.AMOUNT)
                                .orElse(null)
                );

        String status = record.get(DREP.STATUS) != null ? record.get(DREP.STATUS).toString() : null;
        Integer lastCertEpoch = record.get(DREP.EPOCH);

        // last_active_epoch = max(last cert epoch, last vote epoch)
        Integer lastVoteEpoch = dsl.select(DSL.max(VOTING_PROCEDURE.EPOCH))
                .from(VOTING_PROCEDURE)
                .where(VOTING_PROCEDURE.VOTER_HASH.eq(drepHex))
                .and(VOTING_PROCEDURE.VOTER_TYPE.in("DREP_KEY_HASH", "DREP_SCRIPT_HASH"))
                .fetchOne(0, Integer.class);
        Integer lastActiveEpoch = (lastVoteEpoch != null && (lastCertEpoch == null || lastVoteEpoch > lastCertEpoch))
                ? lastVoteEpoch : lastCertEpoch;

        boolean retired = "RETIRED".equalsIgnoreCase(status);

        // A DRep is expired if it is not retired and has been inactive for more than drep_activity epochs
        boolean expired = false;
        if (!retired && lastActiveEpoch != null) {
            int currentEpoch = fetchCurrentEpoch();
            int drepActivity = fetchDRepActivity();
            if (drepActivity > 0 && currentEpoch > 0) {
                expired = (currentEpoch - lastActiveEpoch) > drepActivity;
            }
        }

        return Optional.of(BFDRep.builder()
                .drepId(record.get(DREP.DREP_ID))
                .drepHash(drepHex)
                .status(status)
                .epoch(lastActiveEpoch)
                .activeEpoch(firstRegistrationEpoch)
                .amount(amount)
                .hasScript(hasScript)
                .expired(expired)
                .build());
    }

    @Override
    public List<BFDRepDelegator> findDRepDelegators(String drepHex, int page, int count, Order order) {
        // The latest completed AdaPot reward calculation provides a stable epoch_stake baseline.
        // This lets the endpoint read only reward changes after that epoch instead of full history.
        Integer snapshotEpoch = findLatestStakeSnapshotEpoch();

        // Resolve active delegators and pagination first, then load balance data only for this page.
        var delegators = buildDRepDelegatorsQuery(drepHex, page, count, order, snapshotEpoch).fetch();
        if (delegators.isEmpty()) {
            return List.of();
        }

        // Addresses with an epoch_stake row can start from the checkpoint. Missing rows fall back
        // to full reward history so an incomplete snapshot does not omit their reward balance.
        List<String> checkpointAddresses = delegators.stream()
                .filter(r -> r.get("checkpoint_amount", Long.class) != null)
                .map(r -> r.get("address", String.class))
                .toList();
        List<String> addressesWithoutCheckpoint = delegators.stream()
                .filter(r -> r.get("checkpoint_amount", Long.class) == null)
                .map(r -> r.get("address", String.class))
                .toList();

        // Both calls are batched by address group and populate the same map: post-checkpoint
        // changes for checkpointed addresses, and lifetime changes for fallback addresses.
        Map<String, Long> rewardChanges = new HashMap<>();
        fetchRewardChanges(rewardChanges, checkpointAddresses, snapshotEpoch);
        fetchRewardChanges(rewardChanges, addressesWithoutCheckpoint, null);

        return delegators
                .map(r -> {
                    String address = r.get("address", String.class);
                    // stake_address_balance supplies the current UTxO component.
                    long currentBalance = r.get("amount", Long.class);
                    Long checkpointAmount = r.get("checkpoint_amount", Long.class);
                    long snapshotBalance = Optional.ofNullable(r.get("snapshot_balance", Long.class)).orElse(0L);
                    // epoch_stake stores controlled amount. Subtract its UTxO component to recover
                    // the reward balance at the checkpoint, then apply only later reward changes.
                    // For fallback addresses checkpointReward is zero and rewardChanges contains
                    // their complete reward and withdrawal history.
                    long checkpointReward = checkpointAmount == null ? 0L : checkpointAmount - snapshotBalance;
                    // Unclaimed rewards cannot be negative; withdrawals can reduce them only to zero.
                    long withdrawableReward = Math.max(
                            checkpointReward + rewardChanges.getOrDefault(address, 0L),
                            0L
                    );
                    // Blockfrost amount is controlled stake: current UTxO plus unclaimed rewards.
                    return BFDRepDelegator.builder()
                            .address(address)
                            .amount(currentBalance + withdrawableReward)
                            .build();
                });
    }

    private Integer findLatestStakeSnapshotEpoch() {
        Integer completedJobEpoch = dsl.select(ADAPOT_JOBS.EPOCH)
                .from(ADAPOT_JOBS)
                .where(ADAPOT_JOBS.TYPE.eq("REWARD_CALC"))
                .and(ADAPOT_JOBS.STATUS.eq("COMPLETED"))
                .orderBy(ADAPOT_JOBS.EPOCH.desc())
                .limit(1)
                .fetchOne(ADAPOT_JOBS.EPOCH);
        // Reward calculation for job epoch N materializes the epoch_stake snapshot for N - 1.
        return completedJobEpoch == null ? null : completedJobEpoch - 1;
    }

    private void fetchRewardChanges(Map<String, Long> rewardChanges,
                                    List<String> addresses,
                                    Integer snapshotEpoch) {
        if (addresses.isEmpty()) {
            return;
        }

        buildRewardBalanceQuery(addresses, snapshotEpoch).fetch().forEach(r -> rewardChanges.put(
                r.get("address", String.class),
                r.get("amount", Long.class)
        ));
    }

    Select<?> buildDRepDelegatorsQuery(String drepHex, int page, int count, Order order, Integer snapshotEpoch) {
        SortOrder sortOrder = order == Order.desc ? SortOrder.DESC : SortOrder.ASC;
        var newerDelegation = DELEGATION_VOTE.as("newer");
        var newerStakeRegistration = STAKE_REGISTRATION.as("newer_stake_reg");
        var newerDRepLifecycleEvent = DREP_REGISTRATION.as("newer_drep_lifecycle");

        // Chain order includes transaction and certificate positions because multiple delegation
        // certificates can share a slot. Resolve current delegators before pagination so balance
        // lookups stay bounded to the requested page.
        var newerInChainOrder = newerDelegation.SLOT.gt(DELEGATION_VOTE.SLOT)
                .or(newerDelegation.SLOT.eq(DELEGATION_VOTE.SLOT)
                        .and(newerDelegation.TX_INDEX.gt(DELEGATION_VOTE.TX_INDEX)))
                .or(newerDelegation.SLOT.eq(DELEGATION_VOTE.SLOT)
                        .and(newerDelegation.TX_INDEX.eq(DELEGATION_VOTE.TX_INDEX))
                        .and(newerDelegation.CERT_INDEX.gt(DELEGATION_VOTE.CERT_INDEX)));
        var newerStakeRegistrationInChainOrder = newerStakeRegistration.SLOT.gt(STAKE_REGISTRATION.SLOT)
                .or(newerStakeRegistration.SLOT.eq(STAKE_REGISTRATION.SLOT)
                        .and(newerStakeRegistration.TX_INDEX.gt(STAKE_REGISTRATION.TX_INDEX)))
                .or(newerStakeRegistration.SLOT.eq(STAKE_REGISTRATION.SLOT)
                        .and(newerStakeRegistration.TX_INDEX.eq(STAKE_REGISTRATION.TX_INDEX))
                        .and(newerStakeRegistration.CERT_INDEX.gt(STAKE_REGISTRATION.CERT_INDEX)));
        var delegationAtOrAfterCurrentRegistration = DELEGATION_VOTE.SLOT.gt(STAKE_REGISTRATION.SLOT)
                .or(DELEGATION_VOTE.SLOT.eq(STAKE_REGISTRATION.SLOT)
                        .and(DELEGATION_VOTE.TX_INDEX.gt(STAKE_REGISTRATION.TX_INDEX)))
                .or(DELEGATION_VOTE.SLOT.eq(STAKE_REGISTRATION.SLOT)
                        .and(DELEGATION_VOTE.TX_INDEX.eq(STAKE_REGISTRATION.TX_INDEX))
                        .and(DELEGATION_VOTE.CERT_INDEX.ge(STAKE_REGISTRATION.CERT_INDEX)));
        var newerDRepLifecycleEventInChainOrder = newerDRepLifecycleEvent.SLOT.gt(DREP_REGISTRATION.SLOT)
                .or(newerDRepLifecycleEvent.SLOT.eq(DREP_REGISTRATION.SLOT)
                        .and(newerDRepLifecycleEvent.TX_INDEX.gt(DREP_REGISTRATION.TX_INDEX)))
                .or(newerDRepLifecycleEvent.SLOT.eq(DREP_REGISTRATION.SLOT)
                        .and(newerDRepLifecycleEvent.TX_INDEX.eq(DREP_REGISTRATION.TX_INDEX))
                        .and(newerDRepLifecycleEvent.CERT_INDEX.gt(DREP_REGISTRATION.CERT_INDEX)));
        var delegationAtOrAfterCurrentDRepRegistration = DELEGATION_VOTE.SLOT.gt(DREP_REGISTRATION.SLOT)
                .or(DELEGATION_VOTE.SLOT.eq(DREP_REGISTRATION.SLOT)
                        .and(DELEGATION_VOTE.TX_INDEX.gt(DREP_REGISTRATION.TX_INDEX)))
                .or(DELEGATION_VOTE.SLOT.eq(DREP_REGISTRATION.SLOT)
                        .and(DELEGATION_VOTE.TX_INDEX.eq(DREP_REGISTRATION.TX_INDEX))
                        .and(DELEGATION_VOTE.CERT_INDEX.ge(DREP_REGISTRATION.CERT_INDEX)));

        // Deregistration clears vote delegation. Require the latest stake-registration event to
        // be a registration, and ignore a delegation left over from an earlier registration cycle.
        var hasActiveRegistrationForDelegation = DSL.exists(
                dsl.selectOne()
                        .from(STAKE_REGISTRATION)
                        .where(STAKE_REGISTRATION.ADDRESS.eq(DELEGATION_VOTE.ADDRESS))
                        .and(STAKE_REGISTRATION.TYPE.eq("STAKE_REGISTRATION"))
                        .and(delegationAtOrAfterCurrentRegistration)
                        .andNotExists(
                                dsl.selectOne()
                                        .from(newerStakeRegistration)
                                        .where(newerStakeRegistration.ADDRESS.eq(STAKE_REGISTRATION.ADDRESS))
                                        .and(newerStakeRegistrationInChainOrder)
                        )
        );
        // Unregistering a DRep clears all delegations in ledger state. Updates do not start a new
        // lifecycle, so only registration and unregistration certificates delimit the cycle.
        // A later registration starts empty and requires stake accounts to delegate again.
        var hasActiveDRepRegistrationForDelegation = DSL.exists(
                dsl.selectOne()
                        .from(DREP_REGISTRATION)
                        .where(DREP_REGISTRATION.DREP_HASH.eq(drepHex))
                        .and(DREP_REGISTRATION.TYPE.eq("REG_DREP_CERT"))
                        .and(delegationAtOrAfterCurrentDRepRegistration)
                        .andNotExists(
                                dsl.selectOne()
                                        .from(newerDRepLifecycleEvent)
                                        .where(newerDRepLifecycleEvent.DREP_HASH.eq(DREP_REGISTRATION.DREP_HASH))
                                        .and(newerDRepLifecycleEvent.TYPE.in("REG_DREP_CERT", "UNREG_DREP_CERT"))
                                        .and(newerDRepLifecycleEventInChainOrder)
                        )
        );
        var pagedDelegators = dsl.select(
                        DELEGATION_VOTE.ADDRESS.as("address"),
                        DELEGATION_VOTE.SLOT.as("max_slot"),
                        DELEGATION_VOTE.TX_INDEX.as("max_tx_index"),
                        DELEGATION_VOTE.CERT_INDEX.as("max_cert_index")
                )
                .from(DELEGATION_VOTE)
                .where(DELEGATION_VOTE.DREP_HASH.eq(drepHex))
                .and(hasActiveRegistrationForDelegation)
                .and(hasActiveDRepRegistrationForDelegation)
                .and(DSL.notExists(
                        dsl.selectOne()
                                .from(newerDelegation)
                                .where(newerDelegation.ADDRESS.eq(DELEGATION_VOTE.ADDRESS))
                                .and(newerInChainOrder)
                ))
                .orderBy(
                        DELEGATION_VOTE.SLOT.sort(sortOrder),
                        DELEGATION_VOTE.TX_INDEX.sort(sortOrder),
                        DELEGATION_VOTE.CERT_INDEX.sort(sortOrder)
                )
                .limit(count)
                .offset(offset(page, count))
                .asTable("paged_del");

        var address = pagedDelegators.field("address", String.class);
        var maxSlot = pagedDelegators.field("max_slot", Long.class);
        var maxTxIndex = pagedDelegators.field("max_tx_index", Integer.class);
        var maxCertIndex = pagedDelegators.field("max_cert_index", Integer.class);

        // Balance snapshots are keyed by address and slot, so this lookup can fetch the latest
        // balance for each paged delegator without rebuilding it from historical UTxOs.
        var latestBalance = dsl.select(STAKE_ADDRESS_BALANCE.QUANTITY.cast(Long.class))
                .from(STAKE_ADDRESS_BALANCE)
                .where(STAKE_ADDRESS_BALANCE.ADDRESS.eq(address))
                .orderBy(STAKE_ADDRESS_BALANCE.SLOT.desc())
                .limit(1)
                .asField();
        var checkpointAmount = snapshotEpoch == null
                ? DSL.val((Long) null)
                : dsl.select(EPOCH_STAKE.AMOUNT.cast(Long.class))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.EPOCH.eq(snapshotEpoch))
                .and(EPOCH_STAKE.ADDRESS.eq(address))
                .asField();
        var balanceAtSnapshot = snapshotEpoch == null
                ? DSL.val((Long) null)
                : dsl.select(STAKE_ADDRESS_BALANCE.QUANTITY.cast(Long.class))
                .from(STAKE_ADDRESS_BALANCE)
                .where(STAKE_ADDRESS_BALANCE.ADDRESS.eq(address))
                .and(STAKE_ADDRESS_BALANCE.EPOCH.le(snapshotEpoch))
                .orderBy(STAKE_ADDRESS_BALANCE.SLOT.desc())
                .limit(1)
                .asField();

        return dsl.select(
                        address.as("address"),
                        DSL.coalesce(latestBalance, DSL.inline(0L)).as("amount"),
                        checkpointAmount.as("checkpoint_amount"),
                        balanceAtSnapshot.as("snapshot_balance")
                )
                .from(pagedDelegators)
                .orderBy(
                        maxSlot.sort(sortOrder),
                        maxTxIndex.sort(sortOrder),
                        maxCertIndex.sort(sortOrder)
                );
    }

    Select<?> buildRewardBalanceQuery(List<String> addresses, Integer snapshotEpoch) {
        var rewardCondition = REWARD.ADDRESS.in(addresses);
        var rewardRestCondition = REWARD_REST.ADDRESS.in(addresses);
        var instantRewardCondition = INSTANT_REWARD.ADDRESS.in(addresses);
        var withdrawalCondition = WITHDRAWAL.ADDRESS.in(addresses);

        if (snapshotEpoch != null) {
            // The static partition-key lower bound is essential: PostgreSQL can prune all reward
            // partitions already represented by the epoch stake checkpoint.
            // Regular pool rewards earned through snapshot epoch E are already in epoch_stake(E),
            // including rewards that first become spendable in E + 1. Refunds are included only
            // when spendable, so their cutoff remains E. Instant rewards use the same E + 1
            // spendable boundary as the snapshot calculation.
            rewardCondition = rewardCondition
                    .and(REWARD.SPENDABLE_EPOCH.gt(snapshotEpoch))
                    .and(REWARD.TYPE.eq("refund")
                            .or(REWARD.TYPE.in("member", "leader")
                                    .and(REWARD.EARNED_EPOCH.gt(snapshotEpoch)
                                            .or(REWARD.SPENDABLE_EPOCH.gt(snapshotEpoch + 1)))));
            rewardRestCondition = rewardRestCondition
                    .and(REWARD_REST.SPENDABLE_EPOCH.gt(snapshotEpoch));
            instantRewardCondition = instantRewardCondition
                    .and(INSTANT_REWARD.SPENDABLE_EPOCH.gt(snapshotEpoch + 1));
            withdrawalCondition = withdrawalCondition
                    .and(WITHDRAWAL.EPOCH.gt(snapshotEpoch));
        }

        // Blockfrost's controlled amount includes unclaimed rewards. Aggregate changes for the
        // whole page in one query; addresses without a checkpoint fall back to their full history.
        var rewardChanges = dsl.select(
                        REWARD.ADDRESS.as("address"),
                        REWARD.AMOUNT.cast(Long.class).as("amount")
                )
                .from(REWARD)
                .where(rewardCondition)
                .unionAll(dsl.select(
                                REWARD_REST.ADDRESS.as("address"),
                                REWARD_REST.AMOUNT.cast(Long.class).as("amount")
                        )
                        .from(REWARD_REST)
                        .where(rewardRestCondition))
                .unionAll(dsl.select(
                                INSTANT_REWARD.ADDRESS.as("address"),
                                INSTANT_REWARD.AMOUNT.cast(Long.class).as("amount")
                        )
                        .from(INSTANT_REWARD)
                        .where(instantRewardCondition))
                .unionAll(dsl.select(
                                WITHDRAWAL.ADDRESS.as("address"),
                                WITHDRAWAL.AMOUNT.neg().cast(Long.class).as("amount")
                        )
                        .from(WITHDRAWAL)
                        .where(withdrawalCondition))
                .asTable("reward_changes");

        var address = rewardChanges.field("address", String.class);
        var amount = rewardChanges.field("amount", Long.class);
        return dsl.select(address.as("address"), DSL.sum(amount).cast(Long.class).as("amount"))
                .from(rewardChanges)
                .groupBy(address);
    }

    @Override
    public List<DRepRegistration> findDRepUpdates(String drepHex, int page, int count, Order order) {
        SortField<?> sortField = order == Order.desc
                ? DREP_REGISTRATION.SLOT.desc()
                : DREP_REGISTRATION.SLOT.asc();
        return dsl.select()
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.DREP_HASH.eq(drepHex))
                .orderBy(sortField)
                .limit(count)
                .offset(offset(page, count))
                .fetch()
                .map(this::toDRepRegistrationDomain);
    }

    @Override
    public List<VotingProcedure> findDRepVotes(String drepHex, int page, int count, Order order) {
        SortField<?> sortField = order == Order.desc
                ? VOTING_PROCEDURE.SLOT.desc()
                : VOTING_PROCEDURE.SLOT.asc();
        return dsl.select()
                .from(VOTING_PROCEDURE)
                .where(VOTING_PROCEDURE.VOTER_HASH.eq(drepHex))
                .and(VOTING_PROCEDURE.VOTER_TYPE.in("DREP_KEY_HASH", "DREP_SCRIPT_HASH"))
                .orderBy(sortField)
                .limit(count)
                .offset(offset(page, count))
                .fetch()
                .map(this::toVotingProcedureDomain);
    }

    @Override
    public Optional<DRepRegistration> findDRepMetadata(String drepHex) {
        return dsl.select()
                .from(DREP_REGISTRATION)
                .where(DREP_REGISTRATION.DREP_HASH.eq(drepHex))
                .and(DREP_REGISTRATION.ANCHOR_URL.isNotNull())
                .orderBy(DREP_REGISTRATION.SLOT.desc())
                .limit(1)
                .fetchOptional()
                .map(this::toDRepRegistrationDomain);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Proposal queries
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public List<BFProposal> findAllProposals(int page, int count, Order order) {
        SortField<?> sortField = order == Order.desc
                ? GOV_ACTION_PROPOSAL.SLOT.desc()
                : GOV_ACTION_PROPOSAL.SLOT.asc();
        int govActionLifetime = fetchGovActionLifetime();
        var proposals = dsl.select()
                .from(GOV_ACTION_PROPOSAL)
                .orderBy(sortField)
                .limit(count)
                .offset(offset(page, count))
                .fetch();
        return enrichProposalRows(proposals, govActionLifetime);
    }

    @Override
    public Optional<BFProposal> findProposalByTxHashAndIndex(String txHash, int index) {
        int govActionLifetime = fetchGovActionLifetime();
        var proposal = dsl.select()
                .from(GOV_ACTION_PROPOSAL)
                .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                .and(GOV_ACTION_PROPOSAL.IDX.eq(index))
                .fetchOptional();
        return proposal.map(record -> enrichProposalRows(List.of(record), govActionLifetime).getFirst());
    }

    @Override
    public Optional<BFProposal> findParameterChangeProposal(String txHash, int index) {
        int govActionLifetime = fetchGovActionLifetime();
        var proposal = dsl.select()
                .from(GOV_ACTION_PROPOSAL)
                .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                .and(GOV_ACTION_PROPOSAL.IDX.eq(index))
                .and(GOV_ACTION_PROPOSAL.TYPE.eq("PARAMETER_CHANGE_ACTION"))
                .fetchOptional();
        return proposal.map(record -> enrichProposalRows(List.of(record), govActionLifetime).getFirst());
    }

    @Override
    public boolean isWithdrawalProposal(String txHash, int index) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(GOV_ACTION_PROPOSAL)
                        .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                        .and(GOV_ACTION_PROPOSAL.IDX.eq(index))
                        .and(GOV_ACTION_PROPOSAL.TYPE.eq("TREASURY_WITHDRAWALS_ACTION"))
        );
    }

    @Override
    public List<BFDRepDelegator> findProposalWithdrawals(String txHash, int index) {
        // Read withdrawals from gov_action_proposal.details JSON — no adapot dependency
        return dsl.select(GOV_ACTION_PROPOSAL.DETAILS)
                .from(GOV_ACTION_PROPOSAL)
                .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                .and(GOV_ACTION_PROPOSAL.IDX.eq(index))
                .and(GOV_ACTION_PROPOSAL.TYPE.eq("TREASURY_WITHDRAWALS_ACTION"))
                .fetchOptional(GOV_ACTION_PROPOSAL.DETAILS)
                .map(json -> {
                    List<BFDRepDelegator> result = new ArrayList<>();
                    try {
                        JsonNode details = objectMapper.readTree(json.data());
                        JsonNode withdrawals = details.get("withdrawals");
                        if (withdrawals != null && withdrawals.isObject()) {
                            withdrawals.fields().forEachRemaining(entry -> result.add(
                                    BFDRepDelegator.builder()
                                            .address(entry.getKey())
                                            .amount(entry.getValue().asLong())
                                            .build()
                            ));
                        }
                    } catch (Exception e) {
                        log.warn("Could not parse withdrawals from proposal details for {}/{}: {}", txHash, index, e.getMessage());
                    }
                    return result;
                })
                .orElse(new ArrayList<>());
    }

    @Override
    public List<VotingProcedure> findProposalVotes(String txHash, int index, int page, int count, Order order) {
        SortField<?> sortField = order == Order.desc
                ? VOTING_PROCEDURE.SLOT.desc()
                : VOTING_PROCEDURE.SLOT.asc();
        return dsl.select()
                .from(VOTING_PROCEDURE)
                .where(VOTING_PROCEDURE.GOV_ACTION_TX_HASH.eq(txHash))
                .and(VOTING_PROCEDURE.GOV_ACTION_INDEX.eq(index))
                .orderBy(sortField, VOTING_PROCEDURE.IDX.asc())
                .limit(count)
                .offset(offset(page, count))
                .fetch()
                .map(this::toVotingProcedureDomain);
    }

    @Override
    public Optional<BFProposal> findProposalMetadata(String txHash, int index) {
        int govActionLifetime = fetchGovActionLifetime();
        var proposal = dsl.select()
                .from(GOV_ACTION_PROPOSAL)
                .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                .and(GOV_ACTION_PROPOSAL.IDX.eq(index))
                .and(GOV_ACTION_PROPOSAL.ANCHOR_URL.isNotNull())
                .fetchOptional();
        return proposal.map(record -> enrichProposalRows(List.of(record), govActionLifetime).getFirst());
    }
}
