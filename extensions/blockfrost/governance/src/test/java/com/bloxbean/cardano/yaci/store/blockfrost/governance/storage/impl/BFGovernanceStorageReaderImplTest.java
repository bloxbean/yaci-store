package com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model.BFDRep;
import com.bloxbean.cardano.yaci.store.blockfrost.governance.util.BFDRepIdentity;
import com.bloxbean.cardano.yaci.store.common.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
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
import static org.assertj.core.api.Assertions.tuple;

class BFGovernanceStorageReaderImplTest {

    private static final DSLContext CTX = DSL.using(SQLDialect.POSTGRES);
    private static final Field<Integer> EPOCH = DSL.field(DSL.name("epoch"), Integer.class);

    private final List<String> sqls = new ArrayList<>();
    private final List<List<Object>> bindings = new ArrayList<>();

    private BFGovernanceStorageReaderImpl reader(Integer completedJobEpoch, Integer bounded, Integer unbounded) {
        DSLContext dsl = DSL.using(new MockConnection(ctx -> {
            String sql = ctx.sql();
            sqls.add(sql);
            bindings.add(Arrays.asList(ctx.bindings()));

            if (sql.contains("adapot_jobs")) {
                return single(completedJobEpoch);
            }
            return single(sql.contains("\"epoch\" >= ?") ? bounded : unbounded);
        }), SQLDialect.POSTGRES);
        return new BFGovernanceStorageReaderImpl(dsl, new ObjectMapper());
    }

    private static MockResult[] single(Integer value) {
        Result<Record1<Integer>> result = CTX.newResult(EPOCH);
        if (value != null) {
            result.add(CTX.newRecord(EPOCH).values(value));
        }
        return new MockResult[]{new MockResult(result.size(), result)};
    }

    /** Answers delegation_vote / all_dreps queries with {@code rows}; every other query is empty. */
    private BFGovernanceStorageReaderImpl routedReader(String table, Result<Record> rows) {
        DSLContext dsl = DSL.using(new MockConnection(ctx -> {
            sqls.add(ctx.sql());
            if (ctx.sql().contains(table)) {
                return new MockResult[]{new MockResult(rows.size(), rows)};
            }
            return single(null);
        }), SQLDialect.POSTGRES);
        return new BFGovernanceStorageReaderImpl(dsl, new ObjectMapper());
    }

    private static Result<Record> rows(List<Field<?>> fields, Object[]... values) {
        Result<Record> result = CTX.newResult(fields);
        for (Object[] v : values) {
            Record r = CTX.newRecord(fields);
            r.fromArray(v);
            result.add(r);
        }
        return result;
    }

    private static final Field<String> DREP_HASH = DSL.field(DSL.name("drep_hash"), String.class);
    private static final Field<String> DREP_ID = DSL.field(DSL.name("drep_id"), String.class);
    private static final Field<String> DREP_TYPE = DSL.field(DSL.name("drep_type"), String.class);
    private static final Field<String> STATUS = DSL.field(DSL.name("status"), String.class);
    private static final Field<Integer> LAST_ACTIVE_EPOCH = DSL.field(DSL.name("last_active_epoch"), Integer.class);

    // Mainnet: key-hash DRep with 7 delegations and no RegDRep; its script-hash twin is registered.
    private static final String UNREGISTERED_KEY_DREP = "drep1ytj6kdexrv7kxcqd2ejkfpunwzh2qv02xyytpf4a3nh432shudd2f";
    private static final String UNREGISTERED_HASH = "e5ab37261b3d63600d566564879370aea031ea3108b0a6bd8cef58aa";

    @Test
    void findDRepById_unregisteredDRepWithDelegations_isReturnedWithoutLifecycleData() {
        var reader = routedReader("delegation_vote",
                rows(List.of(DREP_HASH, DREP_TYPE), new Object[]{UNREGISTERED_HASH, "ADDR_KEYHASH"}));

        var dRep = reader.findDRepById(BFDRepIdentity.parse(UNREGISTERED_KEY_DREP));

        assertThat(dRep).hasValueSatisfying(d -> {
            assertThat(d.getDrepId()).isEqualTo(UNREGISTERED_KEY_DREP);
            assertThat(d.getDrepHash()).isEqualTo(UNREGISTERED_HASH);
            assertThat(d.getStatus()).isNull();
            assertThat(d.getEpoch()).isNull();
            assertThat(d.getActiveEpoch()).isNull();
            assertThat(d.getHasScript()).isFalse();
            assertThat(d.isExpired()).isFalse();
        });
    }

    @Test
    void findDRepById_noDRepRowAndNoDelegation_isEmpty() {
        var reader = routedReader("delegation_vote", rows(List.of(DREP_HASH, DREP_TYPE)));

        assertThat(reader.findDRepById(BFDRepIdentity.parse(UNREGISTERED_KEY_DREP))).isEmpty();
    }

    @Test
    void findDRepById_rawHashIdentity_doesNotFallBackToDelegations() {
        var reader = routedReader("delegation_vote",
                rows(List.of(DREP_HASH, DREP_TYPE), new Object[]{UNREGISTERED_HASH, "ADDR_KEYHASH"}));

        assertThat(reader.findDRepById(BFDRepIdentity.rawHash(UNREGISTERED_HASH))).isEmpty();
        assertThat(sqls).noneMatch(sql -> sql.contains("delegation_vote"));
    }

    @Test
    void findAllDReps_delegationOnlyScriptDRep_keepsItsScriptFlag() {
        String scriptDRep = "drep1y0j6kdexrv7kxcqd2ejkfpunwzh2qv02xyytpf4a3nh432shwau2w";
        var reader = routedReader("all_dreps", rows(List.of(DREP_HASH, DREP_ID, STATUS, LAST_ACTIVE_EPOCH, DREP_TYPE),
                new Object[]{UNREGISTERED_HASH, scriptDRep, null, null, "SCRIPTHASH"},
                new Object[]{UNREGISTERED_HASH, UNREGISTERED_KEY_DREP, null, null, "ADDR_KEYHASH"}));

        var dReps = reader.findAllDReps(0, 100, Order.asc);

        assertThat(dReps).extracting(BFDRep::getDrepId, BFDRep::getHasScript, BFDRep::getStatus)
                .containsExactly(tuple(scriptDRep, true, null), tuple(UNREGISTERED_KEY_DREP, false, null));
    }

    @Test
    void findLatestDRepDistEpoch_usesTheBoundedQueryWhenItFindsARow() {
        Integer latest = reader(1451, 1451, null).findLatestDRepDistEpoch();

        assertThat(latest).isEqualTo(1451);
        // adapot_jobs lookup + one drep_dist query restricted to epoch >= 1451 - 1
        assertThat(sqls).hasSize(2);
        assertThat(sqls.get(1)).contains("drep_dist").contains("\"epoch\" >= ?");
        assertThat(bindings.get(1)).contains(1450);
    }

    @Test
    void findLatestDRepDistEpoch_boundedQueryFindsNothing_fallsBackToTheUnboundedQuery() {
        Integer latest = reader(1451, null, 1200).findLatestDRepDistEpoch();

        assertThat(latest).isEqualTo(1200);
        assertThat(sqls).hasSize(3);
        assertThat(sqls.get(2)).contains("drep_dist").doesNotContain(">= ?");
    }

    @Test
    void findLatestDRepDistEpoch_noCompletedRewardJob_usesTheUnboundedQuery() {
        Integer latest = reader(null, null, 507).findLatestDRepDistEpoch();

        assertThat(latest).isEqualTo(507);
        assertThat(sqls).hasSize(2);
        assertThat(sqls.get(1)).doesNotContain(">= ?");
    }
}
