package com.bloxbean.cardano.yaci.store.snapshot.validate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class TableScannerIT {
    private final String url = System.getenv("SNAPSHOT_IT_JDBC_URL");
    private final String user = System.getenv().getOrDefault("SNAPSHOT_IT_USER", "postgres");
    private final String password = System.getenv().getOrDefault("SNAPSHOT_IT_PASSWORD", "");
    private final String schema = "yaci_scan_it_" + UUID.randomUUID().toString().replace("-", "");
    private Connection pg;

    @BeforeEach
    void createSchema() throws SQLException {
        pg = connect();
        String s = schema + ".";
        exec("CREATE SCHEMA " + schema,
                "CREATE TABLE " + s + "address_utxo (tx_hash text, output_index int, slot bigint, block bigint,"
                        + " lovelace_amount bigint, amounts jsonb)",
                "INSERT INTO " + s + "address_utxo VALUES"
                        // good
                        + " ('a', 0, 10, 1, 5, '[{\"unit\":\"lovelace\",\"quantity\":5},{\"unit\":\"x\",\"quantity\":9}]'),"
                        // lovelace_amount disagrees with amounts
                        + " ('b', 0, 10, 1, 6, '[{\"unit\":\"lovelace\",\"quantity\":5}]'),"
                        // two lovelace entries: fails the single-lovelace check, and its total is not compared
                        + " ('c', 0, 10, 1, 5, '[{\"unit\":\"lovelace\",\"quantity\":2},{\"unit\":\"lovelace\",\"quantity\":3}]'),"
                        // amounts not an array, no block, beyond the cutoff, null tx_hash
                        + " (null, 0, 99, null, 0, '{\"unit\":\"lovelace\"}'),"
                        // empty amounts
                        + " ('e', 0, 10, 1, 0, '[]'),"
                        // null amounts
                        + " ('f', 0, 10, 1, 0, null)",
                "CREATE TABLE " + s + "block (number bigint, hash text, slot bigint)",
                "INSERT INTO " + s + "block SELECT g, 'h' || g, g * 10 FROM generate_series(1, 20) g");
    }

    @AfterEach
    void dropSchema() throws SQLException {
        exec("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        pg.close();
    }

    @Test
    void readsEveryCheckInOnePass() throws Exception {
        TableScanner.Result r = TableScanner.scanOne(pg, schema, utxo());

        assertThat(r.rows()).isEqualTo(6);
        assertThat(r.nulls()).containsExactly(1L, 0L);
        assertThat(r.beyondCutoff()).isEqualTo(1);
        // non-array, empty and null amounts
        assertThat(r.utxo().badAmounts()).isEqualTo(3);
        // 'b' (6 vs 5) and 'c' (two entries); the non-array row (0 vs 0), '[]' and null compare equal to 0
        assertThat(r.utxo().lovelaceMismatch()).isEqualTo(2);
        assertThat(r.utxo().multiLovelace()).isEqualTo(1);
        assertThat(r.utxo().missingBlock()).isEqualTo(1);
    }

    @Test
    void parallelScanMatchesTheSerialOne() throws Exception {
        List<TableScanner.Target> targets = List.of(utxo(),
                new TableScanner.Target("block", List.of("hash"), "slot", 150, false, 20));
        List<String> said = new ArrayList<>();

        Map<String, TableScanner.Result> serial = TableScanner.scan(schema, targets, pg, null, 1, m -> { });
        Map<String, TableScanner.Result> parallel = TableScanner.scan(schema, targets, pg, this::connect, 2, said::add);

        assertThat(parallel).isEqualTo(serial);
        assertThat(parallel.get("block").beyondCutoff()).isEqualTo(5);
        assertThat(said).first().asString().isEqualTo("Scanning 2 table(s) with 2 connection(s)");
        assertThat(said).last().asString().startsWith("Scanned 2 table(s) in ");
    }

    @Test
    void theAddressUtxoScanCanRunInParallel() throws Exception {
        // A subquery that refers to the row would keep the JSON work in the leader process.
        exec("SET max_parallel_workers_per_gather = 4", "SET parallel_setup_cost = 0", "SET parallel_tuple_cost = 0",
                "SET min_parallel_table_scan_size = 0");
        StringBuilder plan = new StringBuilder();
        try (Statement st = pg.createStatement();
             var rs = st.executeQuery("EXPLAIN " + TableScanner.sql(schema, utxo()))) {
            while (rs.next()) {
                plan.append(rs.getString(1)).append('\n');
            }
        }
        assertThat(plan.toString()).contains("Gather").contains("Partial Aggregate").doesNotContain("SubPlan");
    }

    private TableScanner.Target utxo() {
        return new TableScanner.Target("address_utxo", List.of("tx_hash", "output_index"), "slot", 50, true, 6);
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
}
