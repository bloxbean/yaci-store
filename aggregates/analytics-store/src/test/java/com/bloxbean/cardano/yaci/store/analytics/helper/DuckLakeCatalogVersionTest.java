package com.bloxbean.cardano.yaci.store.analytics.helper;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ADR 0008 D2: the writer never migrates a DuckLake catalog implicitly, refuses catalogs it
 * cannot open with resync steps, and leaves them untouched.
 *
 * <p>Older and newer catalogs are simulated by relabelling the spec version in
 * {@code ducklake_metadata} of a catalog made by this engine; DuckLake's version check reads only
 * that value. The real 0.3 (DuckDB 1.4.1) and 1.0 (DuckDB 1.5.5) catalogs are covered by the M0
 * probe kit ({@code adr/0008-m0/matrix.sh}). Skipped when the DuckLake extension cannot be loaded.</p>
 */
class DuckLakeCatalogVersionTest {

    @TempDir
    Path tempDir;

    private AnalyticsStoreProperties properties;
    private Path catalogFile;

    @BeforeEach
    void setUp() {
        assumeTrue(duckLakeAvailable(), "DuckLake extension not available");
        properties = new AnalyticsStoreProperties();
        properties.setExportPath(tempDir.resolve("analytics").toString());
        properties.getStorage().setType("ducklake");
        properties.getDucklake().setCatalogType("duckdb");
        catalogFile = tempDir.resolve("analytics/ducklake.catalog.db");
        properties.getDucklake().setCatalogPath(catalogFile.toString());
    }

    @Test
    void writerAttachDoesNotRequestMigrationUnlessOptedIn() {
        assertThat(DuckDbConnectionHelper.duckLakeAttachOptions("/d", false, false))
                .isEqualTo("DATA_PATH '/d'");
        assertThat(DuckDbConnectionHelper.duckLakeAttachOptions("/d", false, true))
                .isEqualTo("DATA_PATH '/d', AUTOMATIC_MIGRATION true");
        assertThat(DuckDbConnectionHelper.duckLakeAttachOptions("/d", true, true))
                .isEqualTo("READ_ONLY, DATA_PATH '/d'");
    }

    @Test
    void newCatalogIsCreatedAtTheEngineDefaultSpecAndItsVersionsAreReadable() throws Exception {
        DuckDbConnectionHelper.CatalogVersions versions;
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            helper().prepareConnectionForDuckLake(conn, false, false);
            versions = helper().readCatalogVersions(conn);
        }
        assertThat(versions.catalogSpecVersion()).isEqualTo("1.1");
        assertThat(versions.duckdbVersion()).startsWith("v2.");
        assertThat(versions.ducklakeExtensionVersion()).isNotBlank();
    }

    @Test
    void olderCatalogIsRefusedWithResyncStepsAndLeftUntouched() throws Exception {
        createCatalog();
        relabel("0.3");
        String before = sha256(catalogFile);

        assertThatThrownBy(this::attachReadWrite)
                .isInstanceOfSatisfying(CatalogVersionRefusal.class, e -> {
                    assertThat(e.getCatalogVersion()).isEqualTo("0.3");
                    assertThat(e.isNewerThanEngine()).isFalse();
                    assertThat(e.getMessage())
                            .contains("spec version 0.3")
                            .contains("analytics_export_state")
                            .contains(catalogFile.toString())
                            .contains("auto-migrate=true");
                });
        assertThat(sha256(catalogFile)).isEqualTo(before);
    }

    @Test
    void catalogNewerThanTheEngineIsRefused() throws Exception {
        createCatalog();
        relabel("9.9");

        assertThatThrownBy(this::attachReadWrite)
                .isInstanceOfSatisfying(CatalogVersionRefusal.class, e -> {
                    assertThat(e.getCatalogVersion()).isEqualTo("9.9");
                    assertThat(e.isNewerThanEngine()).isTrue();
                    assertThat(e.getMessage()).contains("newer than this engine supports");
                });
    }

    @Test
    void autoMigrateOptInLetsDuckLakeRewriteTheCatalog() throws Exception {
        createOneDotZeroCatalog();
        properties.getDucklake().setAutoMigrate(true);

        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            helper().prepareConnectionForDuckLake(conn, false, false);
            assertThat(helper().readCatalogVersions(conn).catalogSpecVersion()).isEqualTo("1.1");
        }
    }

    @Test
    void oneDotZeroCatalogIsUsedAsIsWithoutMigration() throws Exception {
        // Without auto-migrate a 1.0 catalog must stay 1.0, and stay writable.
        createOneDotZeroCatalog();
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            helper().prepareConnectionForDuckLake(conn, false, false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("INSERT INTO ducklake_catalog.seed VALUES (1)");
            }
            assertThat(helper().readCatalogVersions(conn).catalogSpecVersion()).isEqualTo("1.0");
        }
    }

    @Test
    void unrelatedAttachFailuresAreNotTurnedIntoRefusals() {
        SQLException other = new SQLException("IO Error: Could not set lock on file");
        assertThat((Object) CatalogVersionRefusal.from(other, "duckdb", properties)).isNull();
    }

    @Test
    void extensionDirectoriesAreQuotedAndBlankEntriesIgnored() {
        assertThat(DuckDbConnectionHelper.extensionDirectoriesSql(List.of())).isNull();
        assertThat(DuckDbConnectionHelper.extensionDirectoriesSql(List.of(" ", ""))).isNull();
        assertThat(DuckDbConnectionHelper.extensionDirectoriesSql(List.of("/opt/ext", "/it's")))
                .isEqualTo("SET extension_directories = ['/opt/ext', '/it''s'];");
    }

    /** A genuine DuckLake 1.0 catalog, as made by DuckDB 1.5.x (here: by this engine on request). */
    private void createOneDotZeroCatalog() throws Exception {
        Files.createDirectories(catalogFile.getParent());
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement stmt = conn.createStatement()) {
            stmt.execute("LOAD ducklake");
            stmt.execute("ATTACH 'ducklake:" + catalogFile + "' AS c (DATA_PATH '" + properties.getExportPath()
                    + "', DUCKLAKE_VERSION '1.0')");
            stmt.execute("CREATE TABLE c.seed (id INTEGER)");
        }
    }

    private void createCatalog() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            helper().prepareConnectionForDuckLake(conn, false, false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE ducklake_catalog.seed (id INTEGER)");
            }
        }
    }

    /** Rewrite the spec version recorded in the catalog file, with no DuckLake attached. */
    private void relabel(String version) throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:" + catalogFile);
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE ducklake_metadata SET value = '" + version + "' WHERE key = 'version'");
        }
    }

    private void attachReadWrite() throws SQLException {
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:")) {
            helper().prepareConnectionForDuckLake(conn, false, false);
        }
    }

    private DuckDbConnectionHelper helper() {
        return new DuckDbConnectionHelper(environment(), properties);
    }

    private static String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private static boolean duckLakeAvailable() {
        try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSTALL ducklake");
            stmt.execute("LOAD ducklake");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static Environment environment() {
        Environment environment = mock(Environment.class);
        when(environment.getProperty("spring.datasource.url"))
                .thenReturn("jdbc:postgresql://localhost:5432/yaci?currentSchema=cardano");
        when(environment.getProperty("spring.datasource.username")).thenReturn("user");
        when(environment.getProperty("spring.datasource.password")).thenReturn("password");
        return environment;
    }
}
