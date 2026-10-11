package com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl;

import com.bloxbean.cardano.yaci.store.blockfrost.address.storage.impl.model.BFAddressTotal;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record4;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BFAddressStorageReaderImplTest {

    private static final String ADDRESS = "addr_test1vryy0xmvalxpv2n5cpx8wuvq0cf8mahkp5hr2fr3uh7f3jgejq7x8";
    private static final String UNIT = "8b4a5b4a2c84a1c1a8b4a5b4a2c84a1c1a8b4a5b4a2c84a1c1a8b4a5b4a2c8474657374";

    private static final Field<String> UNIT_FIELD = DSL.field(DSL.name("unit"), String.class);
    private static final Field<BigDecimal> RECEIVED_FIELD = DSL.field(DSL.name("received"), BigDecimal.class);
    private static final Field<BigDecimal> SENT_FIELD = DSL.field(DSL.name("sent"), BigDecimal.class);
    private static final Field<Long> TX_COUNT_FIELD = DSL.field(DSL.name("tx_count"), Long.class);

    private final List<String> executedSql = new ArrayList<>();

    @Test
    void getAddressTotal_postgres_runsSingleQueryAndMergesLovelaceRows() {
        Result<Record4<String, BigDecimal, BigDecimal, Long>> rows = newResult();
        rows.add(row("lovelace", new BigDecimal("1000"), new BigDecimal("400"), 3L));
        rows.add(row(UNIT, new BigDecimal("7"), null, null));
        // lovelace from JSON for outputs whose lovelace_amount is missing or zero
        rows.add(row("lovelace", new BigDecimal("5"), new BigDecimal("5"), null));

        BFAddressTotal total = reader(rows).getAddressTotal(ADDRESS);

        assertThat(executedSql).hasSize(1);
        assertThat(executedSql.get(0)).contains("as materialized").contains("left outer join");
        assertThat(total.receivedSum())
                .containsEntry("lovelace", BigInteger.valueOf(1005))
                .containsEntry(UNIT, BigInteger.valueOf(7))
                .hasSize(2);
        assertThat(total.sentSum())
                .containsEntry("lovelace", BigInteger.valueOf(405))
                .hasSize(1);
        assertThat(total.txCount()).isEqualTo(3L);
    }

    @Test
    void getAddressTotal_postgres_unusedAddressReturnsEmptySumsAndZeroCount() {
        Result<Record4<String, BigDecimal, BigDecimal, Long>> rows = newResult();
        rows.add(row("lovelace", null, null, 0L));

        BFAddressTotal total = reader(rows).getAddressTotal(ADDRESS);

        assertThat(total.receivedSum()).isEmpty();
        assertThat(total.sentSum()).isEmpty();
        assertThat(total.txCount()).isZero();
    }

    private BFAddressStorageReaderImpl reader(Result<Record4<String, BigDecimal, BigDecimal, Long>> rows) {
        DSLContext dsl = DSL.using(new MockConnection(ctx -> {
            executedSql.add(ctx.sql());
            return new MockResult[]{new MockResult(rows.size(), rows)};
        }), SQLDialect.POSTGRES);
        return new BFAddressStorageReaderImpl(dsl);
    }

    private static Result<Record4<String, BigDecimal, BigDecimal, Long>> newResult() {
        return DSL.using(SQLDialect.POSTGRES).newResult(UNIT_FIELD, RECEIVED_FIELD, SENT_FIELD, TX_COUNT_FIELD);
    }

    private static Record4<String, BigDecimal, BigDecimal, Long> row(String unit, BigDecimal received, BigDecimal sent, Long txCount) {
        return DSL.using(SQLDialect.POSTGRES).newRecord(UNIT_FIELD, RECEIVED_FIELD, SENT_FIELD, TX_COUNT_FIELD)
                .values(unit, received, sent, txCount);
    }
}
