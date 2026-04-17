package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl;

import com.bloxbean.cardano.yaci.store.account.jooq.tables.StakeAddressBalanceView;
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
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.SortField;
import org.jooq.Table;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.*;
import static com.bloxbean.cardano.yaci.store.blocks.jooq.Tables.BLOCK;
import static com.bloxbean.cardano.yaci.store.transaction.jooq.Tables.WITHDRAWAL;
import static com.bloxbean.cardano.yaci.store.governance.jooq.Tables.VOTING_PROCEDURE;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.EPOCH_STAKE;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.INSTANT_REWARD;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD;
import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD_REST;
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
        Table<?> currentPools = latestPoolStateTable(true, false, false);
        SortField<?> registrationOrder = "desc".equals(order)
                ? field(name("current_pools", "registration_slot"), Long.class).desc()
                : field(name("current_pools", "registration_slot"), Long.class).asc();
        SortField<?> slotOrder = "desc".equals(order)
                ? field(name("current_pools", "slot"), Long.class).desc()
                : field(name("current_pools", "slot"), Long.class).asc();

        return dsl.select(field(name("current_pools", "pool_id"), String.class))
                .from(currentPools)
                .orderBy(registrationOrder, slotOrder)
                .limit(count)
                .offset(offset)
                .fetch(record -> PoolUtil.getBech32PoolId(record.get(field(name("current_pools", "pool_id"), String.class))));
    }

    @Override
    public List<BFPoolRetireItem> getRetiredPools(int page, int count, String order) {
        int offset = page * count;
        Integer currentEpoch = dsl.select(max(BLOCK.EPOCH)).from(BLOCK).fetchOneInto(Integer.class);
        Field<Integer> rn = rowNumber()
                .over(partitionBy(POOL.POOL_ID).orderBy(POOL.SLOT.desc(), POOL.TX_INDEX.desc(), POOL.CERT_INDEX.desc()))
                .as("rn");
        Table<?> rankedRetired = dsl.select(
                        POOL.POOL_ID,
                        POOL.STATUS,
                        POOL.RETIRE_EPOCH,
                        POOL.SLOT,
                        POOL.TX_HASH,
                        POOL.TX_INDEX,
                        POOL.CERT_INDEX,
                        rn
                )
                .from(POOL)
                .where(currentEpoch != null ? POOL.EPOCH.le(currentEpoch) : noCondition())
                .asTable("ranked_retired");
        Table<?> retiredPools = dsl.select(
                        field(name("ranked_retired", "pool_id")).as("pool_id"),
                        field(name("ranked_retired", "retire_epoch")).as("retire_epoch"),
                        field(name("ranked_retired", "slot")).as("slot"),
                        field(name("ranked_retired", "tx_hash")).as("tx_hash"),
                        field(name("ranked_retired", "tx_index")).as("tx_index"),
                        field(name("ranked_retired", "cert_index")).as("cert_index")
                )
                .from(rankedRetired)
                .where(field(name("ranked_retired", "rn"), Integer.class).eq(1))
                .and(field(name("ranked_retired", "status"), String.class).eq("RETIRED"))
                .asTable("retired_pools");
        Table<?> latestRetiring = latestPoolStatusTable("RETIRING", "latest_retiring");
        Field<String> poolIdField = retiredPools.field("pool_id", String.class);
        Field<Integer> retireEpochField = retiredPools.field("retire_epoch", Integer.class);
        Field<Long> slotField = retiredPools.field("slot", Long.class);
        Field<String> txHashField = retiredPools.field("tx_hash", String.class);
        Field<Integer> certIndexField = retiredPools.field("cert_index", Integer.class);
        Field<Long> retireSortSlotField = coalesce(latestRetiring.field("slot", Long.class), slotField);
        Field<String> retireSortTxHashField = coalesce(latestRetiring.field("tx_hash", String.class), txHashField);
        Field<Integer> retireSortCertIndexField = coalesce(latestRetiring.field("cert_index", Integer.class), certIndexField);
        SortField<?> retireEpochOrder = "desc".equals(order)
                ? retireEpochField.desc()
                : retireEpochField.asc();
        SortField<?> slotOrder = "desc".equals(order)
                ? retireSortSlotField.desc()
                : retireSortSlotField.asc();

        return dsl.select(poolIdField, retireEpochField)
                .from(retiredPools)
                .leftJoin(latestRetiring).on(poolIdField.eq(latestRetiring.field("pool_id", String.class)))
                .orderBy(retireEpochOrder, slotOrder, retireSortTxHashField.asc(), retireSortCertIndexField.asc(), poolIdField.asc())
                .limit(count)
                .offset(offset)
                .fetch(record -> new BFPoolRetireItem(
                        record.get(poolIdField),
                        record.get(retireEpochField)
                ));
    }

    @Override
    public List<BFPoolRetireItem> getRetiringPools(int page, int count, String order) {
        int offset = page * count;
        Table<?> retiringPools = latestPoolStateTable(false, false, true);
        Field<String> poolIdField = retiringPools.field("pool_id", String.class);
        Field<Integer> retireEpochField = retiringPools.field("retire_epoch", Integer.class);
        Field<Long> slotField = retiringPools.field("slot", Long.class);
        Field<String> txHashField = retiringPools.field("tx_hash", String.class);
        Field<Integer> certIndexField = retiringPools.field("cert_index", Integer.class);
        SortField<?> retireEpochOrder = "desc".equals(order)
                ? retireEpochField.desc()
                : retireEpochField.asc();
        SortField<?> slotOrder = "desc".equals(order)
                ? slotField.desc()
                : slotField.asc();

        return dsl.select(poolIdField, retireEpochField)
                .from(retiringPools)
                .orderBy(retireEpochOrder, slotOrder, txHashField.asc(), certIndexField.asc())
                .limit(count)
                .offset(offset)
                .fetch(record -> new BFPoolRetireItem(
                        record.get(poolIdField),
                        record.get(retireEpochField)
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
                // Cardano relay type 1 (SingleHostName): has port + dnsName → dns field
                // Cardano relay type 2 (MultiHostName / SRV): no port, only dnsName → dns_srv field
                // yaci stores both as "dnsName"; type 2 has port=null or port=0
                String dnsName = getStringField(relay, "dnsName");
                Integer port   = getIntField(relay, "port");
                boolean isSrv  = dnsName != null && (port == null || port == 0);
                String dns    = (!isSrv) ? dnsName : null;
                String dnsSrv = isSrv ? dnsName : null;
                result.add(BFPoolRelayDto.builder()
                        .ipv4(getStringField(relay, "ipv4"))
                        .ipv6(getStringField(relay, "ipv6"))
                        .dns(dns)
                        .dnsSrv(dnsSrv)
                        .port(port)
                        .build());
            }
            Comparator<BFPoolRelayDto> relayOrder = Comparator
                    .comparing(BFPoolRelayDto::getIpv4,   Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(BFPoolRelayDto::getIpv6,   Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(BFPoolRelayDto::getDns,    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(BFPoolRelayDto::getDnsSrv, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(BFPoolRelayDto::getPort,   Comparator.nullsLast(Comparator.naturalOrder()));
            result.sort(relayOrder);
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
    public Optional<String> getLatestPoolStatus(String poolIdHex) {
        return dsl.select(POOL.STATUS)
                .from(POOL)
                .where(POOL.POOL_ID.eq(poolIdHex))
                .orderBy(POOL.SLOT.desc(), POOL.TX_INDEX.desc(), POOL.CERT_INDEX.desc())
                .limit(1)
                .fetchOptional(POOL.STATUS);
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
        Table<?> currentPools = latestPoolStateTable(true, false, false);
        Table<?> latestRegistration = latestPoolRegistrationTable();

        var blockCounts = dsl.select(
                        BLOCK.SLOT_LEADER,
                        count().as("blocks_minted")
                )
                .from(BLOCK)
                .groupBy(BLOCK.SLOT_LEADER)
                .asTable("block_counts");

        SortField<?> registrationOrder = "desc".equals(order)
                ? field(name("current_pools", "registration_slot"), Long.class).desc()
                : field(name("current_pools", "registration_slot"), Long.class).asc();
        SortField<?> slotOrder = "desc".equals(order)
                ? field(name("current_pools", "slot"), Long.class).desc()
                : field(name("current_pools", "slot"), Long.class).asc();

        return dsl.select(
                        field(name("latest_reg", "pool_id"), String.class).as(POOL_REGISTRATION.POOL_ID.getName()),
                        field(name("latest_reg", "vrf_key"), String.class).as(POOL_REGISTRATION.VRF_KEY.getName()),
                        field(name("latest_reg", "pledge"), BigInteger.class).as(POOL_REGISTRATION.PLEDGE.getName()),
                        field(name("latest_reg", "cost"), BigInteger.class).as(POOL_REGISTRATION.COST.getName()),
                        field(name("latest_reg", "margin_numerator"), BigInteger.class).as(POOL_REGISTRATION.MARGIN_NUMERATOR.getName()),
                        field(name("latest_reg", "margin_denominator"), BigInteger.class).as(POOL_REGISTRATION.MARGIN_DENOMINATOR.getName()),
                        field(name("latest_reg", "metadata_url")).as(POOL_REGISTRATION.METADATA_URL.getName()),
                        field(name("latest_reg", "metadata_hash"), String.class).as(POOL_REGISTRATION.METADATA_HASH.getName()),
                        coalesce(field(name("block_counts", "blocks_minted"), Integer.class), val(0)).as("blocks_minted")
                )
                .from(currentPools)
                .innerJoin(latestRegistration)
                .on(field(name("current_pools", "pool_id"), String.class).eq(field(name("latest_reg", "pool_id"), String.class)))
                // Fix: block.slot_leader = pool_id hex, not vrf_key
                .leftJoin(blockCounts)
                .on(field(name("latest_reg", "pool_id"), String.class).eq(field(name("block_counts", "slot_leader"), String.class)))
                .orderBy(registrationOrder, slotOrder)
                .limit(count)
                .offset(offset)
                .fetch(record -> {
                    Object metadataUrlField = record.get(POOL_REGISTRATION.METADATA_URL.getName());
                    String metadataUrl = metadataUrlField != null
                            ? metadataUrlField.toString()
                            : null;
                    return new BFPoolRegistrationInfo(
                            record.get(POOL_REGISTRATION.POOL_ID.getName(), String.class),
                            record.get(POOL_REGISTRATION.VRF_KEY.getName(), String.class),
                            toBigInteger(record.get(POOL_REGISTRATION.PLEDGE.getName())),
                            toBigInteger(record.get(POOL_REGISTRATION.COST.getName())),
                            toBigInteger(record.get(POOL_REGISTRATION.MARGIN_NUMERATOR.getName())),
                            toBigInteger(record.get(POOL_REGISTRATION.MARGIN_DENOMINATOR.getName())),
                            metadataUrl,
                            record.get(POOL_REGISTRATION.METADATA_HASH.getName(), String.class),
                            record.get("blocks_minted", Integer.class)
                    );
                });
    }

    private Table<?> latestPoolStateTable(boolean includeRegistered, boolean includeRetired, boolean includeRetiring) {
        Integer currentEpoch = dsl.select(max(BLOCK.EPOCH)).from(BLOCK).fetchOneInto(Integer.class);

        Field<Integer> rn = rowNumber()
                .over(partitionBy(POOL.POOL_ID).orderBy(POOL.SLOT.desc(), POOL.TX_INDEX.desc(), POOL.CERT_INDEX.desc()))
                .as("rn");

        Table<?> ranked = dsl.select(
                        POOL.POOL_ID,
                        POOL.STATUS,
                        POOL.RETIRE_EPOCH,
                        POOL.SLOT,
                        POOL.TX_HASH,
                        POOL.TX_INDEX,
                        POOL.CERT_INDEX,
                        POOL.REGISTRATION_SLOT,
                        rn
                )
                .from(POOL)
                .where(currentEpoch != null ? POOL.EPOCH.le(currentEpoch) : noCondition())
                .asTable("ranked_pools");

        Field<String> statusField = field(name("ranked_pools", "status"), String.class);
        List<String> includedStatuses = new ArrayList<>();
        if (includeRegistered) {
            includedStatuses.add("REGISTRATION");
            includedStatuses.add("UPDATE");
        }
        if (includeRetired) includedStatuses.add("RETIRED");
        if (includeRetiring) includedStatuses.add("RETIRING");

        return dsl.select(
                        field(name("ranked_pools", "pool_id")).as("pool_id"),
                        statusField.as("status"),
                        field(name("ranked_pools", "retire_epoch")).as("retire_epoch"),
                        field(name("ranked_pools", "slot")).as("slot"),
                        field(name("ranked_pools", "tx_hash")).as("tx_hash"),
                        field(name("ranked_pools", "tx_index")).as("tx_index"),
                        field(name("ranked_pools", "cert_index")).as("cert_index"),
                        field(name("ranked_pools", "registration_slot")).as("registration_slot")
                )
                .from(ranked)
                .where(field(name("ranked_pools", "rn"), Integer.class).eq(1))
                .and(statusField.in(includedStatuses))
                .asTable("current_pools");
    }

    private Table<?> latestPoolRegistrationTable() {
        Field<Integer> rn = rowNumber()
                .over(partitionBy(POOL_REGISTRATION.POOL_ID)
                        .orderBy(POOL_REGISTRATION.SLOT.desc(), POOL_REGISTRATION.TX_INDEX.desc(), POOL_REGISTRATION.CERT_INDEX.desc()))
                .as("rn");

        Table<?> ranked = dsl.select(
                        POOL_REGISTRATION.POOL_ID,
                        POOL_REGISTRATION.VRF_KEY,
                        POOL_REGISTRATION.PLEDGE,
                        POOL_REGISTRATION.COST,
                        POOL_REGISTRATION.MARGIN_NUMERATOR,
                        POOL_REGISTRATION.MARGIN_DENOMINATOR,
                        POOL_REGISTRATION.METADATA_URL,
                        POOL_REGISTRATION.METADATA_HASH,
                        rn
                )
                .from(POOL_REGISTRATION)
                .asTable("ranked_reg");

        return dsl.select(
                        field(name("ranked_reg", "pool_id")).as("pool_id"),
                        field(name("ranked_reg", "vrf_key")).as("vrf_key"),
                        field(name("ranked_reg", "pledge")).as("pledge"),
                        field(name("ranked_reg", "cost")).as("cost"),
                        field(name("ranked_reg", "margin_numerator")).as("margin_numerator"),
                        field(name("ranked_reg", "margin_denominator")).as("margin_denominator"),
                        field(name("ranked_reg", "metadata_url")).as("metadata_url"),
                        field(name("ranked_reg", "metadata_hash")).as("metadata_hash")
                )
                .from(ranked)
                .where(field(name("ranked_reg", "rn"), Integer.class).eq(1))
                .asTable("latest_reg");
    }

    private Table<?> latestPoolStatusTable(String status, String alias) {
        Field<Integer> rn = rowNumber()
                .over(partitionBy(POOL.POOL_ID).orderBy(POOL.SLOT.desc(), POOL.TX_INDEX.desc(), POOL.CERT_INDEX.desc()))
                .as("rn");

        Table<?> ranked = dsl.select(
                        POOL.POOL_ID,
                        POOL.SLOT,
                        POOL.TX_HASH,
                        POOL.TX_INDEX,
                        POOL.CERT_INDEX,
                        rn
                )
                .from(POOL)
                .where(POOL.STATUS.eq(status))
                .asTable("ranked_" + alias);

        return dsl.select(
                        field(name("ranked_" + alias, "pool_id")).as("pool_id"),
                        field(name("ranked_" + alias, "slot")).as("slot"),
                        field(name("ranked_" + alias, "tx_hash")).as("tx_hash"),
                        field(name("ranked_" + alias, "tx_index")).as("tx_index"),
                        field(name("ranked_" + alias, "cert_index")).as("cert_index")
                )
                .from(ranked)
                .where(field(name("ranked_" + alias, "rn"), Integer.class).eq(1))
                .asTable(alias);
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
        Integer currentBlockEpoch = dsl.select(max(BLOCK.EPOCH)).from(BLOCK).fetchOneInto(Integer.class);
        List<PoolFeeParam> poolFeeParams = getPoolFeeParams(poolIdHex);

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

        // Subquery: total rewards per earned_epoch for this pool.
        // Exclude type='refund': that is the 500 ADA pool deposit returned on retirement,
        // not a staking reward. Including it inflates rewards and fees for the retirement epoch.
        var rewardSub = dsl.select(
                        REWARD.EARNED_EPOCH.as("r_epoch"),
                        sum(REWARD.AMOUNT).as("rewards")
                )
                .from(REWARD)
                .where(REWARD.POOL_ID.eq(poolIdHex))
                .and(REWARD.TYPE.ne("refund"))
                .groupBy(REWARD.EARNED_EPOCH)
                .asTable("reward_sub");

        // Subquery: blocks and fees per epoch for this pool
        var blockSub = dsl.select(
                        BLOCK.EPOCH.as("b_epoch"),
                        count().as("blocks")
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
                        field(name("block_sub", "blocks"))
                )
                .from(EPOCH_STAKE)
                .leftJoin(totalStakeSub)
                    .on(field(name("total_stake_sub", "ts_epoch"), Integer.class).eq(EPOCH_STAKE.EPOCH.plus(2)))
                .leftJoin(rewardSub)
                    .on(field(name("reward_sub", "r_epoch"), Integer.class).eq(EPOCH_STAKE.EPOCH.plus(2)))
                .leftJoin(blockSub)
                    .on(field(name("block_sub", "b_epoch"), Integer.class).eq(EPOCH_STAKE.EPOCH.plus(2)))
                .where(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                .and(currentBlockEpoch != null
                        ? EPOCH_STAKE.EPOCH.plus(2).le(currentBlockEpoch - 1)
                        : noCondition())
                .groupBy(
                        EPOCH_STAKE.EPOCH,
                        field(name("total_stake_sub", "total_stake")),
                        field(name("reward_sub", "rewards")),
                        field(name("block_sub", "blocks"))
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
                    Integer blocks         = r.get("blocks", Integer.class);
                    Integer epoch          = r.get("active_epoch", Integer.class);
                    PoolFeeParam poolFeeParam = findEffectivePoolFeeParam(poolFeeParams, epoch);
                    Double activeSize = calculateActiveSize(activeStake, totalStake);
                    String rewardsValue = mapHistoryRewardsValue(rewards);
                    String feesValue = calculateOperatorFees(rewards, poolFeeParam);
                    return BFPoolHistoryDto.builder()
                            .epoch(epoch)
                            .blocks(blocks != null ? blocks : 0)
                            .fees(feesValue)
                            .activeStake(activeStake != null ? activeStake.toString() : null)
                            .activeSize(activeSize)
                            .delegatorsCount(r.get("delegators_count", Integer.class))
                            .rewards(rewardsValue)
                            .build();
                })
                .toList();
    }

    private String mapHistoryRewardsValue(BigInteger rewards) {
        return rewards != null ? rewards.toString() : "0";
    }

    private List<PoolFeeParam> getPoolFeeParams(String poolIdHex) {
        return dsl.select(
                        POOL.ACTIVE_EPOCH,
                        POOL_REGISTRATION.COST,
                        POOL_REGISTRATION.MARGIN_NUMERATOR,
                        POOL_REGISTRATION.MARGIN_DENOMINATOR
                )
                .from(POOL)
                .join(POOL_REGISTRATION)
                    .on(POOL_REGISTRATION.POOL_ID.eq(POOL.POOL_ID))
                    .and(POOL_REGISTRATION.TX_HASH.eq(POOL.TX_HASH))
                    .and(POOL_REGISTRATION.TX_INDEX.eq(POOL.TX_INDEX))
                    .and(POOL_REGISTRATION.CERT_INDEX.eq(POOL.CERT_INDEX))
                .where(POOL.POOL_ID.eq(poolIdHex))
                .and(POOL.STATUS.in("REGISTRATION", "UPDATE"))
                .orderBy(POOL.ACTIVE_EPOCH.asc(), POOL.SLOT.asc(), POOL.TX_INDEX.asc(), POOL.CERT_INDEX.asc())
                .fetch(record -> new PoolFeeParam(
                        record.get(POOL.ACTIVE_EPOCH),
                        record.get(POOL_REGISTRATION.COST),
                        record.get(POOL_REGISTRATION.MARGIN_NUMERATOR),
                        record.get(POOL_REGISTRATION.MARGIN_DENOMINATOR)
                ));
    }

    private PoolFeeParam findEffectivePoolFeeParam(List<PoolFeeParam> poolFeeParams, Integer epoch) {
        if (epoch == null) {
            return null;
        }

        PoolFeeParam effective = null;
        for (PoolFeeParam poolFeeParam : poolFeeParams) {
            if (poolFeeParam.effectiveActiveEpoch() != null && poolFeeParam.effectiveActiveEpoch() <= epoch) {
                effective = poolFeeParam;
            } else {
                break;
            }
        }

        return effective;
    }

    private String calculateOperatorFees(BigInteger rewards, PoolFeeParam poolFeeParam) {
        if (rewards == null || rewards.signum() <= 0) {
            return "0";
        }

        if (poolFeeParam == null
                || poolFeeParam.cost() == null
                || poolFeeParam.marginNumerator() == null
                || poolFeeParam.marginDenominator() == null
                || BigInteger.ZERO.equals(poolFeeParam.marginDenominator())) {
            return "0";
        }

        BigInteger cost = poolFeeParam.cost();
        BigInteger marginNumerator = poolFeeParam.marginNumerator();
        BigInteger marginDenominator = poolFeeParam.marginDenominator();

        if (rewards.compareTo(cost) <= 0) {
            return rewards.toString();
        }

        BigInteger variablePortion = rewards.subtract(cost)
                .multiply(marginNumerator)
                .divide(marginDenominator);

        return cost.add(variablePortion).toString();
    }

    private Double calculateActiveSize(BigInteger activeStake, BigInteger totalStake) {
        if (activeStake == null || totalStake == null || totalStake.compareTo(BigInteger.ZERO) <= 0) {
            return null;
        }

        return new BigDecimal(activeStake)
                .divide(new BigDecimal(totalStake), 20, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record PoolFeeParam(Integer effectiveActiveEpoch, BigInteger cost, BigInteger marginNumerator, BigInteger marginDenominator) {
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

        Integer globalLatestEpoch = dsl.select(max(EPOCH_STAKE.EPOCH))
                .from(EPOCH_STAKE)
                .fetchOneInto(Integer.class);
        Integer latestEpoch = dsl.select(max(EPOCH_STAKE.EPOCH))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                .fetchOneInto(Integer.class);

        if (latestEpoch == null || globalLatestEpoch == null) return List.of();
        boolean historicalPool = latestEpoch < globalLatestEpoch;

        Table<?> currentDelegators = currentDelegatorStateTable();
        Field<String> addressField = field(name("current_delegators", "address"), String.class);
        Field<Long> slotField = field(name("current_delegators", "slot"), Long.class);
        Field<Integer> certIndexField = field(name("current_delegators", "cert_index"), Integer.class);
        Field<String> poolIdField = field(name("current_delegators", "pool_id"), String.class);
        Table<?> rewardTotals = aggregatedAddressAmountTable(REWARD, "reward_total", "reward_totals");
        Table<?> rewardRestTotals = aggregatedAddressAmountTable(REWARD_REST, "reward_rest_total", "reward_rest_totals");
        Table<?> instantRewardTotals = aggregatedAddressAmountTable(INSTANT_REWARD, "instant_reward_total", "instant_reward_totals");
        Table<?> withdrawalTotals = aggregatedAddressAmountTable(WITHDRAWAL, "withdrawal_total", "withdrawal_totals");

        StakeAddressBalanceView stakeBalanceView = StakeAddressBalanceView.STAKE_ADDRESS_BALANCE_VIEW.as("stake_balance_view");
        Field<BigInteger> rewardTotalField = coalesce(field(name("reward_totals", "reward_total"), BigInteger.class), BigInteger.ZERO);
        Field<BigInteger> rewardRestTotalField = coalesce(field(name("reward_rest_totals", "reward_rest_total"), BigInteger.class), BigInteger.ZERO);
        Field<BigInteger> instantRewardTotalField = coalesce(field(name("instant_reward_totals", "instant_reward_total"), BigInteger.class), BigInteger.ZERO);
        Field<BigInteger> withdrawalTotalField = coalesce(field(name("withdrawal_totals", "withdrawal_total"), BigInteger.class), BigInteger.ZERO);
        Field<BigInteger> availableRewardField = rewardTotalField
                .add(rewardRestTotalField)
                .add(instantRewardTotalField)
                .sub(withdrawalTotalField);
        Field<BigInteger> nonNegativeAvailableRewardField = when(availableRewardField.lt(BigInteger.ZERO), BigInteger.ZERO)
                .otherwise(availableRewardField);
        Field<BigInteger> currentStakeBalance = historicalPool
                ? stakeBalanceView.QUANTITY.add(nonNegativeAvailableRewardField)
                : when(stakeBalanceView.EPOCH.gt(latestEpoch), stakeBalanceView.QUANTITY.add(nonNegativeAvailableRewardField))
                    .otherwise((BigInteger) null);
        Field<BigInteger> liveStakeField = coalesce(currentStakeBalance, EPOCH_STAKE.AMOUNT).as("live_stake");
        SortField<?> slotOrder = asc ? slotField.asc() : slotField.desc();
        SortField<?> certIndexOrder = asc ? certIndexField.asc() : certIndexField.desc();
        SortField<?> addressOrder = asc ? addressField.asc() : addressField.desc();

        var query = dsl.select(addressField, liveStakeField)
                .from(currentDelegators);

        var joined = historicalPool
                ? query.leftJoin(EPOCH_STAKE)
                    .on(EPOCH_STAKE.ADDRESS.eq(addressField))
                    .and(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                    .and(EPOCH_STAKE.EPOCH.eq(latestEpoch))
                : query.join(EPOCH_STAKE)
                    .on(EPOCH_STAKE.ADDRESS.eq(addressField))
                    .and(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                    .and(EPOCH_STAKE.EPOCH.eq(latestEpoch));

        return joined
                .leftJoin(stakeBalanceView)
                    .on(stakeBalanceView.ADDRESS.eq(addressField))
                .leftJoin(rewardTotals)
                    .on(field(name("reward_totals", "address"), String.class).eq(addressField))
                .leftJoin(rewardRestTotals)
                    .on(field(name("reward_rest_totals", "address"), String.class).eq(addressField))
                .leftJoin(instantRewardTotals)
                    .on(field(name("instant_reward_totals", "address"), String.class).eq(addressField))
                .leftJoin(withdrawalTotals)
                    .on(field(name("withdrawal_totals", "address"), String.class).eq(addressField))
                .where(poolIdField.eq(poolIdHex))
                .orderBy(slotOrder, certIndexOrder, addressOrder)
                .limit(count).offset(offset)
                .fetch()
                .stream()
                .map(r -> BFPoolDelegatorDto.builder()
                        .address(r.get(addressField))
                        .liveStake(r.get(liveStakeField) != null
                                ? r.get(liveStakeField).toString()
                                : null)
                        .build())
                .toList();
    }

    private Table<?> aggregatedAddressAmountTable(Table<?> sourceTable, String totalAlias, String tableAlias) {
        Field<String> address = field(name(sourceTable.getName(), "address"), String.class);
        Field<BigInteger> amount = field(name(sourceTable.getName(), "amount"), BigInteger.class);

        return dsl.select(
                        address.as("address"),
                        coalesce(sum(amount), BigInteger.ZERO).as(totalAlias)
                )
                .from(sourceTable)
                .groupBy(address)
                .asTable(tableAlias);
    }

    private Table<?> currentDelegatorStateTable() {
        if (BlockfrostDialectUtil.isPostgres(dsl)) {
            var latestDel = dsl
                    .select(
                            field("DISTINCT ON (address) address", String.class).as("address"),
                            DELEGATION.POOL_ID.as("pool_id"),
                            DELEGATION.SLOT.as("slot"),
                            DELEGATION.CERT_INDEX.as("cert_index")
                    )
                    .from(DELEGATION)
                    .orderBy(DELEGATION.ADDRESS, DELEGATION.SLOT.desc(), DELEGATION.CERT_INDEX.desc())
                    .asTable("latest_del");

            return dsl.select(
                            field(name("latest_del", "address")).as("address"),
                            field(name("latest_del", "pool_id")).as("pool_id"),
                            field(name("latest_del", "slot")).as("slot"),
                            field(name("latest_del", "cert_index")).as("cert_index")
                    )
                    .from(latestDel)
                    .asTable("current_delegators");
        }

        var ranked = dsl.select(
                        DELEGATION.ADDRESS.as("address"),
                        DELEGATION.POOL_ID.as("pool_id"),
                        DELEGATION.SLOT.as("slot"),
                        DELEGATION.CERT_INDEX.as("cert_index"),
                        rowNumber().over(
                                partitionBy(DELEGATION.ADDRESS)
                                        .orderBy(DELEGATION.SLOT.desc(), DELEGATION.CERT_INDEX.desc())
                        ).as("rn")
                )
                .from(DELEGATION)
                .asTable("ranked_delegation");

        return dsl.select(
                        field(name("ranked_delegation", "address")).as("address"),
                        field(name("ranked_delegation", "pool_id")).as("pool_id"),
                        field(name("ranked_delegation", "slot")).as("slot"),
                        field(name("ranked_delegation", "cert_index")).as("cert_index")
                )
                .from(ranked)
                .where(field(name("ranked_delegation", "rn"), Integer.class).eq(1))
                .asTable("current_delegators");
    }

    // -------------------------------------------------------------------------
    // Stake info: live/active stake from epoch_stake (for pool detail & extended)
    // -------------------------------------------------------------------------

    @Override
    public Optional<BFPoolStakeInfo> getPoolStakeInfo(String poolIdHex) {
        Integer globalLatestEpoch = dsl.select(max(EPOCH_STAKE.EPOCH))
                .from(EPOCH_STAKE)
                .fetchOneInto(Integer.class);
        if (globalLatestEpoch == null) return Optional.empty();

        Integer poolLatestEpoch = dsl.select(max(EPOCH_STAKE.EPOCH))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                .fetchOneInto(Integer.class);

        int activeEpoch = Math.max(globalLatestEpoch - 2, 0);

        BigInteger liveStake = BigInteger.ZERO;
        int liveDelegators = 0;
        if (poolLatestEpoch != null) {
            boolean historicalPool = poolLatestEpoch < globalLatestEpoch;
            Table<?> currentDelegators = currentDelegatorStateTable();
            Field<String> addressField = field(name("current_delegators", "address"), String.class);
            Field<String> poolIdField = field(name("current_delegators", "pool_id"), String.class);
            Table<?> rewardTotals = aggregatedAddressAmountTable(REWARD, "reward_total", "reward_totals");
            Table<?> rewardRestTotals = aggregatedAddressAmountTable(REWARD_REST, "reward_rest_total", "reward_rest_totals");
            Table<?> instantRewardTotals = aggregatedAddressAmountTable(INSTANT_REWARD, "instant_reward_total", "instant_reward_totals");
            Table<?> withdrawalTotals = aggregatedAddressAmountTable(WITHDRAWAL, "withdrawal_total", "withdrawal_totals");

            StakeAddressBalanceView stakeBalanceView = StakeAddressBalanceView.STAKE_ADDRESS_BALANCE_VIEW.as("stake_balance_view");
            Field<BigInteger> rewardTotalField = coalesce(field(name("reward_totals", "reward_total"), BigInteger.class), BigInteger.ZERO);
            Field<BigInteger> rewardRestTotalField = coalesce(field(name("reward_rest_totals", "reward_rest_total"), BigInteger.class), BigInteger.ZERO);
            Field<BigInteger> instantRewardTotalField = coalesce(field(name("instant_reward_totals", "instant_reward_total"), BigInteger.class), BigInteger.ZERO);
            Field<BigInteger> withdrawalTotalField = coalesce(field(name("withdrawal_totals", "withdrawal_total"), BigInteger.class), BigInteger.ZERO);
            Field<BigInteger> availableRewardField = rewardTotalField
                    .add(rewardRestTotalField)
                    .add(instantRewardTotalField)
                    .sub(withdrawalTotalField);
            Field<BigInteger> nonNegativeAvailableRewardField = when(availableRewardField.lt(BigInteger.ZERO), BigInteger.ZERO)
                    .otherwise(availableRewardField);
            Field<BigInteger> currentStakeBalance = historicalPool
                    ? stakeBalanceView.QUANTITY.add(nonNegativeAvailableRewardField)
                    : when(stakeBalanceView.EPOCH.gt(poolLatestEpoch), stakeBalanceView.QUANTITY.add(nonNegativeAvailableRewardField))
                        .otherwise((BigInteger) null);
            Field<BigInteger> liveStakeValueField = coalesce(currentStakeBalance, EPOCH_STAKE.AMOUNT);
            var base = dsl.select(
                            coalesce(sum(liveStakeValueField), val(BigInteger.ZERO)).as("live_stake"),
                            count().as("live_delegators")
                    )
                    .from(currentDelegators);
            var joined = historicalPool
                    ? base.leftJoin(EPOCH_STAKE)
                        .on(EPOCH_STAKE.ADDRESS.eq(addressField))
                        .and(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                        .and(EPOCH_STAKE.EPOCH.eq(poolLatestEpoch))
                    : base.join(EPOCH_STAKE)
                        .on(EPOCH_STAKE.ADDRESS.eq(addressField))
                        .and(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                        .and(EPOCH_STAKE.EPOCH.eq(poolLatestEpoch));

            Record liveRec = joined
                    .leftJoin(stakeBalanceView)
                        .on(stakeBalanceView.ADDRESS.eq(addressField))
                    .leftJoin(rewardTotals)
                        .on(field(name("reward_totals", "address"), String.class).eq(addressField))
                    .leftJoin(rewardRestTotals)
                        .on(field(name("reward_rest_totals", "address"), String.class).eq(addressField))
                    .leftJoin(instantRewardTotals)
                        .on(field(name("instant_reward_totals", "address"), String.class).eq(addressField))
                    .leftJoin(withdrawalTotals)
                        .on(field(name("withdrawal_totals", "address"), String.class).eq(addressField))
                    .where(poolIdField.eq(poolIdHex))
                    .fetchOne();

            liveStake = liveRec != null ? toBigInteger(liveRec.get("live_stake")) : BigInteger.ZERO;
            liveDelegators = liveRec != null ? liveRec.get("live_delegators", Integer.class) : 0;
        }

        if (liveDelegators == 0 && poolLatestEpoch == null) return Optional.empty();

        // Pool aggregation at active epoch (latest - 2)
        BigInteger activeStake = dsl.select(coalesce(sum(EPOCH_STAKE.AMOUNT), val(BigInteger.ZERO)).as("active_stake"))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                .and(EPOCH_STAKE.EPOCH.eq(activeEpoch))
                .fetchOne("active_stake", BigInteger.class);

        // Total stake at each epoch
        BigInteger totalLiveStake = dsl.select(coalesce(sum(EPOCH_STAKE.AMOUNT), val(BigInteger.ZERO)).as("total"))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.EPOCH.eq(globalLatestEpoch))
                .fetchOne("total", BigInteger.class);

        BigInteger totalActiveStake = dsl.select(coalesce(sum(EPOCH_STAKE.AMOUNT), val(BigInteger.ZERO)).as("total"))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.EPOCH.eq(activeEpoch))
                .fetchOne("total", BigInteger.class);

        // nopt and circulation from adapot/epoch_param
        int nopt = fetchNopt(globalLatestEpoch);
        BigInteger circulation = fetchCirculation(globalLatestEpoch);

        return Optional.of(new BFPoolStakeInfo(
                poolIdHex, liveStake, liveDelegators, activeStake,
                totalLiveStake, totalActiveStake, circulation, nopt
        ));
    }

    @Override
    public BigInteger getPoolOwnerLiveStake(String poolIdHex, List<String> ownerAddresses) {
        if (ownerAddresses == null || ownerAddresses.isEmpty()) return BigInteger.ZERO;

        Integer globalLatestEpoch = dsl.select(max(EPOCH_STAKE.EPOCH))
                .from(EPOCH_STAKE)
                .fetchOneInto(Integer.class);
        Integer poolLatestEpoch = dsl.select(max(EPOCH_STAKE.EPOCH))
                .from(EPOCH_STAKE)
                .where(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                .fetchOneInto(Integer.class);
        if (globalLatestEpoch == null || poolLatestEpoch == null) return BigInteger.ZERO;

        boolean historicalPool = poolLatestEpoch < globalLatestEpoch;
        Table<?> currentDelegators = currentDelegatorStateTable();
        Field<String> addressField = field(name("current_delegators", "address"), String.class);
        Field<String> poolIdField = field(name("current_delegators", "pool_id"), String.class);
        Table<?> rewardTotals = aggregatedAddressAmountTable(REWARD, "reward_total", "reward_totals");
        Table<?> rewardRestTotals = aggregatedAddressAmountTable(REWARD_REST, "reward_rest_total", "reward_rest_totals");
        Table<?> instantRewardTotals = aggregatedAddressAmountTable(INSTANT_REWARD, "instant_reward_total", "instant_reward_totals");
        Table<?> withdrawalTotals = aggregatedAddressAmountTable(WITHDRAWAL, "withdrawal_total", "withdrawal_totals");

        StakeAddressBalanceView stakeBalanceView = StakeAddressBalanceView.STAKE_ADDRESS_BALANCE_VIEW.as("stake_balance_view");
        Field<BigInteger> rewardTotalField = coalesce(field(name("reward_totals", "reward_total"), BigInteger.class), BigInteger.ZERO);
        Field<BigInteger> rewardRestTotalField = coalesce(field(name("reward_rest_totals", "reward_rest_total"), BigInteger.class), BigInteger.ZERO);
        Field<BigInteger> instantRewardTotalField = coalesce(field(name("instant_reward_totals", "instant_reward_total"), BigInteger.class), BigInteger.ZERO);
        Field<BigInteger> withdrawalTotalField = coalesce(field(name("withdrawal_totals", "withdrawal_total"), BigInteger.class), BigInteger.ZERO);
        Field<BigInteger> availableRewardField = rewardTotalField
                .add(rewardRestTotalField)
                .add(instantRewardTotalField)
                .sub(withdrawalTotalField);
        Field<BigInteger> nonNegativeAvailableRewardField = when(availableRewardField.lt(BigInteger.ZERO), BigInteger.ZERO)
                .otherwise(availableRewardField);
        Field<BigInteger> currentStakeBalance = historicalPool
                ? stakeBalanceView.QUANTITY.add(nonNegativeAvailableRewardField)
                : when(stakeBalanceView.EPOCH.gt(poolLatestEpoch), stakeBalanceView.QUANTITY.add(nonNegativeAvailableRewardField))
                    .otherwise((BigInteger) null);
        Field<BigInteger> liveStakeValueField = coalesce(currentStakeBalance, EPOCH_STAKE.AMOUNT);

        var base = dsl.select(coalesce(sum(liveStakeValueField), val(BigInteger.ZERO)).as("live_pledge"))
                .from(currentDelegators);
        var joined = historicalPool
                ? base.leftJoin(EPOCH_STAKE)
                    .on(EPOCH_STAKE.ADDRESS.eq(addressField))
                    .and(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                    .and(EPOCH_STAKE.EPOCH.eq(poolLatestEpoch))
                : base.join(EPOCH_STAKE)
                    .on(EPOCH_STAKE.ADDRESS.eq(addressField))
                    .and(EPOCH_STAKE.POOL_ID.eq(poolIdHex))
                    .and(EPOCH_STAKE.EPOCH.eq(poolLatestEpoch));

        return joined
                .leftJoin(stakeBalanceView)
                    .on(stakeBalanceView.ADDRESS.eq(addressField))
                .leftJoin(rewardTotals)
                    .on(field(name("reward_totals", "address"), String.class).eq(addressField))
                .leftJoin(rewardRestTotals)
                    .on(field(name("reward_rest_totals", "address"), String.class).eq(addressField))
                .leftJoin(instantRewardTotals)
                    .on(field(name("instant_reward_totals", "address"), String.class).eq(addressField))
                .leftJoin(withdrawalTotals)
                    .on(field(name("withdrawal_totals", "address"), String.class).eq(addressField))
                .where(poolIdField.eq(poolIdHex))
                .and(addressField.in(ownerAddresses))
                .fetchOne("live_pledge", BigInteger.class);
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
