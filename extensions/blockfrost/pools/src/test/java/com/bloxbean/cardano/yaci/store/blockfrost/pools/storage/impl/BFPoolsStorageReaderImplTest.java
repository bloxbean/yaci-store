package com.bloxbean.cardano.yaci.store.blockfrost.pools.storage.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BFPoolsStorageReaderImplTest {

    private static final String POOL = "f3e0e5c79aca2a3e46640ed57d2a649c7e9c32ce7574111053f7d4e7";

    private static final DSLContext CTX = DSL.using(SQLDialect.POSTGRES);
    private static final Field<Integer> EPOCH = DSL.field(DSL.name("epoch"), Integer.class);
    private static final Field<Integer> GLOBAL = DSL.field(DSL.name("global_latest_epoch"), Integer.class);
    private static final Field<Integer> POOL_LATEST = DSL.field(DSL.name("pool_latest_epoch"), Integer.class);

    private final List<String> sqls = new ArrayList<>();
    private final List<List<Object>> bindings = new ArrayList<>();

    /**
     * Answers each statement by its shape: the adapot_jobs lookup, the epoch-bounded epoch_stake query,
     * the per-pool fallback, or the unbounded epoch_stake query.
     */
    private BFPoolsStorageReaderImpl reader(Integer completedJobEpoch,
                                            Integer[] bounded,
                                            Integer poolFallback,
                                            Integer[] unbounded) {
        DSLContext dsl = DSL.using(new MockConnection(ctx -> {
            String sql = ctx.sql();
            sqls.add(sql);
            bindings.add(Arrays.asList(ctx.bindings()));

            if (sql.contains("adapot_jobs")) {
                return single(EPOCH, completedJobEpoch);
            }
            boolean epochBound = sql.contains("\"epoch\" >= ?");
            boolean poolLookup = sql.contains("\"pool_id\" = ?") && !sql.contains("case");
            if (poolLookup) {
                return single(EPOCH, poolFallback);
            }
            Integer[] values = epochBound ? bounded : unbounded;
            if (sql.contains("case")) {
                Result<Record2<Integer, Integer>> result = CTX.newResult(GLOBAL, POOL_LATEST);
                result.add(CTX.newRecord(GLOBAL, POOL_LATEST).values(values[0], values[1]));
                return new MockResult[]{new MockResult(1, result)};
            }
            return single(EPOCH, values[0]);
        }), SQLDialect.POSTGRES);
        return new BFPoolsStorageReaderImpl(dsl, new ObjectMapper());
    }

    private static MockResult[] single(Field<Integer> field, Integer value) {
        Result<Record1<Integer>> result = CTX.newResult(field);
        if (value != null) {
            result.add(CTX.newRecord(field).values(value));
        }
        return new MockResult[]{new MockResult(result.size(), result)};
    }

    @Test
    void fetchEpochBounds_poolInRecentSnapshot_usesOnlyTheBoundedQuery() {
        var bounds = reader(1451, new Integer[]{1450, 1450}, null, null).fetchEpochBounds(POOL);

        assertThat(bounds).isEqualTo(new BFPoolsStorageReaderImpl.EpochBounds(1450, 1450));
        // adapot_jobs lookup + one epoch_stake query restricted to the latest snapshot epoch (1451 - 1)
        assertThat(sqls).hasSize(2);
        assertThat(sqls.get(1)).contains("\"epoch\" >= ?");
        assertThat(bindings.get(1)).contains(1450);
    }

    @Test
    void fetchEpochBounds_poolMissingFromRecentSnapshots_looksUpItsLastEpochByPoolId() {
        var bounds = reader(1451, new Integer[]{1450, null}, 1300, null).fetchEpochBounds(POOL);

        // A retired pool keeps its own last epoch, so historicalPool logic is unchanged.
        assertThat(bounds).isEqualTo(new BFPoolsStorageReaderImpl.EpochBounds(1450, 1300));
        assertThat(sqls).hasSize(3);
        assertThat(sqls.get(2)).contains("\"pool_id\" = ?").doesNotContain("case");
        assertThat(bindings.get(2)).containsExactly(POOL);
    }

    @Test
    void fetchEpochBounds_noCompletedRewardJob_usesTheUnboundedQuery() {
        var bounds = reader(null, null, null, new Integer[]{5, 3}).fetchEpochBounds(POOL);

        assertThat(bounds).isEqualTo(new BFPoolsStorageReaderImpl.EpochBounds(5, 3));
        assertThat(sqls).hasSize(2);
        assertThat(sqls.get(1)).contains("case").doesNotContain(">= ?");
    }

    @Test
    void fetchEpochBounds_boundedQueryFindsNothing_fallsBackToTheUnboundedQuery() {
        var bounds = reader(1451, new Integer[]{null, null}, null, new Integer[]{1400, 1399}).fetchEpochBounds(POOL);

        assertThat(bounds).isEqualTo(new BFPoolsStorageReaderImpl.EpochBounds(1400, 1399));
        assertThat(sqls).hasSize(3);
        assertThat(sqls.get(2)).contains("case").doesNotContain(">= ?");
    }

    @Test
    void findLatestEpochStakeEpoch_usesTheBoundedQueryWhenItFindsARow() {
        Integer latest = reader(661, new Integer[]{660}, null, null).findLatestEpochStakeEpoch();

        assertThat(latest).isEqualTo(660);
        assertThat(sqls).hasSize(2);
        assertThat(bindings.get(1)).contains(660);
    }

    @Test
    void findLatestEpochStakeEpoch_fallsBackToTheUnboundedQuery() {
        Integer latest = reader(661, new Integer[]{null}, null, new Integer[]{655}).findLatestEpochStakeEpoch();

        assertThat(latest).isEqualTo(655);
        assertThat(sqls).hasSize(3);
        assertThat(sqls.get(2)).doesNotContain(">= ?");
    }
}
