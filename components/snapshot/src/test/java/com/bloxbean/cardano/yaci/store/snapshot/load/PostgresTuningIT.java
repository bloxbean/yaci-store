package com.bloxbean.cardano.yaci.store.snapshot.load;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class PostgresTuningIT {
    private final String schema = "yaci_tune_it_" + UUID.randomUUID().toString().replace("-", "");
    private Connection pg;

    @BeforeEach
    void connect() throws SQLException {
        pg = DriverManager.getConnection(System.getenv("SNAPSHOT_IT_JDBC_URL"),
                System.getenv().getOrDefault("SNAPSHOT_IT_USER", "postgres"),
                System.getenv().getOrDefault("SNAPSHOT_IT_PASSWORD", ""));
        exec("CREATE SCHEMA " + schema);
    }

    @AfterEach
    void close() throws SQLException {
        exec("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        pg.close();
    }

    @Test
    void adviceNamesEverySettingBelowItsRecommendation() throws Exception {
        assertThat(PostgresTuning.advice(pg)).allSatisfy(line -> assertThat(line).contains("--tune-postgres"));
        assertThat(PostgresTuning.pending(pg, schema)).isFalse();
    }

    @Test
    void parallelismAdviceOnlyForManyWorkersOnASmallPool() throws Exception {
        assertThat(PostgresTuning.parallelismAdvice(pg, 1)).isEmpty();
        long pool = Math.min(Long.parseLong(setting("max_worker_processes")), Long.parseLong(setting("max_parallel_workers")));
        assertThat(PostgresTuning.parallelismAdvice(pg, (int) pool)).singleElement().asString()
                .contains("needs a PostgreSQL restart");
    }

    /** Changes the server's configuration and puts it back, so it only runs when asked for. */
    @Test
    @EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_ALTER_SYSTEM", matches = "true")
    void appliesForTheImportAndRestoresTheOriginals() throws Exception {
        assertThat(PostgresTuning.cannotApply(pg)).isNull();
        String walBefore = setting("max_wal_size");
        String timeoutBefore = setting("checkpoint_timeout");
        try {
            PostgresTuning.apply(pg, schema);
            reload();
            assertThat(PostgresTuning.advice(pg)).isEmpty();
            assertThat(PostgresTuning.pending(pg, schema)).isTrue();
            // A resumed import keeps the originals recorded by the first run.
            PostgresTuning.apply(pg, schema);
        } finally {
            PostgresTuning.restore(pg, schema);
        }
        reload();
        assertThat(setting("max_wal_size")).isEqualTo(walBefore);
        assertThat(setting("checkpoint_timeout")).isEqualTo(timeoutBefore);
        assertThat(PostgresTuning.pending(pg, schema)).isFalse();
    }

    /** pg_reload_conf only signals; a fresh connection sees the reloaded values. */
    private void reload() throws Exception {
        Thread.sleep(500);
        pg.close();
        connect0();
    }

    private void connect0() throws SQLException {
        pg = DriverManager.getConnection(System.getenv("SNAPSHOT_IT_JDBC_URL"),
                System.getenv().getOrDefault("SNAPSHOT_IT_USER", "postgres"),
                System.getenv().getOrDefault("SNAPSHOT_IT_PASSWORD", ""));
    }

    private String setting(String name) throws SQLException {
        try (Statement st = pg.createStatement(); ResultSet rs = st.executeQuery("SELECT current_setting('" + name + "')")) {
            rs.next();
            return rs.getString(1);
        }
    }

    private void exec(String sql) throws SQLException {
        try (Statement st = pg.createStatement()) {
            st.execute(sql);
        }
    }
}
