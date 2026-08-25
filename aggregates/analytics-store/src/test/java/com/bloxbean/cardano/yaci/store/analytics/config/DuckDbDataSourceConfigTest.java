package com.bloxbean.cardano.yaci.store.analytics.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DuckDbDataSourceConfigTest {

    @Test
    void appliesMemoryAndThreadSettingsToTheWriter() throws Exception {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.getDuckdb().setMemoryLimit("128MB");
        properties.getDuckdb().setThreads(2);

        DuckDbDataSourceConfig config = new DuckDbDataSourceConfig(properties);
        try (HikariDataSource dataSource = (HikariDataSource) config.duckDbWriterDataSource();
             Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT current_setting('threads'), current_setting('memory_limit')")) {
            result.next();
            assertEquals(2, result.getInt(1));
            assertEquals("122.0 MiB", result.getString(2));
        }
    }
}
