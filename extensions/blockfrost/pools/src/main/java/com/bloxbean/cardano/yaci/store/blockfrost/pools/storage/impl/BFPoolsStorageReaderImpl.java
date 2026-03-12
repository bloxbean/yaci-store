package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.common.util.BlockfrostDialectUtil;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.dto.BFPoolDelegatorDto;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.dto.BFPoolHistoryDto;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.dto.BFPoolRelayDto;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.BFPoolsStorageReader;
import com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl.model.*;
import com.bloxbean.cardano.yaci.store.common.util.PoolUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SortField;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.*;

import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.blocks.jooq.Tables.BLOCK;
import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.VOTING_PROCEDURE;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.EPOCH_STAKE;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD;
import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BFPoolsStorageReaderImpl implements BFPoolsStorageReader {
    private final DSLContext dsl;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> getPoolIds(int page, int count, String order) {
        int offset = page * count;
        SortField<?> orderBy = "desc".equals(order) ? min(POOL_REGISTRATION.SLOT).desc() : min(POOL_REGISTRATION.SLOT).asc();

        return dsl.select(POOL_REGISTRATION.POOL_ID)
                .from(POOL_REGISTRATION)
                .groupBy(POOL_REGISTRATION.POOL_ID)
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(record -> PoolUtil.getBech32PoolId(record.get(POOL_REGISTRATION.POOL_ID)));
    }

    @Override
    public List<BFPoolRetireItem> getRetiredPools(int page, int count, String order) {
        int offset = page * count;
        SortField<?> orderBy = "desc".equals(order) ? POOL.RETIRE_EPOCH.desc() : POOL.RETIRE_EPOCH.asc();

        return dsl.select(POOL.POOL_ID, POOL.RETIRE_EPOCH)
                .from(POOL)
                .where(POOL.STATUS.eq("RETIRED"))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(record -> new BFPoolRetireItem(
                        record.get(POOL.POOL_ID),
                        record.get(POOL.RETIRE_EPOCH)
                ));
    }

    @Override
    public List<BFPoolRetireItem> getRetiringPools(int page, int count, String order) {
        int offset = page * count;
        // Deduplicate: one pool may have multiple RETIRING rows; take max retire_epoch per pool_id.
        // Exclude pools that already have a RETIRED row.
        var maxRetireEpoch = max(POOL.RETIRE_EPOCH).as("max_retire_epoch");
        SortField<?> orderBy = "desc".equals(order)
                ? field(name("max_retire_epoch"), Integer.class).desc()
                : field(name("max_retire_epoch"), Integer.class).asc();

        var retiredPoolIds = dsl.select(POOL.POOL_ID)
                .from(POOL)
                .where(POOL.STATUS.eq("RETIRED"));

        return dsl.select(POOL.POOL_ID, maxRetireEpoch)
                .from(POOL)
                .where(POOL.STATUS.eq("RETIRING"))
                .and(POOL.POOL_ID.notIn(retiredPoolIds))
                .groupBy(POOL.POOL_ID)
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(record -> new BFPoolRetireItem(
                        record.get(POOL.POOL_ID),
                        record.get(maxRetireEpoch)
                ));
    }

    @Override
    public Optional<BFPoolSummary> getPoolDetail(String poolIdHex) {
        Record latestReg = dsl.select(
                        POOL_REGISTRATION.POOL_ID,
                        POOL_REGISTRATION.VRF_KEY,
                        POOL_REGISTRATION.PLEDGE,
                        POOL_REGISTRATION.COST,
                        POOL_REGISTRATION.MARGIN_NUMERATOR,
                        POOL_REGISTRATION.MARGIN_DENOMINATOR,
                        POOL_REGISTRATION.REWARD_ACCOUNT,
                        POOL_REGISTRATION.POOL_OWNERS
                )
                .from(POOL_REGISTRATION)
                .where(POOL_REGISTRATION.POOL_ID.eq(poolIdHex))
                .orderBy(POOL_REGISTRATION.SLOT.desc())
                .limit(1)
                .fetchOne();

        if (latestReg == null) {
            return Optional.empty();
        }

        String vrfKey = latestReg.get(POOL_REGISTRATION.VRF_KEY);

        // Fix: PostgreSQL requires ORDER BY columns to appear in SELECT for DISTINCT.
        // Use GROUP BY tx_hash ORDER BY min(slot) instead.
        List<String> registrationTxHashes = dsl.select(POOL_REGISTRATION.TX_HASH)
                .from(POOL_REGISTRATION)
                .where(POOL_REGISTRATION.POOL_ID.eq(poolIdHex))
                .groupBy(POOL_REGISTRATION.TX_HASH)
                .orderBy(min(POOL_REGISTRATION.SLOT).asc())
                .fetch(POOL_REGISTRATION.TX_HASH);

        List<String> retirementTxHashes = dsl.select(POOL_RETIREMENT.TX_HASH)
                .from(POOL_RETIREMENT)
                .where(POOL_RETIREMENT.POOL_ID.eq(poolIdHex))
                .groupBy(POOL_RETIREMENT.TX_HASH)
                .orderBy(min(POOL_RETIREMENT.SLOT).asc())
                .fetch(POOL_RETIREMENT.TX_HASH);

        // Fix: block.slot_leader stores the 56-char pool_id hex, not the 64-char vrf_key
        int blocksMinted = dsl.selectCount()
                .from(BLOCK)
                .where(BLOCK.SLOT_LEADER.eq(poolIdHex))
                .fetchOne(0, int.class);

        Integer maxEpoch = dsl.select(max(BLOCK.EPOCH)).from(BLOCK).fetchOne(0, Integer.class);
        int blocksEpoch = 0;
        if (maxEpoch != null) {
            blocksEpoch = dsl.selectCount()
                    .from(BLOCK)
                    .where(BLOCK.SLOT_LEADER.eq(poolIdHex))
                    .and(BLOCK.EPOCH.eq(maxEpoch))
                    .fetchOne(0, int.class);
        }

        return Optional.of(new BFPoolSummary(
                poolIdHex,
                vrfKey,
                toBigInteger(latestReg.get(POOL_REGISTRATION.PLEDGE)),
                toBigInteger(latestReg.get(POOL_REGISTRATION.COST)),
                toBigInteger(latestReg.get(POOL_REGISTRATION.MARGIN_NUMERATOR)),
                toBigInteger(latestReg.get(POOL_REGISTRATION.MARGIN_DENOMINATOR)),
                latestReg.get(POOL_REGISTRATION.REWARD_ACCOUNT),
                parsePoolOwners(latestReg.get(POOL_REGISTRATION.POOL_OWNERS)),
                blocksMinted,
                blocksEpoch,
                registrationTxHashes,
                retirementTxHashes
        ));
    }

    @Override
    public List<BFPoolUpdate> getPoolUpdates(String poolIdHex, int page, int count, String order) {
        int offset = page * count;
        SortField<?> slotOrder = "desc".equals(order) ? field("slot", Long.class).desc() : field("slot", Long.class).asc();
        SortField<?> certOrder = "desc".equals(order) ? field("cert_index", Integer.class).desc() : field("cert_index", Integer.class).asc();

        var registrations = dsl.select(
                        POOL_REGISTRATION.TX_HASH.as("tx_hash"),
                        POOL_REGISTRATION.CERT_INDEX.as("cert_index"),
                        POOL_REGISTRATION.SLOT.as("slot"),
                        val("registered").as("action")
                )
                .from(POOL_REGISTRATION)
                .where(POOL_REGISTRATION.POOL_ID.eq(poolIdHex));

        var retirements = dsl.select(
                        POOL_RETIREMENT.TX_HASH.as("tx_hash"),
                        POOL_RETIREMENT.CERT_INDEX.as("cert_index"),
                        POOL_RETIREMENT.SLOT.as("slot"),
                        val("deregistered").as("action")
                )
                .from(POOL_RETIREMENT)
                .where(POOL_RETIREMENT.POOL_ID.eq(poolIdHex));

        return dsl.select()
                .from(registrations.unionAll(retirements))
                .orderBy(slotOrder, certOrder)
                .limit(count)
                .offset(offset)
                .fetch(record -> new BFPoolUpdate(
                        record.get("tx_hash", String.class),
                        record.get("cert_index", Integer.class),
                        record.get("action", String.class)
                ));
    }

    @Override
    public Optional<BFPoolMetadata> getPoolMetadata(String poolIdHex) {
        Record record = dsl.select(
                        POOL_REGISTRATION.POOL_ID,
                        POOL_REGISTRATION.METADATA_URL,
                        POOL_REGISTRATION.METADATA_HASH
                )
                .from(POOL_REGISTRATION)
                .where(POOL_REGISTRATION.POOL_ID.eq(poolIdHex))
                .orderBy(POOL_REGISTRATION.SLOT.desc())
                .limit(1)
                .fetchOne();

        if (record == null) {
            return Optional.empty();
        }

        String metadataUrl = record.get(POOL_REGISTRATION.METADATA_URL) != null
                ? record.get(POOL_REGISTRATION.METADATA_URL).toString()
                : null;

        return Optional.of(new BFPoolMetadata(
                poolIdHex,
                metadataUrl,
                record.get(POOL_REGISTRATION.METADATA_HASH)
        ));
    }

    @Override
    public List<BFPoolRelayDto> getPoolRelays(String poolIdHex) {
        Record record = dsl.select(POOL_REGISTRATION.RELAYS)
                .from(POOL_REGISTRATION)
                .where(POOL_REGISTRATION.POOL_ID.eq(poolIdHex))
                .orderBy(POOL_REGISTRATION.SLOT.desc())
                .limit(1)
                .fetchOne();

        if (record == null) {
            return Collections.emptyList();
        }

        Object relaysObj = record.get(POOL_REGISTRATION.RELAYS);
        if (relaysObj == null) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, Object>> relays = objectMapper.readValue(relaysObj.toString(), new TypeReference<>() {});
            List<BFPoolRelayDto> result = new ArrayList<>();
            for (Map<String, Object> relay : relays) {
                result.add(BFPoolRelayDto.builder()
                        .ipv4(getStringField(relay, "ipv4"))
                        .ipv6(getStringField(relay, "ipv6"))
                        .dns(getStringField(relay, "dnsName"))
                        .dnsSrv(getStringField(relay, "dnsSrvName"))
                        .port(getIntField(relay, "port"))
                        .build());
            }
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse relays JSON for pool {}: {}", poolIdHex, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<String> getVrfKeyByPoolId(String poolIdHex) {
        return dsl.select(POOL_REGISTRATION.VRF_KEY)
                .from(POOL_REGISTRATION)
                .where(POOL_REGISTRATION.POOL_ID.eq(poolIdHex))
                .orderBy(POOL_REGISTRATION.SLOT.desc())
                .limit(1)
                .fetchOptional(POOL_REGISTRATION.VRF_KEY);
    }

    @Override
    public List<String> getPoolBlockHashes(String poolIdHex, int page, int count, String order) {
        int offset = page * count;
        SortField<?> orderBy = "desc".equals(order) ? BLOCK.NUMBER.desc() : BLOCK.NUMBER.asc();

        // block.slot_leader stores the 56-char pool_id hex (same as pool_registration.pool_id)
        return dsl.select(BLOCK.HASH)
                .from(BLOCK)
                .where(BLOCK.SLOT_LEADER.eq(poolIdHex))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(BLOCK.HASH);
    }

    @Override
    public List<BFPoolVote> getPoolVotes(String poolIdHex, int page, int count, String order) {
        int offset = page * count;
        SortField<?> slotOrder = "desc".equals(order) ? VOTING_PROCEDURE.SLOT.desc() : VOTING_PROCEDURE.SLOT.asc();
        SortField<?> idxOrder = "desc".equals(order) ? VOTING_PROCEDURE.IDX.desc() : VOTING_PROCEDURE.IDX.asc();

        return dsl.select(
                        VOTING_PROCEDURE.TX_HASH,
                        VOTING_PROCEDURE.IDX,
                        VOTING_PROCEDURE.VOTE
                )
                .from(VOTING_PROCEDURE)
                .where(VOTING_PROCEDURE.VOTER_TYPE.eq("STAKING_POOL_KEY_HASH"))
                .and(VOTING_PROCEDURE.VOTER_HASH.eq(poolIdHex))
                .orderBy(slotOrder, idxOrder)
                .limit(count)
                .offset(offset)
                .fetch(record -> new BFPoolVote(
                        record.get(VOTING_PROCEDURE.TX_HASH),
                        record.get(VOTING_PROCEDURE.IDX),
                        record.get(VOTING_PROCEDURE.VOTE)
                ));
    }

    @Override
    public List<BFPoolRegistrationInfo> getExtendedPools(int page, int count, String order) {
        int offset = page * count;

        var latestSlot = dsl.select(
                        POOL_REGISTRATION.POOL_ID,
                        max(POOL_REGISTRATION.SLOT).as("max_slot")
                )
                .from(POOL_REGISTRATION)
                .groupBy(POOL_REGISTRATION.POOL_ID)
                .asTable("latest_slot");

        var blockCounts = dsl.select(
                        BLOCK.SLOT_LEADER,
                        count().as("blocks_minted")
                )
                .from(BLOCK)
                .groupBy(BLOCK.SLOT_LEADER)
                .asTable("block_counts");

        SortField<?> orderBy = "desc".equals(order) ?
                field(name("latest_slot", "max_slot"), Long.class).desc() :
                field(name("latest_slot", "max_slot"), Long.class).asc();

        return dsl.select(
                        POOL_REGISTRATION.POOL_ID,
                        POOL_REGISTRATION.VRF_KEY,
                        POOL_REGISTRATION.PLEDGE,
                        POOL_REGISTRATION.COST,
                        POOL_REGISTRATION.MARGIN_NUMERATOR,
                        POOL_REGISTRATION.MARGIN_DENOMINATOR,
                        POOL_REGISTRATION.METADATA_URL,
                        POOL_REGISTRATION.METADATA_HASH,
                        coalesce(field(name("block_counts", "blocks_minted"), Integer.class), val(0)).as("blocks_minted")
                )
                .from(POOL_REGISTRATION)
                .innerJoin(latestSlot)
                .on(POOL_REGISTRATION.POOL_ID.eq(field(name("latest_slot", "pool_id"), String.class)))
                .and(POOL_REGISTRATION.SLOT.eq(field(name("latest_slot", "max_slot"), Long.class)))
                // Fix: block.slot_leader = pool_id hex, not vrf_key
                .leftJoin(blockCounts)
                .on(POOL_REGISTRATION.POOL_ID.eq(field(name("block_counts", "slot_leader"), String.class)))
                .orderBy(orderBy)
                .limit(count)
                .offset(offset)
                .fetch(record -> {
                    String metadataUrl = record.get(POOL_REGISTRATION.METADATA_URL) != null
                            ? record.get(POOL_REGISTRATION.METADATA_URL).toString()
                            : null;
                    return new BFPoolRegistrationInfo(
                            record.get(POOL_REGISTRATION.POOL_ID),
                            record.get(POOL_REGISTRATION.VRF_KEY),
                            toBigInteger(record.get(POOL_REGISTRATION.PLEDGE)),
                            toBigInteger(record.get(POOL_REGISTRATION.COST)),
                            toBigInteger(record.get(POOL_REGISTRATION.MARGIN_NUMERATOR)),
                            toBigInteger(record.get(POOL_REGISTRATION.MARGIN_DENOMINATOR)),
                            metadataUrl,
                            record.get(POOL_REGISTRATION.METADATA_HASH),
                            record.get("blocks_minted", Integer.class)
                    );
                });
    }

    // -------------------------------------------------------------------------
    // T005: getPoolHistoryBase — block table only (adapot OFF path)
    // -------------------------------------------------------------------------

    @Override
    public List<BFPoolHistoryDto> getPoolHistoryBase(String poolIdHex, int page, int count, String order) {
        boolean asc = !"desc".equalsIgnoreCase(order);
        int offset = page * count;

        return dsl.select(
                        BLOCK.EPOCH,
                        count().as("blocks"),
                        coalesce(sum(BLOCK.TOTAL_FEES), val(0L)).as("fees")
                )
                .from(BLOCK)
                .where(BLOCK.SLOT_LEADER.eq(poolIdHex))
                .groupBy(BLOCK.EPOCH)
                .orderBy(asc ? BLOCK.EPOCH.asc() : BLOCK.EPOCH.desc())
                .limit(count)
                .offset(offset)
                .fetch()
                .stream()
                .map(r -> BFPoolHistoryDto.builder()
                        .epoch(r.get(BLOCK.EPOCH))
                        .blocks(r.get("blocks", Integer.class))
                        .fees(String.valueOf(r.get("fees", Long.class)))
                        .build()) // activeStake, activeSize, delegatorsCount, rewards default to null
                .toList();
    }

    // -------------------------------------------------------------------------
    // T006: getPoolHistoryFull — epoch_stake + reward + block (adapot ON path)
    // -------------------------------------------------------------------------

    @Override
    public List<BFPoolHistoryDto> getPoolHistoryFull(String poolIdHex, int page, int count, String order) {
        boolean asc = !"desc".equalsIgnoreCase(order);
        int offset = page * count;

        // Cardano epoch offset: epoch_stake at epoch E records the snapshot taken at
        // the boundary of epoch E. This stake becomes *active* at epoch E+2.
        // Blockfrost's /history shows the "active epoch" — the epoch where the stake
        // was actually used for block production and reward calculation.
        var activeEpoch = EPOCH_STAKE.EPOCH.plus(2).as("active_epoch");

        // Subquery: total epoch stake per active epoch (denominator for active_size)
        var totalStakeSub = dsl.select(
                        EPOCH_STAKE.EPOCH.plus(2).as("ts_epoch"),
                        sum(EPOCH_STAKE.AMOUNT).as("total_stake")
                )
                .from(EPOCH_STAKE)
                .groupBy(EPOCH_STAKE.EPOCH)
                .asTable("total_stake_sub");

        // Subquery: total rewards + leader-only rewards per earned_epoch for this pool
        var rewardSub = dsl.select(
                        REWARD.EARNED_EPOCH.as("r_epoch"),
                        sum(REWARD.AMOUNT).as("rewards"),
                        sum(when(REWARD.TYPE.eq("leader"), REWARD.AMOUNT).otherwise(val(BigInteger.ZERO))).as("leader_rewards")
                )
                .from(REWARD)
                .where(REWARD.POOL_ID.eq(poolIdHex))
                .groupBy(REWARD.EARNED_EPOCH)
                .asTable("reward_sub");

        // Subquery: blocks and fees per epoch for this pool
        var blockSub = dsl.select(
                        BLOCK.EPOCH.as("b_epoch"),
                        count().as("blocks"),
                        coalesce(sum(BLOCK.TOTAL_FEES), val(0L)).as("fees")
                )
                .from(BLOCK)
                .where(BLOCK.SLOT_LEADER.eq(poolIdHex))
                .groupBy(BLOCK.EPOCH)
                .asTable("block_sub");

        return dsl.select(
                        activeEpoch,
                        sum(EPOCH_STAKE.AMOUNT).as("active_stake"),
                        count(EPOCH_STAKE.ADDRESS).as("delegators_count"),
                        field(name("total_stake_sub", "total_stake")),
                        field(name("reward_sub", "rewards")),
                        field(name("reward_sub", "leader_rewards")),
                        field(name("block_sub", "blocks")),
                        field(name("block_sub", "fees"))
                )
                .from(EPOCH_STAKE)
                .leftJoin(totalStakeSub)
                    .on(field(name("total_stake_sub", "ts_epoch"), Integer.class).eq(EPOCH_STAKE.EPOCH.plus(2)))
                .leftJoin(rewardSub)
                    .on(field(name("reward_sub", "r_epoch"), Integer.class).eq(EPOCH_STAKE.EPOCH.plus(2)))
                .leftJoin(blockSub)
                    .on(field(name("block_sub", "b_epoch"), Integer.class).eq(EPOCH_STAKE.EPOCH.plus(2)))
                .where(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                .groupBy(
                        EPOCH_STAKE.EPOCH,
                        field(name("total_stake_sub", "total_stake")),
                        field(name("reward_sub", "rewards")),
                        field(name("reward_sub", "leader_rewards")),
                        field(name("block_sub", "blocks")),
                        field(name("block_sub", "fees"))
                )
                .orderBy(asc ? EPOCH_STAKE.EPOCH.asc() : EPOCH_STAKE.EPOCH.desc())
                .limit(count)
                .offset(offset)
                .fetch()
                .stream()
                .map(r -> {
                    BigInteger activeStake = r.get("active_stake", BigInteger.class);
                    BigInteger totalStake  = r.get("total_stake",  BigInteger.class);
                    BigInteger rewards     = r.get("rewards",      BigInteger.class);
                    BigInteger leaderRew   = r.get("leader_rewards", BigInteger.class);
                    Double activeSize = (activeStake != null && totalStake != null
                            && totalStake.compareTo(BigInteger.ZERO) > 0)
                            ? activeStake.doubleValue() / totalStake.doubleValue()
                            : null;
                    return BFPoolHistoryDto.builder()
                            .epoch(r.get("active_epoch", Integer.class))
                            .blocks(r.get("blocks", Integer.class) != null ? r.get("blocks", Integer.class) : 0)
                            .fees(leaderRew != null ? leaderRew.toString() : "0")
                            .activeStake(activeStake != null ? activeStake.toString() : null)
                            .activeSize(activeSize)
                            .delegatorsCount(r.get("delegators_count", Integer.class))
                            .rewards(rewards != null ? rewards.toString() : null)
                            .build();
                })
                .toList();
    }

    // -------------------------------------------------------------------------
    // T011: getPoolDelegatorsBase — delegation table, latest cert per address
    // -------------------------------------------------------------------------

    @Override
    public List<BFPoolDelegatorDto> getPoolDelegatorsBase(String poolIdHex, int page, int count, String order) {
        boolean asc = !"desc".equalsIgnoreCase(order);
        int offset = page * count;

        List<String> addresses;
        if (BlockfrostDialectUtil.isPostgres(dsl)) {
            // PostgreSQL DISTINCT ON: one pass to find latest delegation cert per address,
            // then filter to those still pointing to this pool.
            var latestDel = dsl
                    .select(
                            field("DISTINCT ON (address) address", String.class).as("address"),
                            DELEGATION.POOL_ID
                    )
                    .from(DELEGATION)
                    .orderBy(DELEGATION.ADDRESS, DELEGATION.SLOT.desc(), DELEGATION.CERT_INDEX.desc())
                    .asTable("latest_del");

            addresses = dsl.select(field(name("latest_del", "address"), String.class))
                    .from(latestDel)
                    .where(field(name("latest_del", "pool_id"), String.class).eq(poolIdHex))
                    .orderBy(asc
                            ? field(name("latest_del", "address"), String.class).asc()
                            : field(name("latest_del", "address"), String.class).desc())
                    .limit(count).offset(offset)
                    .fetch(0, String.class);
        } else {
            // MySQL / H2: ROW_NUMBER() window function equivalent
            var ranked = dsl.select(
                            DELEGATION.ADDRESS,
                            DELEGATION.POOL_ID,
                            rowNumber().over(
                                    partitionBy(DELEGATION.ADDRESS)
                                            .orderBy(DELEGATION.SLOT.desc(), DELEGATION.CERT_INDEX.desc())
                            ).as("rn")
                    )
                    .from(DELEGATION)
                    .asTable("ranked");

            addresses = dsl.select(field(name("ranked", "address"), String.class))
                    .from(ranked)
                    .where(field(name("ranked", "rn"), Integer.class).eq(1))
                    .and(field(name("ranked", "pool_id"), String.class).eq(poolIdHex))
                    .orderBy(asc
                            ? field(name("ranked", "address"), String.class).asc()
                            : field(name("ranked", "address"), String.class).desc())
                    .limit(count).offset(offset)
                    .fetch(0, String.class);
        }

        return addresses.stream()
                .map(addr -> BFPoolDelegatorDto.builder().address(addr).build()) // liveStake = null
                .toList();
    }

    // -------------------------------------------------------------------------
    // T012: getPoolDelegatorsFull — epoch_stake latest snapshot (adapot ON path)
    // -------------------------------------------------------------------------

    @Override
    public List<BFPoolDelegatorDto> getPoolDelegatorsFull(String poolIdHex, int page, int count, String order) {
        boolean asc = !"desc".equalsIgnoreCase(order);
        int offset = page * count;

        Integer latestEpoch = dsl.select(max(EPOCH_STAKE.EPOCH))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                .fetchOneInto(Integer.class);

        if (latestEpoch == null) return List.of();

        // Sort by amount (primary), then address (secondary for stable pagination)
        var amountOrder = asc ? EPOCH_STAKE.AMOUNT.asc().nullsLast() : EPOCH_STAKE.AMOUNT.desc().nullsLast();
        return dsl.select(EPOCH_STAKE.ADDRESS, EPOCH_STAKE.AMOUNT)
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.POOL_ID.eq(poolIdHex)
                        .and(EPOCH_STAKE.EPOCH.eq(latestEpoch)))
                .orderBy(amountOrder, EPOCH_STAKE.ADDRESS.asc())
                .limit(count).offset(offset)
                .fetch()
                .stream()
                .map(r -> BFPoolDelegatorDto.builder()
                        .address(r.get(EPOCH_STAKE.ADDRESS))
                        .liveStake(r.get(EPOCH_STAKE.AMOUNT) != null
                                ? r.get(EPOCH_STAKE.AMOUNT).toString()
                                : null)
                        .build())
                .toList();
    }

    // -------------------------------------------------------------------------
    // Stake info: live/active stake from epoch_stake (for pool detail & extended)
    // -------------------------------------------------------------------------

    @Override
    public Optional<BFPoolStakeInfo> getPoolStakeInfo(String poolIdHex) {
        // Find the latest epoch in epoch_stake (global, not per-pool, to ensure consistency)
        Integer latestEpoch = dsl.select(max(EPOCH_STAKE.EPOCH))
                .from(EPOCH_STAKE)
                .fetchOneInto(Integer.class);
        if (latestEpoch == null) return Optional.empty();

        int activeEpoch = Math.max(latestEpoch - 2, 0);

        // Pool aggregation at latest epoch (live)
        Record liveRec = dsl.select(
                        coalesce(sum(EPOCH_STAKE.AMOUNT), val(BigInteger.ZERO)).as("live_stake"),
                        count().as("live_delegators")
                )
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                .and(EPOCH_STAKE.EPOCH.eq(latestEpoch))
                .fetchOne();

        BigInteger liveStake = liveRec != null ? toBigInteger(liveRec.get("live_stake")) : BigInteger.ZERO;
        int liveDelegators = liveRec != null ? liveRec.get("live_delegators", Integer.class) : 0;

        // If pool has no stake at latest epoch, return empty (pool might not be active)
        if (liveDelegators == 0) return Optional.empty();

        // Pool aggregation at active epoch (latest - 2)
        BigInteger activeStake = dsl.select(coalesce(sum(EPOCH_STAKE.AMOUNT), val(BigInteger.ZERO)).as("active_stake"))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                .and(EPOCH_STAKE.EPOCH.eq(activeEpoch))
                .fetchOne("active_stake", BigInteger.class);

        // Total stake at each epoch
        BigInteger totalLiveStake = dsl.select(coalesce(sum(EPOCH_STAKE.AMOUNT), val(BigInteger.ZERO)).as("total"))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.EPOCH.eq(latestEpoch))
                .fetchOne("total", BigInteger.class);

        BigInteger totalActiveStake = dsl.select(coalesce(sum(EPOCH_STAKE.AMOUNT), val(BigInteger.ZERO)).as("total"))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.EPOCH.eq(activeEpoch))
                .fetchOne("total", BigInteger.class);

        // nopt and circulation from adapot/epoch_param
        int nopt = fetchNopt(latestEpoch);
        BigInteger circulation = fetchCirculation(latestEpoch);

        return Optional.of(new BFPoolStakeInfo(
                poolIdHex, liveStake, liveDelegators, activeStake,
                totalLiveStake, totalActiveStake, circulation, nopt
        ));
    }

    @Override
    public Map<String, BFPoolStakeInfo> getPoolsStakeInfoBatch(List<String> poolIdHexes) {
        if (poolIdHexes == null || poolIdHexes.isEmpty()) return Map.of();

        Integer latestEpoch = dsl.select(max(EPOCH_STAKE.EPOCH))
                .from(EPOCH_STAKE)
                .fetchOneInto(Integer.class);
        if (latestEpoch == null) return Map.of();

        int activeEpoch = Math.max(latestEpoch - 2, 0);

        // Total stakes (computed once)
        BigInteger totalLiveStake = dsl.select(coalesce(sum(EPOCH_STAKE.AMOUNT), val(BigInteger.ZERO)).as("total"))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.EPOCH.eq(latestEpoch))
                .fetchOne("total", BigInteger.class);

        BigInteger totalActiveStake = dsl.select(coalesce(sum(EPOCH_STAKE.AMOUNT), val(BigInteger.ZERO)).as("total"))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.EPOCH.eq(activeEpoch))
                .fetchOne("total", BigInteger.class);

        int nopt = fetchNopt(latestEpoch);
        BigInteger circulation = fetchCirculation(latestEpoch);

        // Live stake per pool (latest epoch) in one query
        Map<String, BigInteger> liveStakes = new HashMap<>();
        Map<String, Integer> liveDelegatorCounts = new HashMap<>();
        dsl.select(EPOCH_STAKE.POOL_ID,
                        coalesce(sum(EPOCH_STAKE.AMOUNT), val(BigInteger.ZERO)).as("live_stake"),
                        count().as("live_delegators"))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.POOL_ID.in(poolIdHexes))
                .and(EPOCH_STAKE.EPOCH.eq(latestEpoch))
                .groupBy(EPOCH_STAKE.POOL_ID)
                .fetch()
                .forEach(r -> {
                    String pid = r.get(EPOCH_STAKE.POOL_ID);
                    liveStakes.put(pid, toBigInteger(r.get("live_stake")));
                    liveDelegatorCounts.put(pid, r.get("live_delegators", Integer.class));
                });

        // Active stake per pool (latest-2 epoch) in one query
        Map<String, BigInteger> activeStakes = new HashMap<>();
        dsl.select(EPOCH_STAKE.POOL_ID,
                        coalesce(sum(EPOCH_STAKE.AMOUNT), val(BigInteger.ZERO)).as("active_stake"))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.POOL_ID.in(poolIdHexes))
                .and(EPOCH_STAKE.EPOCH.eq(activeEpoch))
                .groupBy(EPOCH_STAKE.POOL_ID)
                .fetch()
                .forEach(r -> activeStakes.put(r.get(EPOCH_STAKE.POOL_ID), toBigInteger(r.get("active_stake"))));

        // Build result map
        Map<String, BFPoolStakeInfo> result = new HashMap<>();
        for (String pid : poolIdHexes) {
            BigInteger live = liveStakes.getOrDefault(pid, BigInteger.ZERO);
            int delegators = liveDelegatorCounts.getOrDefault(pid, 0);
            BigInteger active = activeStakes.getOrDefault(pid, BigInteger.ZERO);
            if (delegators > 0 || live.compareTo(BigInteger.ZERO) > 0) {
                result.put(pid, new BFPoolStakeInfo(pid, live, delegators, active,
                        totalLiveStake, totalActiveStake, circulation, nopt));
            }
        }
        return result;
    }

    /**
     * Fetch nopt (desired number of pools) from epoch_param JSON for the given epoch.
     * Falls back to the latest available epoch_param if the requested epoch is missing.
     */
    private int fetchNopt(int epoch) {
        var epochParamTable = table(name("epoch_param"));
        var epochField = field(name("epoch_param", "epoch"), Integer.class);
        var paramsField = field(name("epoch_param", "params"));

        // Try exact epoch first, then fallback to latest
        Object paramsJson = dsl.select(paramsField)
                .from(epochParamTable)
                .where(epochField.eq(epoch))
                .fetchOne(0);

        if (paramsJson == null) {
            paramsJson = dsl.select(paramsField)
                    .from(epochParamTable)
                    .orderBy(epochField.desc())
                    .limit(1)
                    .fetchOne(0);
        }

        if (paramsJson != null) {
            try {
                Map<String, Object> params = objectMapper.readValue(paramsJson.toString(), new TypeReference<>() {});
                Object noptVal = params.get("nopt");
                if (noptVal instanceof Number) return ((Number) noptVal).intValue();
                if (noptVal != null) return Integer.parseInt(noptVal.toString());
            } catch (Exception e) {
                log.warn("Failed to parse nopt from epoch_param: {}", e.getMessage());
            }
        }
        return 500; // Cardano default
    }

    /**
     * Fetch total circulating lovelace from adapot table for the given epoch.
     * Used as denominator for live_saturation: saturation = live_stake / (circulation / nopt).
     * Falls back to latest available row if the exact epoch is missing.
     * Returns null if adapot table is empty or missing.
     */
    private BigInteger fetchCirculation(int epoch) {
        var adapotTable = table(name("adapot"));
        var epochField = field(name("adapot", "epoch"), Integer.class);
        var circulationField = field(name("adapot", "circulation"));
        try {
            Object val = dsl.select(circulationField)
                    .from(adapotTable)
                    .where(epochField.eq(epoch))
                    .fetchOne(0);
            if (val == null) {
                val = dsl.select(circulationField)
                        .from(adapotTable)
                        .orderBy(epochField.desc())
                        .limit(1)
                        .fetchOne(0);
            }
            return val != null ? toBigInteger(val) : null;
        } catch (Exception e) {
            log.debug("Could not fetch circulation from adapot: {}", e.getMessage());
            return null;
        }
    }

    // --- helper methods ---

    @SuppressWarnings("unchecked")
    private List<String> parsePoolOwners(Object poolOwnersObj) {
        if (poolOwnersObj == null) {
            return Collections.emptyList();
        }
        try {
            List<String> owners = objectMapper.readValue(poolOwnersObj.toString(), new TypeReference<>() {});
            return owners != null ? owners : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to parse pool owners JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getStringField(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Integer getIntField(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigInteger toBigInteger(Object value) {
        if (value == null) return null;
        if (value instanceof BigInteger) return (BigInteger) value;
        if (value instanceof java.math.BigDecimal) return ((java.math.BigDecimal) value).toBigInteger();
        if (value instanceof Number) return BigInteger.valueOf(((Number) value).longValue());
        return new BigInteger(value.toString());
    }
}
