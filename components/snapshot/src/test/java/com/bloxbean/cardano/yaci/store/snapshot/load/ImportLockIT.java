package com.bloxbean.cardano.yaci.store.snapshot.load;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Imports into two schemas of one database run side by side; into one schema, or with server tuning, they do not. */
@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class ImportLockIT {

    @Test
    void oneImportPerSchemaAndATunedImportRunsAlone() throws Exception {
        try (Connection a = connect(); Connection b = connect(); Connection c = connect(); Connection d = connect()) {
            SnapshotImporter.acquireAdvisoryLocks(a, "lock_it_one", false);

            assertThatCode(() -> SnapshotImporter.acquireAdvisoryLocks(b, "lock_it_two", false)).doesNotThrowAnyException();
            assertThatThrownBy(() -> SnapshotImporter.acquireAdvisoryLocks(c, "lock_it_one", false))
                    .hasMessageContaining("schema 'lock_it_one'");
            assertThatThrownBy(() -> SnapshotImporter.acquireAdvisoryLocks(d, "lock_it_three", true))
                    .hasMessageContaining("runs alone");
        }
        try (Connection tuned = connect(); Connection other = connect()) {
            SnapshotImporter.acquireAdvisoryLocks(tuned, "lock_it_one", true);
            assertThatThrownBy(() -> SnapshotImporter.acquireAdvisoryLocks(other, "lock_it_two", false))
                    .hasMessageContaining("--tune-postgres is running");
        }
        // Closing the sessions released every lock
        try (Connection again = connect()) {
            assertThatCode(() -> SnapshotImporter.acquireAdvisoryLocks(again, "lock_it_one", true)).doesNotThrowAnyException();
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(System.getenv("SNAPSHOT_IT_JDBC_URL"),
                System.getenv().getOrDefault("SNAPSHOT_IT_USER", "postgres"),
                System.getenv().getOrDefault("SNAPSHOT_IT_PASSWORD", ""));
    }
}
