package com.bloxbean.cardano.yaci.store.analytics.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DuckDbReadConnectionProviderTest {

    @TempDir
    Path tempDirectory;

    @Test
    void sandboxKeepsParquetViewsAndRefreshWorkingButBlocksOtherFiles() throws Exception {
        Path analyticsDirectory = Files.createDirectory(tempDirectory.resolve("analytics"));
        Path secret = Files.writeString(tempDirectory.resolve("secret.csv"), "secret\nvalue\n");

        TestProvider provider = new TestProvider();
        provider.initialize(analyticsDirectory);

        assertEquals(42, provider.querySingleValue());

        try (Connection connection = provider.getReadConnection();
             Statement statement = connection.createStatement()) {
            assertThrows(SQLException.class,
                    () -> statement.executeQuery("SELECT * FROM sniff_csv('" + sqlPath(secret) + "')"));
            assertThrows(SQLException.class,
                    () -> statement.execute("SET enable_external_access = true"));
        }

        Files.copy(analyticsDirectory.resolve("data.parquet"),
                analyticsDirectory.resolve("new-partition.parquet"));
        provider.refreshView();

        assertEquals(84, provider.querySingleValue());
    }

    private static String sqlPath(Path path) {
        return path.toAbsolutePath().normalize().toString().replace("'", "''");
    }

    private static final class TestProvider extends DuckDbReadConnectionProvider {
        private Path parquetGlob;

        private TestProvider() {
            super(1, null, 1, 5);
        }

        private void initialize(Path analyticsDirectory) throws SQLException {
            Path parquetFile = analyticsDirectory.resolve("data.parquet");
            parquetGlob = analyticsDirectory.resolve("*.parquet");
            executeOnParent("CREATE TABLE seed AS SELECT 42 AS value");
            executeOnParent("COPY seed TO '" + sqlPath(parquetFile) + "' (FORMAT PARQUET)");
            refreshView();
            lockDown(analyticsDirectory);
        }

        private void refreshView() throws SQLException {
            executeOnParent("CREATE OR REPLACE VIEW analytics AS SELECT * FROM read_parquet('"
                    + sqlPath(parquetGlob) + "')");
        }

        private int querySingleValue() throws SQLException {
            try (Connection connection = getReadConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT SUM(value) FROM analytics")) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}
