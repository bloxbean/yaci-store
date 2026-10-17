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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class DeferredIndexesIT {
    private final String url = System.getenv("SNAPSHOT_IT_JDBC_URL");
    private final String user = System.getenv().getOrDefault("SNAPSHOT_IT_USER", "postgres");
    private final String password = System.getenv().getOrDefault("SNAPSHOT_IT_PASSWORD", "");
    private final String schema = "yaci_defer_it_" + UUID.randomUUID().toString().replace("-", "");
    private final List<String> said = new ArrayList<>();
    private Connection pg;

    @BeforeEach
    void createSchema() throws SQLException {
        pg = connect();
        exec("CREATE SCHEMA " + schema,
                // A partitioned table with a key, a parent index and an existing partition.
                "CREATE TABLE " + schema + ".stake (epoch int NOT NULL, address text NOT NULL, amount bigint,"
                        + " PRIMARY KEY (epoch, address)) PARTITION BY RANGE (epoch)",
                "CREATE TABLE " + schema + ".stake_1 PARTITION OF " + schema + ".stake FOR VALUES FROM (1) TO (2)",
                "CREATE INDEX idx_stake_address ON " + schema + ".stake (address)",
                // A plain table with a key whose columns are not in attribute order, a partial index
                // and a unique index that is not a constraint.
                "CREATE TABLE " + schema + ".utxo (slot bigint, tx_hash text NOT NULL, output_index int NOT NULL,"
                        + " PRIMARY KEY (output_index, tx_hash))",
                "CREATE INDEX idx_utxo_slot ON " + schema + ".utxo (slot) WHERE slot > 0",
                "CREATE UNIQUE INDEX uq_utxo_slot_hash ON " + schema + ".utxo (slot, tx_hash)",
                // A table that is not loaded keeps its index.
                "CREATE TABLE " + schema + ".other (id int PRIMARY KEY)");
    }

    @AfterEach
    void dropSchema() throws SQLException {
        exec("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        pg.close();
    }

    @Test
    void dropsCapturedIndexesAndRebuildsThemExactly() throws Exception {
        Map<String, String> before = definitions();
        DeferredIndexes deferred = new DeferredIndexes(pg, schema);

        List<DeferredIndexes.Entry> pending = deferred.defer("s1", List.of("stake", "utxo"), said::add);

        assertThat(pending).extracting(DeferredIndexes.Entry::name).containsExactlyInAnyOrder(
                "stake_pkey", "idx_stake_address", "utxo_pkey", "idx_utxo_slot", "uq_utxo_slot_hash");
        assertThat(definitions()).containsOnlyKeys("other_pkey");
        // Schema checks on a resumed import still see the key the table will get back.
        assertThat(new PgSchema(pg, schema).primaryKey("utxo")).containsExactly("tx_hash", "output_index");

        // A partition created after the drop gets the indexes from its parent on rebuild.
        exec("CREATE TABLE " + schema + ".stake_2 PARTITION OF " + schema + ".stake FOR VALUES FROM (2) TO (3)",
                "INSERT INTO " + schema + ".stake SELECT 1 + g % 2, 'a' || g, g FROM generate_series(1, 1000) g",
                "INSERT INTO " + schema + ".utxo SELECT g, 'h' || g, g % 3 FROM generate_series(1, 1000) g");

        deferred.rebuild("s1", 3, "64MB", this::connect, said::add);

        // Partition indexes come back under the same generated names, plus the new partition's.
        assertThat(definitions()).containsAllEntriesOf(before).hasSize(before.size() + 2);
        assertThat(deferred.problems("s1")).isEmpty();
        assertThat(count("SELECT count(*) FROM pg_index x JOIN pg_class c ON c.oid = x.indrelid"
                + " WHERE c.relname = 'stake_2' AND c.relnamespace = '" + schema + "'::regnamespace")).isEqualTo(2);
        assertThat(count("SELECT count(*) FROM pg_index x JOIN pg_class c ON c.oid = x.indexrelid"
                + " WHERE c.relnamespace = '" + schema + "'::regnamespace AND NOT x.indisvalid")).isZero();
        assertThat(said).anyMatch(s -> s.startsWith("Rebuilt 5 key(s) and index(es)"));
    }

    @Test
    void theLargestTableOnDiskStartsFirstWithMoreParallelWorkers() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(count("SELECT current_setting('max_parallel_maintenance_workers')::int") < 4
                && count("SELECT least(current_setting('max_parallel_workers')::int,"
                + " current_setting('max_worker_processes')::int)") >= 8);
        DeferredIndexes deferred = new DeferredIndexes(pg, schema);
        deferred.defer("s1", List.of("stake", "utxo"), said::add);
        // utxo has fewer rows than stake, but wider ones.
        exec("INSERT INTO " + schema + ".stake SELECT 1, 'a' || g, g FROM generate_series(1, 20000) g",
                "INSERT INTO " + schema + ".utxo SELECT g, repeat('h', 500) || g, 0 FROM generate_series(1, 5000) g");

        deferred.rebuild("s1", 2, null, this::connect, said::add);

        assertThat(said).contains("Starting key utxo_pkey on utxo first, with up to 4 parallel workers");
        assertThat(deferred.problems("s1")).isEmpty();
    }

    @Test
    void resumeUsesTheJournalAndOnlyRebuildsWhatIsMissing() throws Exception {
        DeferredIndexes deferred = new DeferredIndexes(pg, schema);
        deferred.defer("s1", List.of("utxo"), said::add);
        deferred.rebuild("s1", 1, null, this::connect, said::add);
        // A crash after one index came back: drop it again behind the journal's back.
        exec("DROP INDEX " + schema + ".idx_utxo_slot",
                "UPDATE " + schema + "." + DeferredIndexes.TABLE + " SET built = false WHERE name = 'idx_utxo_slot'");

        List<DeferredIndexes.Entry> pending = new DeferredIndexes(pg, schema).defer("s1", List.of("utxo"), said::add);

        assertThat(pending).extracting(DeferredIndexes.Entry::name).containsExactly("idx_utxo_slot");
        assertThat(definitions()).containsKeys("utxo_pkey", "uq_utxo_slot_hash");
        deferred.rebuild("s1", 2, null, this::connect, said::add);
        assertThat(deferred.problems("s1")).isEmpty();
    }

    @Test
    void duplicateRowsFailOnTheKeyAndLeaveItPending() throws Exception {
        DeferredIndexes deferred = new DeferredIndexes(pg, schema);
        deferred.defer("s1", List.of("utxo"), said::add);
        exec("INSERT INTO " + schema + ".utxo VALUES (1, 'h', 0), (2, 'h', 0)");

        assertThatThrownBy(() -> deferred.rebuild("s1", 2, null, this::connect, said::add))
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("utxo_pkey on utxo")
                .hasMessageContaining("duplicate keys");
        assertThat(deferred.problems("s1")).anyMatch(p -> p.contains("utxo_pkey") && p.contains("not been rebuilt"));
    }

    @Test
    void reportsAnIndexThatChangedAfterTheRebuild() throws Exception {
        DeferredIndexes deferred = new DeferredIndexes(pg, schema);
        deferred.defer("s1", List.of("utxo"), said::add);
        deferred.rebuild("s1", 1, null, this::connect, said::add);
        exec("DROP INDEX " + schema + ".idx_utxo_slot",
                "CREATE INDEX idx_utxo_slot ON " + schema + ".utxo (slot)");

        assertThat(deferred.problems("s1")).singleElement().asString().contains("idx_utxo_slot", "differs");
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void exec(String... sql) throws SQLException {
        try (Statement st = pg.createStatement()) {
            for (String s : sql) {
                st.execute(s);
            }
        }
    }

    private long count(String sql) throws SQLException {
        try (Statement st = pg.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    /** Index name to definition for every index in the schema, journal excluded. */
    private Map<String, String> definitions() throws SQLException {
        Map<String, String> out = new java.util.TreeMap<>();
        try (Statement st = pg.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT c.relname, pg_get_indexdef(c.oid) FROM pg_index x JOIN pg_class c ON c.oid = x.indexrelid"
                        + " JOIN pg_class t ON t.oid = x.indrelid WHERE c.relnamespace = '" + schema
                        + "'::regnamespace AND t.relname NOT LIKE '\\_yaci%'")) {
            while (rs.next()) {
                out.put(rs.getString(1), rs.getString(2));
            }
        }
        return out;
    }
}
