package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl;

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
