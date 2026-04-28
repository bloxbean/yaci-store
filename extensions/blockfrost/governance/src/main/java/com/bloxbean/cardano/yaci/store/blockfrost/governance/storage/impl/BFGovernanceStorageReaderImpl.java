package com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.common.util.BlockfrostDialectUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.BFGovernanceStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRepDelegator;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRep;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFProposal;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.bloxbean.cardano.yaci.store.governance.domain.DRepRegistration;
import com.bloxbean.cardano.yaci.store.governance.domain.VotingProcedure;
import com.bloxbean.cardano.yaci.store.governance.storage.impl.model.DRepEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.JSON;
import org.jooq.SortField;
import org.jooq.SortOrder;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bloxbean.cardano.yaci.store.epoch.jooq.Tables.EPOCH_PARAM;
import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.DREP_DIST;
import static com.bloxbean.cardano.yaci.store.governance_aggr.jooq.Tables.GOV_ACTION_PROPOSAL_STATUS;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.ADDRESS_UTXO;
import static com.bloxbean.cardano.yaci.store.utxo.jooq.Tables.TX_INPUT;

@Slf4j
@Component
@RequiredArgsConstructor
public class BFGovernanceStorageReaderImpl implements BFGovernanceStorageReader {

    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Normalise a DRep ID to the raw 28-byte hex stored in the drep_hash column.
     * Accepts:
     *  - 56-char hex (raw hash, stored in DB as-is)
     *  - 66-char hex (CIP-129 with 1-byte credential header, strip first byte)
     *  - bech32 drep1... (decoded bytes may be 29 bytes with header, strip first byte)
     */
    String normalizeDRepId(String drepIdOrHex) {
        if (drepIdOrHex == null) return null;
        // Already a raw 56-char hex hash (no header byte)
        if (drepIdOrHex.matches("[0-9a-fA-F]{56}")) {
            return drepIdOrHex.toLowerCase();
        }
        // 66-char hex: CIP-129 format with 1-byte credential type prefix (e.g. "22" + 64 chars)
        if (drepIdOrHex.matches("[0-9a-fA-F]{66}")) {
            return drepIdOrHex.substring(2).toLowerCase();
        }
        // bech32: drep1... - decoded bytes are 29 bytes (1 header + 28 hash)
        try {
            Bech32.Bech32Data decoded = Bech32.decode(drepIdOrHex);
            byte[] data = decoded.data;
            // If 29 bytes, first byte is credential type header - strip it
            if (data.length == 29) {
                byte[] hash = java.util.Arrays.copyOfRange(data, 1, 29);
                return HexUtil.encodeHexString(hash);
            }
            // If 28 bytes, use directly
            return HexUtil.encodeHexString(data);
        } catch (Exception e) {
            log.debug("Could not bech32-decode DRep ID '{}', returning as-is", drepIdOrHex);
            return drepIdOrHex;
        }
    }

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

    /** Returns map of status→epoch for a given proposal using governance-aggr table. */
    private Map<String, Integer> fetchProposalStatusEpochs(String txHash, int idx) {
        Map<String, Integer> result = new HashMap<>();
        try {
            // Use gov_action_proposal_status from governance-aggr (has RATIFIED/EXPIRED rows)
            dsl.select(GOV_ACTION_PROPOSAL_STATUS.STATUS, GOV_ACTION_PROPOSAL_STATUS.EPOCH)
                    .from(GOV_ACTION_PROPOSAL_STATUS)
                    .where(GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_TX_HASH.eq(txHash))
                    .and(GOV_ACTION_PROPOSAL_STATUS.GOV_ACTION_INDEX.eq(idx))
                    .and(GOV_ACTION_PROPOSAL_STATUS.STATUS.in("RATIFIED", "EXPIRED"))
                    .fetch()
                    .forEach(r -> {
                        String status = r.get(GOV_ACTION_PROPOSAL_STATUS.STATUS);
                        Integer epoch = r.get(GOV_ACTION_PROPOSAL_STATUS.EPOCH);
                        // Keep the first (earliest) epoch for each status
                        result.putIfAbsent(status, epoch);
                    });
        } catch (Exception e) {
            log.debug("Could not fetch proposal status epochs for {}/{}: {}", txHash, idx, e.getMessage());
        }
        return result;
    }

    private BFProposal toProposalRow(org.jooq.Record r, int govActionLifetime) {
        String txHash = r.get(GOV_ACTION_PROPOSAL.TX_HASH);
        int idx = r.get(GOV_ACTION_PROPOSAL.IDX);
        Integer proposalEpoch = r.get(GOV_ACTION_PROPOSAL.EPOCH);
        Map<String, Integer> statusEpochs = fetchProposalStatusEpochs(txHash, idx);
        return BFProposal.builder()
                .txHash(txHash)
                .certIndex(idx)
                .type(r.get(GOV_ACTION_PROPOSAL.TYPE))
                .details(jsonNodeFromJSON(r.get(GOV_ACTION_PROPOSAL.DETAILS)))
                .deposit(r.get(GOV_ACTION_PROPOSAL.DEPOSIT))
                .returnAddress(r.get(GOV_ACTION_PROPOSAL.RETURN_ADDRESS))
                .anchorUrl(r.get(GOV_ACTION_PROPOSAL.ANCHOR_URL))
                .anchorHash(r.get(GOV_ACTION_PROPOSAL.ANCHOR_HASH))
                .epoch(proposalEpoch)
                .ratifiedEpoch(statusEpochs.get("RATIFIED"))
                .expiredEpoch(statusEpochs.get("EXPIRED"))
                .govActionLifetime(govActionLifetime)
                .build();
    }

    private DRepRegistration toDRepRegistrationDomain(org.jooq.Record r) {
        // Map the String type from DB to CertificateType enum
        String typeStr = r.get(DREP_REGISTRATION.TYPE);
        com.bloxbean.cardano.yaci.core.model.certs.CertificateType certType = null;
        if (typeStr != null) {
            try {
                certType = com.bloxbean.cardano.yaci.core.model.certs.CertificateType.valueOf(typeStr);
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
                .slot(r.get(DREP_REGISTRATION.SLOT))
                .epoch(r.get(DREP_REGISTRATION.EPOCH))
                .build();
    }

    private VotingProcedure toVotingProcedureDomain(org.jooq.Record r) {
        String voterTypeStr = r.get(VOTING_PROCEDURE.VOTER_TYPE);
        com.bloxbean.cardano.yaci.core.model.governance.VoterType voterType = null;
        if (voterTypeStr != null) {
            try { voterType = com.bloxbean.cardano.yaci.core.model.governance.VoterType.valueOf(voterTypeStr); }
            catch (IllegalArgumentException ignored) {}
        }
        String voteStr = r.get(VOTING_PROCEDURE.VOTE);
        com.bloxbean.cardano.yaci.core.model.governance.Vote vote = null;
        if (voteStr != null) {
            try { vote = com.bloxbean.cardano.yaci.core.model.governance.Vote.valueOf(voteStr); }
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
    public List<DRepEntity> findAllDReps(int page, int count, Order order) {
        SortField<?> outerSort = order == Order.desc ? DREP.SLOT.desc() : DREP.SLOT.asc();

        // Sub-select: latest row per drep_hash using window function
        var ranked = dsl.select(
                        DREP.DREP_HASH,
                        DREP.DREP_ID,
                        DREP.SLOT,
                        DREP.EPOCH,
                        DSL.rowNumber()
                                .over(DSL.partitionBy(DREP.DREP_HASH).orderBy(DREP.SLOT.desc()))
                                .as("rn")
                )
                .from(DREP)
                .where(DREP.DREP_ID.isNotNull())
                .asTable("ranked");

        return dsl.select(
                        ranked.field("drep_hash", String.class),
                        ranked.field("drep_id", String.class),
                        ranked.field("slot", Long.class),
                        ranked.field("epoch", Integer.class)
                )
                .from(ranked)
                .where(ranked.field("rn", Integer.class).eq(1))
                .orderBy(ranked.field("slot", Long.class).sort(order == Order.desc
                        ? org.jooq.SortOrder.DESC : org.jooq.SortOrder.ASC))
                .limit(count)
                .offset(offset(page, count))
                .fetch()
                .map(r -> {
                    DRepEntity entity = new DRepEntity();
                    entity.setDrepHash(r.get("drep_hash", String.class));
                    entity.setDrepId(r.get("drep_id", String.class));
                    entity.setSlot(r.get("slot", Long.class));
                    entity.setEpoch(r.get("epoch", Integer.class));
                    return entity;
                });
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
                .and(DREP_REGISTRATION.TYPE.eq("REG_DREP_CERT"))
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
        if (BlockfrostDialectUtil.isPostgres(dsl)) {
            // Subquery: latest delegation row per address using window function
            var latestDelegation = dsl.select(
                            DELEGATION_VOTE.ADDRESS,
                            DELEGATION_VOTE.DREP_HASH,
                            DSL.rowNumber()
                                    .over(DSL.partitionBy(DELEGATION_VOTE.ADDRESS).orderBy(DELEGATION_VOTE.SLOT.desc()))
                                    .as("rn"),
                            DELEGATION_VOTE.SLOT.as("max_slot")
                    )
                    .from(DELEGATION_VOTE)
                    .asTable("latest_del");

            return dsl.select(
                            latestDelegation.field("address", String.class).as("address"),
                            DSL.coalesce(
                                    DSL.sum(ADDRESS_UTXO.LOVELACE_AMOUNT).cast(Long.class),
                                    DSL.inline(0L)
                            ).as("amount")
                    )
                    .from(latestDelegation)
                    .leftJoin(ADDRESS_UTXO)
                    .on(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(latestDelegation.field("address", String.class)))
                    .and(DSL.notExists(
                            dsl.selectOne()
                                    .from(TX_INPUT)
                                    .where(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                                    .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
                    ))
                    .where(latestDelegation.field("rn", Integer.class).eq(1))
                    .and(latestDelegation.field("drep_hash", String.class).eq(drepHex))
                    .groupBy(latestDelegation.field("address", String.class), latestDelegation.field("max_slot", Long.class))
                    .orderBy(latestDelegation.field("max_slot", Long.class).sort(order == Order.desc ? SortOrder.DESC : SortOrder.ASC))
                    .limit(count)
                    .offset(offset(page, count))
                    .fetch()
                    .map(r -> BFDRepDelegator.builder()
                            .address(r.get("address", String.class))
                            .amount(r.get("amount", Long.class))
                            .build());
        } else {
            // SQLite/H2 fallback: fetch addresses whose latest delegation is to this DRep
            List<String> addresses = dsl.select(DELEGATION_VOTE.ADDRESS)
                    .from(DELEGATION_VOTE)
                    .where(DELEGATION_VOTE.DREP_HASH.eq(drepHex))
                    .and(DSL.notExists(
                            dsl.selectOne()
                                    .from(DELEGATION_VOTE.as("newer"))
                                    .where(DELEGATION_VOTE.as("newer").field(DELEGATION_VOTE.ADDRESS).eq(DELEGATION_VOTE.ADDRESS))
                                    .and(DELEGATION_VOTE.as("newer").field(DELEGATION_VOTE.SLOT).gt(DELEGATION_VOTE.SLOT))
                    ))
                    .orderBy(order == Order.desc ? DELEGATION_VOTE.SLOT.desc() : DELEGATION_VOTE.SLOT.asc())
                    .limit(count)
                    .offset(offset(page, count))
                    .fetch(DELEGATION_VOTE.ADDRESS);
            List<BFDRepDelegator> result = new ArrayList<>();
            for (String address : addresses) {
                Long sum = dsl.select(DSL.coalesce(
                                DSL.sum(ADDRESS_UTXO.LOVELACE_AMOUNT).cast(Long.class),
                                DSL.inline(0L)
                        ).as("total"))
                        .from(ADDRESS_UTXO)
                        .where(ADDRESS_UTXO.OWNER_STAKE_ADDR.eq(address))
                        .and(DSL.notExists(
                                dsl.selectOne()
                                        .from(TX_INPUT)
                                        .where(TX_INPUT.TX_HASH.eq(ADDRESS_UTXO.TX_HASH))
                                        .and(TX_INPUT.OUTPUT_INDEX.eq(ADDRESS_UTXO.OUTPUT_INDEX))
                        ))
                        .fetchOne(0, Long.class);
                result.add(BFDRepDelegator.builder()
                        .address(address)
                        .amount(sum != null ? sum : 0L)
                        .build());
            }
            return result;
        }
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
        return dsl.select()
                .from(GOV_ACTION_PROPOSAL)
                .orderBy(sortField)
                .limit(count)
                .offset(offset(page, count))
                .fetch()
                .map(r -> toProposalRow(r, govActionLifetime));
    }

    @Override
    public Optional<BFProposal> findProposalByTxHashAndIndex(String txHash, int certIndex) {
        int govActionLifetime = fetchGovActionLifetime();
        return dsl.select()
                .from(GOV_ACTION_PROPOSAL)
                .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                .and(GOV_ACTION_PROPOSAL.IDX.eq(certIndex))
                .fetchOptional()
                .map(r -> toProposalRow(r, govActionLifetime));
    }

    @Override
    public Optional<BFProposal> findParameterChangeProposal(String txHash, int certIndex) {
        int govActionLifetime = fetchGovActionLifetime();
        return dsl.select()
                .from(GOV_ACTION_PROPOSAL)
                .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                .and(GOV_ACTION_PROPOSAL.IDX.eq(certIndex))
                .and(GOV_ACTION_PROPOSAL.TYPE.eq("PARAMETER_CHANGE_ACTION"))
                .fetchOptional()
                .map(r -> toProposalRow(r, govActionLifetime));
    }

    @Override
    public boolean isWithdrawalProposal(String txHash, int certIndex) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(GOV_ACTION_PROPOSAL)
                        .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                        .and(GOV_ACTION_PROPOSAL.IDX.eq(certIndex))
                        .and(GOV_ACTION_PROPOSAL.TYPE.eq("TREASURY_WITHDRAWALS_ACTION"))
        );
    }

    @Override
    public List<BFDRepDelegator> findProposalWithdrawals(String txHash, int certIndex) {
        // Read withdrawals from gov_action_proposal.details JSON — no adapot dependency
        return dsl.select(GOV_ACTION_PROPOSAL.DETAILS)
                .from(GOV_ACTION_PROPOSAL)
                .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                .and(GOV_ACTION_PROPOSAL.IDX.eq(certIndex))
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
                        log.warn("Could not parse withdrawals from proposal details for {}/{}: {}", txHash, certIndex, e.getMessage());
                    }
                    return result;
                })
                .orElse(new ArrayList<>());
    }

    @Override
    public List<VotingProcedure> findProposalVotes(String txHash, int certIndex, int page, int count, Order order) {
        SortField<?> sortField = order == Order.desc
                ? VOTING_PROCEDURE.SLOT.desc()
                : VOTING_PROCEDURE.SLOT.asc();
        return dsl.select()
                .from(VOTING_PROCEDURE)
                .where(VOTING_PROCEDURE.GOV_ACTION_TX_HASH.eq(txHash))
                .and(VOTING_PROCEDURE.GOV_ACTION_INDEX.eq(certIndex))
                .orderBy(sortField, VOTING_PROCEDURE.IDX.asc())
                .limit(count)
                .offset(offset(page, count))
                .fetch()
                .map(this::toVotingProcedureDomain);
    }

    @Override
    public Optional<BFProposal> findProposalMetadata(String txHash, int certIndex) {
        int govActionLifetime = fetchGovActionLifetime();
        return dsl.select()
                .from(GOV_ACTION_PROPOSAL)
                .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                .and(GOV_ACTION_PROPOSAL.IDX.eq(certIndex))
                .and(GOV_ACTION_PROPOSAL.ANCHOR_URL.isNotNull())
                .fetchOptional()
                .map(r -> toProposalRow(r, govActionLifetime));
    }
}
