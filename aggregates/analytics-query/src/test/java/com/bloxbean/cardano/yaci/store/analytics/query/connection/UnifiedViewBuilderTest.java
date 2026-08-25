package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.query.connection.CutoffSlotResolver.Cutoff;
import com.bloxbean.cardano.yaci.store.analytics.query.connection.UnifiedViewBuilder.Federation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The "PostgreSQL" side is stood in by tables in the DuckDB instance's own catalog (the builder
 * only needs {@code DESCRIBE} to work on {@code <db>.<schema>.<table>}); the resulting view SQL is
 * executed to prove both halves union cleanly.
 */
class UnifiedViewBuilderTest {

    private Connection connection;
    private String database;

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection("jdbc:duckdb:");
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT current_database()")) {
            resultSet.next();
            database = resultSet.getString(1);
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    void dailyTableUsesCutoffSlotAndDerivesDateFromPostgresPartitionColumn() throws Exception {
        exec("CREATE VIEW parquet_block AS SELECT CAST('2026-01-01' AS DATE) AS date, 10::BIGINT AS slot, to_timestamp(1) AS block_time");
        exec("CREATE TABLE block(slot BIGINT, block_time BIGINT)");
        exec("INSERT INTO block VALUES (1, 100), (200, 5000)");

        String sql = UnifiedViewBuilder.buildUnifiedViewSql(
                "block", database, "main", new Cutoff(100, -1, 10, -1),
                new Federation("slot", PartitionStrategy.DAILY, Map.of()), "block_time", connection);

        assertNotNull(sql);
        assertTrue(sql.contains("WHERE \"slot\" BETWEEN 10 AND 100"), sql);
        assertTrue(sql.contains("WHERE (\"slot\" < 10 OR \"slot\" > 100)"), sql);
        assertTrue(sql.contains("to_timestamp(\"block_time\")"), sql);
        assertTrue(sql.contains("AS \"date\""), sql);
        // Rows before an admin-range export remain on PG; the exported interval and live tail
        // are both present as well.
        assertEquals(3, count(sql, "block_unified"));
    }

    @Test
    void boundaryColumnOverrideIsUsedOnBothSides() throws Exception {
        // tx_input-like: keyed by the spend slot, no 'slot' column at all
        exec("CREATE VIEW parquet_tx_input AS SELECT 'h' AS tx_hash, 0::INTEGER AS output_index, "
                + "10::BIGINT AS spent_at_slot, CAST('2026-01-01' AS DATE) AS date, to_timestamp(10) AS spent_block_time");
        exec("CREATE TABLE tx_input(tx_hash VARCHAR, output_index INTEGER, spent_at_slot BIGINT, spent_block_time BIGINT)");
        exec("INSERT INTO tx_input VALUES ('h', 0, 10, 10), ('h2', 1, 500, 500)");

        String sql = UnifiedViewBuilder.buildUnifiedViewSql("tx_input", database, "main", new Cutoff(100, -1),
                new Federation("spent_at_slot", PartitionStrategy.DAILY, Map.of()), "spent_block_time", connection);

        assertNotNull(sql);
        assertTrue(sql.contains("WHERE \"spent_at_slot\" <= 100"), sql);
        assertTrue(sql.contains("WHERE \"spent_at_slot\" > 100"), sql);
        assertEquals(2, count(sql, "tx_input_unified"));
    }

    @Test
    void epochTableUsesCutoffEpochAndMapsRenamedSourceColumns() throws Exception {
        // reward-like: export renames earned_epoch -> epoch, partitions are epochs, no slot boundary
        exec("CREATE VIEW parquet_reward AS SELECT 'addr' AS address, 300::INTEGER AS epoch, "
                + "302::INTEGER AS spendable_epoch, 5::DECIMAL(38,0) AS amount, 1::BIGINT AS slot");
        exec("CREATE TABLE reward(address VARCHAR, earned_epoch INTEGER, spendable_epoch INTEGER, amount BIGINT, slot BIGINT)");
        exec("INSERT INTO reward VALUES ('addr', 299, 301, 3, 0), ('addr', 300, 302, 5, 1), ('addr', 301, 303, 7, 9999)");

        String sql = UnifiedViewBuilder.buildUnifiedViewSql(
                "reward", database, "main", new Cutoff(123_456, 300, 100_000, 300),
                new Federation("epoch", PartitionStrategy.EPOCH, Map.of("epoch", "earned_epoch")), "slot", connection);

        assertNotNull(sql);
        assertTrue(sql.contains("WHERE \"epoch\" BETWEEN 300 AND 300"), sql);
        assertTrue(sql.contains("WHERE (\"earned_epoch\" < 300 OR \"earned_epoch\" > 300)"), sql);
        assertTrue(sql.contains("\"earned_epoch\" AS \"epoch\""), sql);     // mapped in the select list too
        assertTrue(sql.contains("CAST(\"amount\" AS DECIMAL(38,0)) AS \"amount\""), sql);
        assertEquals(3, count(sql, "reward_unified"));
    }

    @Test
    void tableWithoutBoundaryIsNotFederated() throws Exception {
        exec("CREATE VIEW parquet_epoch_param AS SELECT 300::INTEGER AS epoch, 'p' AS params, 1::BIGINT AS slot");
        exec("CREATE TABLE epoch_param(epoch INTEGER, params VARCHAR, slot BIGINT)");

        assertNull(UnifiedViewBuilder.buildUnifiedViewSql("epoch_param", database, "main", new Cutoff(1, 300),
                new Federation(null, PartitionStrategy.EPOCH, Map.of()), "slot", connection));
        assertNull(UnifiedViewBuilder.buildUnifiedViewSql("epoch_param", database, "main", new Cutoff(1, 300),
                null, "slot", connection));
        // declared boundary column that the export does not have
        assertNull(UnifiedViewBuilder.buildUnifiedViewSql("epoch_param", database, "main", new Cutoff(1, 300),
                new Federation("spent_at_slot", PartitionStrategy.DAILY, Map.of()), "slot", connection));
    }

    @Test
    void unmappedExportedColumnMissingInPostgresDisablesFederation() throws Exception {
        exec("CREATE VIEW parquet_datum AS SELECT 'h' AS hash, 1::BIGINT AS slot, CAST('2026-01-01' AS DATE) AS block_date");
        exec("CREATE TABLE datum(hash VARCHAR, slot BIGINT)");

        assertNull(UnifiedViewBuilder.buildUnifiedViewSql("datum", database, "main", new Cutoff(100, -1),
                new Federation("slot", PartitionStrategy.DAILY, Map.of()), "slot", connection));
    }

    private void exec(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /** Execute the generated CREATE VIEW under a different name and count its rows. */
    private long count(String createViewSql, String viewName) throws SQLException {
        String renamed = createViewSql.replaceFirst("CREATE OR REPLACE VIEW \"[^\"]+\"",
                "CREATE OR REPLACE VIEW \"" + viewName + "\"");
        exec(renamed);
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT count(*) FROM \"" + viewName + "\"")) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
