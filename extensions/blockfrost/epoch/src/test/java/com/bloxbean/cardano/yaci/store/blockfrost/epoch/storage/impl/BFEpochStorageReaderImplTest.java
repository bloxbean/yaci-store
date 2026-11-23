package com.bloxbean.cardano.yaci.store.blockfrost.epoch.storage.impl;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BFEpochStorageReaderImplTest {
    private Connection connection;
    private DSLContext dsl;
    private BFEpochStorageReaderImpl reader;

    @BeforeEach
    void setUp() throws SQLException {
        connection = DriverManager.getConnection("jdbc:h2:mem:bf_epoch_reader;DATABASE_TO_LOWER=TRUE", "sa", "");
        dsl = DSL.using(connection, SQLDialect.H2);
        reader = new BFEpochStorageReaderImpl(dsl);

        dsl.execute("create table adapot (epoch int primary key, total_epoch_stake numeric(38))");
        dsl.execute("create table epoch_stake (epoch int, address varchar(255), amount numeric(38), primary key (epoch, address))");
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void getActiveStakesByEpochs_readsStoredSnapshotTotal() {
        //The AdaPot job of epoch 500 snapshots epoch 499, which is the active stake of epoch 501
        dsl.execute("insert into adapot values (500, 1000)");
        //A differing snapshot sum shows the stored total is read instead of summing epoch_stake
        dsl.execute("insert into epoch_stake values (499, 'stake1', 7)");

        var activeStakes = reader.getActiveStakesByEpochs(List.of(501));

        assertThat(activeStakes).containsExactlyEntriesOf(Map.of(501, BigInteger.valueOf(1000)));
    }

    @Test
    void getActiveStakesByEpochs_sumsSnapshotWhenTotalIsNotStored() {
        dsl.execute("insert into adapot values (500, 1000), (501, null)");
        dsl.execute("insert into epoch_stake values (500, 'stake1', 30), (500, 'stake2', 12)");

        var activeStakes = reader.getActiveStakesByEpochs(List.of(501, 502, 503));

        assertThat(activeStakes)
                .hasSize(2)
                .containsEntry(501, BigInteger.valueOf(1000))
                .containsEntry(502, BigInteger.valueOf(42))
                .doesNotContainKey(503);
    }
}
