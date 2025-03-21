package com.bloxbean.cardano.yaci.store.staking.storage.impl;

import com.bloxbean.cardano.yaci.store.staking.domain.PoolDetails;
import com.bloxbean.cardano.yaci.store.staking.storage.PoolStorageReader;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;

import java.util.List;

import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.POOL;
import static com.bloxbean.cardano.yaci.store.staking.jooq.Tables.POOL_REGISTRATION;
import static org.jooq.impl.DSL.*;

@RequiredArgsConstructor
public class PoolStorageReaderImpl implements PoolStorageReader {
    private final DSLContext dsl;

    @Override
    public List<PoolDetails> getPoolDetails(List<String> poolIds, Integer epoch) {
        Field<Integer> rn = rowNumber()
                .over(partitionBy(POOL.POOL_ID).orderBy(POOL.SLOT.desc(), POOL.CERT_INDEX.desc()))
                .as("rn");

        Table<?> subquery = dsl.select(POOL.EPOCH,
                        POOL.POOL_ID,
                        POOL_REGISTRATION.VRF_KEY.as("vrf_key_hash"),
                        POOL_REGISTRATION.PLEDGE,
                        POOL_REGISTRATION.COST,
                        POOL_REGISTRATION.MARGIN_NUMERATOR,
                        POOL_REGISTRATION.MARGIN_DENOMINATOR,
                        POOL_REGISTRATION.REWARD_ACCOUNT,
                        POOL_REGISTRATION.POOL_OWNERS,
                        POOL_REGISTRATION.RELAYS,
                        POOL_REGISTRATION.METADATA_URL,
                        POOL_REGISTRATION.METADATA_HASH,
                        POOL.TX_HASH,
                        POOL.CERT_INDEX,
                        POOL.STATUS,
                        POOL.RETIRE_EPOCH, rn)
                .from(POOL)
                .join(POOL_REGISTRATION)
                .on(POOL.TX_HASH.eq(POOL_REGISTRATION.TX_HASH).and(POOL.CERT_INDEX.eq(POOL_REGISTRATION.CERT_INDEX)))
                .where(
                        POOL.ACTIVE_EPOCH.le(param("epoch", epoch))
                        .and(POOL.POOL_ID.in(poolIds))) // replace epoch with your variable

                .asTable("p");

        var result = dsl.select()
                .from(subquery)
                .where(field(name("rn"), Integer.class).eq(1))
                .fetchInto(PoolDetails.class);

        return result;

    }

    @Override
    public List<PoolDetails> getLatestPoolUpdateDetails(List<String> poolIds, Integer txSubmissionEpoch) {
        Field<Integer> rn = rowNumber()
                .over(partitionBy(POOL.POOL_ID).orderBy(POOL.SLOT.desc(), POOL.CERT_INDEX.desc()))
                .as("rn");

        Table<?> subquery = dsl.select(POOL.EPOCH,
                        POOL.POOL_ID,
                        POOL_REGISTRATION.VRF_KEY.as("vrf_key_hash"),
                        POOL_REGISTRATION.PLEDGE,
                        POOL_REGISTRATION.COST,
                        POOL_REGISTRATION.MARGIN_NUMERATOR,
                        POOL_REGISTRATION.MARGIN_DENOMINATOR,
                        POOL_REGISTRATION.REWARD_ACCOUNT,
                        POOL_REGISTRATION.POOL_OWNERS,
                        POOL_REGISTRATION.RELAYS,
                        POOL_REGISTRATION.METADATA_URL,
                        POOL_REGISTRATION.METADATA_HASH,
                        POOL.TX_HASH,
                        POOL.CERT_INDEX,
                        POOL.STATUS,
                        POOL.RETIRE_EPOCH, rn)
                .from(POOL)
                .join(POOL_REGISTRATION)
                .on(POOL.TX_HASH.eq(POOL_REGISTRATION.TX_HASH).and(POOL.CERT_INDEX.eq(POOL_REGISTRATION.CERT_INDEX)))
                .where(
                        POOL.EPOCH.le(param("epoch", txSubmissionEpoch))
                                .and(POOL.POOL_ID.in(poolIds))) // replace epoch with your variable

                .asTable("p");

        var result = dsl.select()
                .from(subquery)
                .where(field(name("rn"), Integer.class).eq(1))
                .fetchInto(PoolDetails.class);

        return result;
    }
}
