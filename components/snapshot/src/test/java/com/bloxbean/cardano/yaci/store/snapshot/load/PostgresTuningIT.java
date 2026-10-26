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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class PostgresTuningIT {
    private static final List<String> SETTINGS = List.of("max_wal_size", "checkpoint_timeout", "wal_compression");

    private final String schema = "yaci_tune_it_" + UUID.randomUUID().toString().replace("-", "");
    /** A role that is not a superuser, entered with SET ROLE so the test needs no second login. */
    private final String role = schema + "_role";
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
        exec("RESET ROLE");
        exec("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        if (exists("SELECT 1 FROM pg_roles WHERE rolname = '" + role + "'")) {
            for (String name : SETTINGS) {
                exec("REVOKE ALTER SYSTEM ON PARAMETER " + name + " FROM " + role);
            }
            exec("REVOKE EXECUTE ON FUNCTION pg_reload_conf() FROM " + role);
            exec("DROP ROLE " + role);
        }
        pg.close();
    }

    @Test
    void aRoleMissingAnyPrivilegeTheTuningNeedsIsRefusedInPreflight() throws Exception {
        exec("CREATE ROLE " + role + " NOLOGIN");
        assertThat(asRole(() -> PostgresTuning.cannotApply(pg)))
                .contains("ALTER SYSTEM on max_wal_size").contains("pg_reload_conf()").contains("pg_read_all_settings");

        for (String name : SETTINGS) {
            exec("GRANT ALTER SYSTEM ON PARAMETER " + name + " TO " + role);
        }
        assertThat(asRole(() -> PostgresTuning.cannotApply(pg)))
                .doesNotContain("ALTER SYSTEM on").contains("pg_reload_conf()").contains("pg_read_all_settings");

        exec("GRANT EXECUTE ON FUNCTION pg_reload_conf() TO " + role);
        assertThat(asRole(() -> PostgresTuning.cannotApply(pg)))
                .doesNotContain("pg_reload_conf()").contains("pg_read_all_settings");

        exec("GRANT pg_read_all_settings TO " + role);
        assertThat(asRole(() -> PostgresTuning.cannotApply(pg))).isNull();
    }

    /**
     * The failure the preflight now catches, forced: settings written to postgresql.auto.conf, then
     * pg_reload_conf() denied. The importer's restore puts back the settings and autovacuum.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_ALTER_SYSTEM", matches = "true")
    void aFailureAfterTheSettingsChangedRestoresThemAndAutovacuum() throws Exception {
        Map<String, String> autoConfBefore = autoConf();
        Map<String, String> before = settings();
        exec("CREATE ROLE " + role + " NOLOGIN");
        for (String name : SETTINGS) {
            exec("GRANT ALTER SYSTEM ON PARAMETER " + name + " TO " + role);
        }
        exec("GRANT pg_read_all_settings TO " + role);
        exec("GRANT USAGE, CREATE ON SCHEMA " + schema + " TO " + role);
        exec("SET ROLE " + role);
        exec("CREATE TABLE " + schema + ".t (id int)");
        Autovacuum.disable(pg, schema, List.of("t"));
        assertThatThrownBy(() -> PostgresTuning.apply(pg, schema)).hasMessageContaining("pg_reload_conf");
        exec("RESET ROLE");
        assertThat(autoConf()).isNotEqualTo(autoConfBefore);
        assertThat(PostgresTuning.pending(pg, schema)).isTrue();

        SnapshotImporter.restoreServerState(pg, schema, List.of("t"));

        reload();
        assertThat(autoConf()).isEqualTo(autoConfBefore);
        assertThat(settings()).isEqualTo(before);
        assertThat(PostgresTuning.pending(pg, schema)).isFalse();
        assertThat(Autovacuum.disabledTables(pg, schema)).isEmpty();
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_ALTER_SYSTEM", matches = "true")
    void aValueAlreadyInAutoConfIsPutBackNotReset() throws Exception {
        Map<String, String> autoConfBefore = autoConf();
        try {
            exec("ALTER SYSTEM SET checkpoint_timeout = '10min'");
            exec("SELECT pg_reload_conf()");
            reload();
            PostgresTuning.apply(pg, schema);
            reload();
            assertThat(setting("checkpoint_timeout")).isEqualTo("15min");

            PostgresTuning.restore(pg, schema);
            reload();
            assertThat(setting("checkpoint_timeout")).isEqualTo("10min");
            assertThat(autoConf()).containsEntry("checkpoint_timeout", "600s");
        } finally {
            String original = autoConfBefore.get("checkpoint_timeout");
            exec(original == null ? "ALTER SYSTEM RESET checkpoint_timeout"
                    : "ALTER SYSTEM SET checkpoint_timeout = '" + original + "'");
            exec("SELECT pg_reload_conf()");
        }
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

    private interface Query<T> {
        T run() throws SQLException;
    }

    private <T> T asRole(Query<T> q) throws SQLException {
        exec("SET ROLE " + role);
        try {
            return q.run();
        } finally {
            exec("RESET ROLE");
        }
    }

    /** What postgresql.auto.conf holds on disk for the tuned settings, reloaded or not. */
    private Map<String, String> autoConf() throws SQLException {
        Map<String, String> out = new TreeMap<>();
        try (Statement st = pg.createStatement(); ResultSet rs = st.executeQuery("SELECT name, setting"
                + " FROM pg_file_settings WHERE name IN ('max_wal_size', 'checkpoint_timeout', 'wal_compression')"
                + " AND sourcefile LIKE '%postgresql.auto.conf'")) {
            while (rs.next()) {
                out.put(rs.getString(1), rs.getString(2));
            }
        }
        return out;
    }

    private Map<String, String> settings() throws SQLException {
        Map<String, String> out = new TreeMap<>();
        for (String name : SETTINGS) {
            out.put(name, setting(name));
        }
        return out;
    }

    private boolean exists(String sql) throws SQLException {
        try (Statement st = pg.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
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
