package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedViewBuilderTest {

    @Test
    void derivesDuckLakeDateColumnFromPostgresPartitionColumn() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:duckdb:");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE VIEW parquet_block AS "
                    + "SELECT CAST('2026-01-01' AS DATE) AS date, "
                    + "1::BIGINT AS slot, to_timestamp(1) AS block_time");
            statement.execute("CREATE TABLE block(slot BIGINT, block_time BIGINT)");

            String database;
            try (ResultSet resultSet = statement.executeQuery("SELECT current_database()")) {
                resultSet.next();
                database = resultSet.getString(1);
            }

            String sql = UnifiedViewBuilder.buildUnifiedViewSql(
                    "block", database, "main", 100, "block_time", connection);

            assertNotNull(sql);
            assertTrue(sql.contains("to_timestamp(\"block_time\")"));
            assertTrue(sql.contains("AS \"date\""));
        }
    }
}
