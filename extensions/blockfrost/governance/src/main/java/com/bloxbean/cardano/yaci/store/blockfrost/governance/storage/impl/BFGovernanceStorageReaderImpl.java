package com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl;

import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.util.HexUtil;
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
import org.jooq.Select;
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

import static com.bloxbean.cardano.yaci.store.account.jooq.Tables.STAKE_ADDRESS_BALANCE;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.ADAPOT_JOBS;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.EPOCH_STAKE;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.INSTANT_REWARD;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD_REST;
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

            // Compute enacted_epoch: if RATIFIED at epoch X and current epoch > X+1, proposal is enacted
            // Treasury withdrawals are distributed in the epoch after ratification (X+1)
            Integer ratifiedEpoch = result.get("RATIFIED");
            if (ratifiedEpoch != null) {
                // Get current epoch from max(block.epoch)
                Integer currentEpoch = getCurrentEpochFromBlock();
                if (currentEpoch != null && currentEpoch > ratifiedEpoch) {
                    result.put("ENACTED", ratifiedEpoch + 1);
                }
            }
        } catch (Exception e) {
            log.warn("Could not fetch proposal status epochs for {}/{}: {}", txHash, idx, e.getMessage());
        }
        return result;
    }

    /** Get current epoch from the block table. */
    private Integer getCurrentEpochFromBlock() {
        try {
            var result = dsl.resultQuery("SELECT MAX(epoch) FROM yaci_store.block").fetchOne();
            return result != null ? result.get(0, Integer.class) : null;
        } catch (Exception e) {
            log.warn("Could not get current epoch: {}", e.getMessage());
            return null;
        }
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
                .enactedEpoch(statusEpochs.get("ENACTED"))
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
        Integer snapshotEpoch = findLatestStakeSnapshotEpoch();
        var delegators = buildDRepDelegatorsQuery(drepHex, page, count, order, snapshotEpoch).fetch();
        if (delegators.isEmpty()) {
            return List.of();
        }

        List<String> checkpointAddresses = delegators.stream()
                .filter(r -> r.get("checkpoint_amount", Long.class) != null)
                .map(r -> r.get("address", String.class))
                .toList();
        List<String> addressesWithoutCheckpoint = delegators.stream()
                .filter(r -> r.get("checkpoint_amount", Long.class) == null)
                .map(r -> r.get("address", String.class))
                .toList();

        Map<String, Long> rewardChanges = new HashMap<>();
        fetchRewardChanges(rewardChanges, checkpointAddresses, snapshotEpoch);
        fetchRewardChanges(rewardChanges, addressesWithoutCheckpoint, null);

        return delegators
                .map(r -> {
                    String address = r.get("address", String.class);
                    long currentBalance = r.get("amount", Long.class);
                    Long checkpointAmount = r.get("checkpoint_amount", Long.class);
                    long snapshotBalance = Optional.ofNullable(r.get("snapshot_balance", Long.class)).orElse(0L);
                    // epoch_stake stores controlled amount. Subtract its UTxO component to recover
                    // the reward balance at the checkpoint, then apply only later reward changes.
                    long checkpointReward = checkpointAmount == null ? 0L : checkpointAmount - snapshotBalance;
                    long withdrawableReward = Math.max(
                            checkpointReward + rewardChanges.getOrDefault(address, 0L),
                            0L
                    );
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
        var pagedDelegators = dsl.select(
                        DELEGATION_VOTE.ADDRESS.as("address"),
                        DELEGATION_VOTE.SLOT.as("max_slot"),
                        DELEGATION_VOTE.TX_INDEX.as("max_tx_index"),
                        DELEGATION_VOTE.CERT_INDEX.as("max_cert_index")
                )
                .from(DELEGATION_VOTE)
                .where(DELEGATION_VOTE.DREP_HASH.eq(drepHex))
                .and(hasActiveRegistrationForDelegation)
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
        // First check if anchor_url exists - if not, return empty (404)
        var hasAnchor = dsl.selectCount()
                .from(GOV_ACTION_PROPOSAL)
                .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                .and(GOV_ACTION_PROPOSAL.IDX.eq(certIndex))
                .and(GOV_ACTION_PROPOSAL.ANCHOR_URL.isNotNull())
                .fetchOne(0, int.class) > 0;

        if (!hasAnchor) {
            return Optional.empty();
        }

        return dsl.select()
                .from(GOV_ACTION_PROPOSAL)
                .where(GOV_ACTION_PROPOSAL.TX_HASH.eq(txHash))
                .and(GOV_ACTION_PROPOSAL.IDX.eq(certIndex))
                .and(GOV_ACTION_PROPOSAL.ANCHOR_URL.isNotNull())
                .fetchOptional()
                .map(r -> toProposalRow(r, govActionLifetime));
    }
}
