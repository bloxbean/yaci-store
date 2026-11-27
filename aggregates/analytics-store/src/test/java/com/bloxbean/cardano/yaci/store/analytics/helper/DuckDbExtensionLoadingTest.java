package com.bloxbean.cardano.yaci.store.analytics.helper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Extension loading must work without network access when the extension is already installed
 * in one of the configured directories, and fail with an actionable message otherwise.
 */
class DuckDbExtensionLoadingTest {

    /** Nothing listens here: any download attempt fails at once. */
    private static final String UNREACHABLE_REPOSITORY = "http://127.0.0.1:9";

    @TempDir
    Path tempDir;

    @Test
    void preinstalledExtensionInALaterDirectoryLoadsWithoutNetwork() throws Exception {
        // The test runs in a JVM that may already have ducklake loaded; LOAD then succeeds from
        // the process-wide handle. What must not happen is an INSTALL into the first (empty)
        // directory, which needs the network and fails against the unreachable repository.
        Path seeded = tempDir.resolve("seeded");
        assumeTrue(seed(seeded, "ducklake"), "ducklake extension not available to seed the test directory");
        Path writable = Files.createDirectories(tempDir.resolve("writable"));

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement statement = conn.createStatement()) {
            statement.execute("SET custom_extension_repository = '" + UNREACHABLE_REPOSITORY + "'");
            statement.execute("SET autoinstall_extension_repository = '" + UNREACHABLE_REPOSITORY + "'");

            assertDoesNotThrow(() -> DuckDbConnectionHelper.loadExtension(conn, "ducklake",
                    List.of(writable.toString(), seeded.toString())));
            assertTrue(loaded(conn, "ducklake"));
        }
    }

    @Test
    void missingExtensionFailsWithAnActionableMessage() throws Exception {
        Path empty = Files.createDirectories(tempDir.resolve("empty"));
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement statement = conn.createStatement()) {
            statement.execute("SET custom_extension_repository = '" + UNREACHABLE_REPOSITORY + "'");
            statement.execute("SET autoinstall_extension_repository = '" + UNREACHABLE_REPOSITORY + "'");

            SQLException e = assertThrows(SQLException.class, () -> DuckDbConnectionHelper.loadExtension(
                    conn, "yaci_no_such_extension", List.of(empty.toString())));

            String message = e.getMessage();
            assertTrue(message.contains("'yaci_no_such_extension'"), message);
            assertTrue(message.contains(empty.toAbsolutePath().normalize().toString()), message);
            assertTrue(message.contains("yaci.store.analytics.duckdb.extension-directories"), message);
            assertTrue(message.contains("INSTALL yaci_no_such_extension"), message);
        }
    }

    @Test
    void extensionDirectoriesAreNotReappliedOnALockedConfiguration() throws Exception {
        Path dir = Files.createDirectories(tempDir.resolve("ext"));
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement statement = conn.createStatement()) {
            statement.execute(DuckDbConnectionHelper.extensionDirectoriesSql(List.of(dir.toString())));
            statement.execute("SET lock_configuration = true");

            // core extension that is statically linked: LOAD succeeds, SET must not abort it
            assertDoesNotThrow(() -> DuckDbConnectionHelper.loadExtension(conn, "parquet", List.of(dir.toString())));
        }
    }

    /** Install {@code extension} into {@code dir} (from the local default directory or the network). */
    private static boolean seed(Path dir, String extension) {
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement statement = conn.createStatement()) {
            Files.createDirectories(dir);
            statement.execute(DuckDbConnectionHelper.extensionDirectoriesSql(List.of(dir.toString())));
            String version = single(conn, "SELECT library_version FROM pragma_version()");
            String platform = single(conn, "SELECT platform FROM pragma_platform()");
            Path local = Path.of(System.getProperty("user.home"), ".duckdb", "extensions", version, platform,
                    extension + ".duckdb_extension");
            if (Files.isRegularFile(local)) {
                Path target = Files.createDirectories(dir.resolve(version).resolve(platform));
                Files.copy(local, target.resolve(local.getFileName()));
                return true;
            }
            statement.execute("INSTALL " + extension);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean loaded(Connection conn, String extension) throws SQLException {
        return "true".equalsIgnoreCase(single(conn,
                "SELECT loaded::VARCHAR FROM duckdb_extensions() WHERE extension_name = '" + extension + "'"));
    }

    private static String single(Connection conn, String sql) throws SQLException {
        try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }
}
