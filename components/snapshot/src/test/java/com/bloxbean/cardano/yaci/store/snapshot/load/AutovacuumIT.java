package com.bloxbean.cardano.yaci.store.snapshot.load;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class AutovacuumIT {
    private final String schema = "yaci_av_it_" + UUID.randomUUID().toString().replace("-", "");
    private Connection pg;

    @BeforeEach
    void createSchema() throws SQLException {
        pg = DriverManager.getConnection(System.getenv("SNAPSHOT_IT_JDBC_URL"),
                System.getenv().getOrDefault("SNAPSHOT_IT_USER", "postgres"),
                System.getenv().getOrDefault("SNAPSHOT_IT_PASSWORD", ""));
        String s = schema + ".";
        exec("CREATE SCHEMA " + schema,
                "CREATE TABLE " + s + "utxo (id int, body text)",
                "CREATE TABLE " + s + "stake (epoch int, amount bigint) PARTITION BY RANGE (epoch)",
                "CREATE TABLE " + s + "stake_1 PARTITION OF " + s + "stake FOR VALUES FROM (1) TO (2)",
                "CREATE TABLE " + s + "stake_2 PARTITION OF " + s + "stake FOR VALUES FROM (2) TO (3)",
                "CREATE TABLE " + s + "other (id int)");
    }

    @AfterEach
    void dropSchema() throws SQLException {
        exec("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        pg.close();
    }

    @Test
    void switchesOffEveryLeafTableAndBackOn() throws Exception {
        assertThat(Autovacuum.disable(pg, schema, List.of("utxo", "stake"))).isEqualTo(3);
        assertThat(Autovacuum.disabledTables(pg, schema)).containsExactly("stake_1", "stake_2", "utxo");

        assertThat(Autovacuum.restore(pg, schema, List.of("utxo", "stake"))).isEqualTo(3);
        assertThat(Autovacuum.disabledTables(pg, schema)).isEmpty();
    }

    private void exec(String... sql) throws SQLException {
        try (Statement st = pg.createStatement()) {
            for (String s : sql) {
                st.execute(s);
            }
        }
    }
}
