package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Result;
import org.jooq.Record1;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(OutputCaptureExtension.class)
class PostgresLockLimitCheckerTest {

    private static final DSLContext CTX = DSL.using(SQLDialect.POSTGRES);
    private static final Field<String> SETTING = DSL.field(DSL.name("max_locks_per_transaction"), String.class);

    private final List<String> sqls = new ArrayList<>();

    private PostgresLockLimitChecker checker(SQLDialect dialect, String setting) {
        DSLContext dsl = DSL.using(new MockConnection(ctx -> {
            sqls.add(ctx.sql());
            Result<Record1<String>> result = CTX.newResult(SETTING);
            result.add(CTX.newRecord(SETTING).values(setting));
            return new MockResult[]{new MockResult(result.size(), result)};
        }), dialect);
        return new PostgresLockLimitChecker(dsl);
    }

    @Test
    void fetchMaxLocksPerTransaction_readsTheServerSetting() {
        assertThat(checker(SQLDialect.POSTGRES, "64").fetchMaxLocksPerTransaction()).isEqualTo(64);
        assertThat(sqls).containsExactly("show max_locks_per_transaction");
    }

    @Test
    void checkMaxLocksPerTransaction_warnsWhenBelowRecommended(CapturedOutput output) {
        checker(SQLDialect.POSTGRES, "64").checkMaxLocksPerTransaction();

        assertThat(output).contains("max_locks_per_transaction is 64");
    }

    @Test
    void checkMaxLocksPerTransaction_doesNotWarnAtRecommended(CapturedOutput output) {
        checker(SQLDialect.POSTGRES, "1024").checkMaxLocksPerTransaction();

        assertThat(output).doesNotContain("max_locks_per_transaction is");
    }

    @Test
    void checkMaxLocksPerTransaction_skipsNonPostgresDatabases() {
        checker(SQLDialect.H2, "64").checkMaxLocksPerTransaction();

        assertThat(sqls).isEmpty();
    }

    @Test
    void checkMaxLocksPerTransaction_doesNotFailOnAnUnexpectedValue() {
        assertThatNoException().isThrownBy(() -> checker(SQLDialect.POSTGRES, "abc").checkMaxLocksPerTransaction());
    }
}
