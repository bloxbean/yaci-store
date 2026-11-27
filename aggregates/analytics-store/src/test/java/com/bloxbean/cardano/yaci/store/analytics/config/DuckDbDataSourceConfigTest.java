package com.bloxbean.cardano.yaci.store.analytics.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuckDbDataSourceConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void appliesMemoryAndThreadSettingsToTheWriter() throws Exception {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.getDuckdb().setMemoryLimit("128MB");
        properties.getDuckdb().setThreads(2);
        properties.setExportPath(tempDir.resolve("analytics").toString());

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

    @Test
    void writerSpillsIntoItsOwnDirectoryUnderTheExportPath() throws Exception {
        AnalyticsStoreProperties properties = new AnalyticsStoreProperties();
        properties.getDuckdb().setMemoryLimit("128MB");
        properties.setExportPath(tempDir.resolve("analytics").toString());

        DuckDbDataSourceConfig config = new DuckDbDataSourceConfig(properties);
        Path temp;
        try (HikariDataSource dataSource = (HikariDataSource) config.duckDbWriterDataSource()) {
            try (Connection connection = dataSource.getConnection();
                 Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT current_setting('temp_directory')")) {
                result.next();
                temp = Path.of(result.getString(1));
            }
            assertEquals(tempDir.resolve("analytics/.tmp").toAbsolutePath().normalize(), temp.getParent());
            assertTrue(temp.getFileName().toString().startsWith("writer-"), temp.toString());
            assertTrue(Files.isDirectory(temp));
        }
        assertFalse(Files.exists(temp), "closing the pool removes the writer's temp directory");
    }
}
