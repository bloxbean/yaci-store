package com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.jooq.Field;
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
