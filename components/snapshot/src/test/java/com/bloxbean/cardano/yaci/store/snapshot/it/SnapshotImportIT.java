package com.bloxbean.cardano.yaci.store.snapshot.it;

import com.bloxbean.cardano.yaci.store.snapshot.load.ImportJournal;
import com.bloxbean.cardano.yaci.store.snapshot.load.ImportOptions;
import com.bloxbean.cardano.yaci.store.snapshot.load.ImportReport;
import com.bloxbean.cardano.yaci.store.snapshot.load.PgSchema;
import com.bloxbean.cardano.yaci.store.snapshot.load.SnapshotImporter;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestCodec;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import com.bloxbean.cardano.yaci.store.snapshot.validate.SnapshotValidator;
import com.bloxbean.cardano.yaci.store.snapshot.validate.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end import against a real PostgreSQL server, using real Parquet, real ZIP parts and the
 * real built-in specifications.
 *
 * <p>Runs only when {@code SNAPSHOT_IT_JDBC_URL} is set, and only ever against its own dedicated
 * schema. See {@link PostgresSupport}.
 */
@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class SnapshotImportIT {

    @TempDir
    Path root;

    private SnapshotSpecRegistry registry;
    private String schema;

    @BeforeEach
    void setUp() throws Exception {
        registry = SnapshotSpecRegistry.builtIn();
        schema = PostgresSupport.schema();
        try (Connection conn = PostgresSupport.connect()) {
            PostgresSupport.resetSchema(conn);
        }
    }

    private SnapshotFixture.Built fixture() throws Exception {
        try (Connection conn = PostgresSupport.connect()) {
            PgSchema pgs = new PgSchema(conn, schema);
            return SnapshotFixture.build(root, pgs.fingerprint(), pgs.flywayFingerprint());
        }
    }

    private ImportOptions options(Path manifest, int workers) {
        return new ImportOptions(manifest, manifest.getParent(), root.resolve("work"),
                PostgresSupport.schemaUrl(), PostgresSupport.user(), PostgresSupport.password(),
                schema, "preprod", 1, 1, 10, workers, "512MB", 0,
                true, false, List.of(), false, false);
    }

    // ------------------------------------------------------------------ happy path

    @Test
    void importsEveryModeAndReconstructsControlState() throws Exception {
        SnapshotFixture.Built built = fixture();
        ImportReport report = new SnapshotImporter(registry)
                .importSnapshot(options(built.manifestPath(), 2), null);

        assertThat(report.status()).isEqualTo(ImportJournal.STATUS_VALIDATING);
        assertThat(report.batchesLoaded()).isGreaterThan(0);
        assertThat(report.batchesSkipped()).isZero();

        try (Connection conn = PostgresSupport.connect()) {
            // MAPPED with a timestamp conversion and three jsonb conversions.
            assertThat(count(conn, "block")).isEqualTo(SnapshotFixture.POINT_BLOCK + 1);
            assertThat(scalarLong(conn, "SELECT block_time FROM " + qt("block")
                    + " WHERE number = 0")).isEqualTo(1700000000L);
            assertThat(scalarString(conn, "SELECT jsonb_typeof(leader_vrf) FROM " + qt("block")
                    + " WHERE number = 0")).isEqualTo("object");

            // MAPPED with a rename: epoch.number comes from the export's epoch column.
            assertThat(count(conn, "epoch")).isEqualTo(3);
            assertThat(scalarLong(conn, "SELECT max(number) FROM " + qt("epoch"))).isEqualTo(2);

            // SQL mode: regrouped one row per (tx_hash, output_index), rebuilt amounts, joined block.
            assertThat(count(conn, "address_utxo")).isEqualTo(SnapshotFixture.POINT_BLOCK + 1);
            assertThat(scalarLong(conn, "SELECT jsonb_array_length(amounts) FROM " + qt("address_utxo")
                    + " WHERE slot = 0")).isEqualTo(2);
            assertThat(scalarString(conn, "SELECT amounts->0->>'unit' FROM " + qt("address_utxo")
                    + " WHERE slot = 0")).isEqualTo("lovelace");
            assertThat(scalarLong(conn, "SELECT lovelace_amount FROM " + qt("address_utxo")
                    + " WHERE slot = 0")).isEqualTo(1000000L);
            assertThat(scalarLong(conn, "SELECT block FROM " + qt("address_utxo")
                    + " WHERE slot = 0")).isZero();
            assertThat(scalarLong(conn, "SELECT count(*) FROM " + qt("address_utxo")
                    + " WHERE owner_addr_full IS NOT NULL")).isZero();

            // cursor_ tail seeded to the configured depth, ending exactly at the point.
            assertThat(count(conn, "cursor_")).isEqualTo(10);
            assertThat(scalarLong(conn, "SELECT max(block_number) FROM " + qt("cursor_")))
                    .isEqualTo(SnapshotFixture.POINT_BLOCK);
            assertThat(scalarString(conn, "SELECT block_hash FROM " + qt("cursor_")
                    + " ORDER BY slot DESC LIMIT 1"))
                    .isEqualTo(SnapshotFixture.hashOf(SnapshotFixture.POINT_BLOCK));

            // era rebuilt from the first block of each era.
            assertThat(count(conn, "era")).isEqualTo(3);
            assertThat(scalarLong(conn, "SELECT block FROM " + qt("era") + " WHERE era = 7"))
                    .isEqualTo(20);

            // AdaPot jobs marked complete only up to the snapshot epoch.
            assertThat(scalarLong(conn, "SELECT max(epoch) FROM " + qt("adapot_jobs")))
                    .isEqualTo(SnapshotFixture.POINT_EPOCH);
            assertThat(scalarString(conn, "SELECT DISTINCT status FROM " + qt("adapot_jobs")))
                    .isEqualTo("COMPLETED");
        }

        assertThat(report.handlerResults()).hasSize(3);
        assertThat(report.sequencesReset()).containsKey("rollback_id_seq");
    }

    @Test
    void nothingIsLoadedBeyondTheConsistencyPoint() throws Exception {
        SnapshotFixture.Built built = fixture();
        new SnapshotImporter(registry).importSnapshot(options(built.manifestPath(), 2), null);

        try (Connection conn = PostgresSupport.connect()) {
            assertThat(scalarLong(conn, "SELECT count(*) FROM " + qt("block") + " WHERE slot > "
                    + SnapshotFixture.slotOf(SnapshotFixture.POINT_BLOCK))).isZero();
            assertThat(scalarLong(conn, "SELECT count(*) FROM " + qt("address_utxo") + " WHERE slot > "
                    + SnapshotFixture.slotOf(SnapshotFixture.POINT_BLOCK))).isZero();
            assertThat(scalarLong(conn, "SELECT count(*) FROM " + qt("epoch") + " WHERE number > "
                    + SnapshotFixture.POINT_EPOCH)).isZero();
            assertThat(scalarLong(conn, "SELECT count(*) FROM " + qt("adapot") + " WHERE epoch > "
                    + SnapshotFixture.POINT_EPOCH)).isZero();
        }
    }

    @Test
    void sequencesAreResetSoTheNextInsertDoesNotCollide() throws Exception {
        SnapshotFixture.Built built = fixture();
        new SnapshotImporter(registry).importSnapshot(options(built.manifestPath(), 1), null);

        try (Connection conn = PostgresSupport.connect(); Statement st = conn.createStatement()) {
            long maxId = scalarLong(conn, "SELECT max(id) FROM " + qt("rollback"));
            try (ResultSet rs = st.executeQuery("SELECT nextval('" + schema + ".rollback_id_seq')")) {
                rs.next();
                assertThat(rs.getLong(1)).isGreaterThan(maxId);
            }
        }
    }

    @Test
    void offlineValidationPasses() throws Exception {
        SnapshotFixture.Built built = fixture();
        new SnapshotImporter(registry).importSnapshot(options(built.manifestPath(), 2), null);

        try (Connection conn = PostgresSupport.connect()) {
            List<ValidationReport> reports = new SnapshotValidator(registry)
                    .validateAll(conn, schema, built.manifest(), 1, 10);
            for (ValidationReport report : reports) {
                assertThat(report.failures())
                        .as("failures in %s: %s", report.level(), report.failures())
                        .isEmpty();
            }
        }
    }

    // ------------------------------------------------------------------ resume

    @Test
    void aResumedImportSkipsCommittedBatchesAndDoesNotDuplicateRows() throws Exception {
        // An interrupted import is modelled as a first run that carried only some of the tables,
        // leaving its journal behind. Batch identity is content-addressed, so the second run
        // recognises the already-committed work by its files, not by any ordinal.
        String snapshotId = "it-resume-snapshot";
        SnapshotFixture.Built partial;
        SnapshotFixture.Built full;
        try (Connection conn = PostgresSupport.connect()) {
            PgSchema pgs = new PgSchema(conn, schema);
            partial = SnapshotFixture.build(root.resolve("partial"), pgs.fingerprint(),
                    pgs.flywayFingerprint(), List.of("block", "epoch"), snapshotId);
            full = SnapshotFixture.build(root.resolve("full"), pgs.fingerprint(),
                    pgs.flywayFingerprint(), SnapshotFixture.ALL_TABLES, snapshotId);
        }

        ImportReport first = new SnapshotImporter(registry)
                .importSnapshot(options(partial.manifestPath(), 1), null);
        assertThat(first.batchesLoaded()).isGreaterThan(0);
        assertThat(first.batchesSkipped()).isZero();

        long blocksAfterFirst;
        long completedAfterFirst;
        try (Connection conn = PostgresSupport.connect()) {
            blocksAfterFirst = count(conn, "block");
            completedAfterFirst = new ImportJournal(conn, schema).completedCount(snapshotId);
            assertThat(count(conn, "address_utxo")).isZero();
        }
        assertThat(blocksAfterFirst).isEqualTo(SnapshotFixture.POINT_BLOCK + 1);
        assertThat(completedAfterFirst).isEqualTo(first.batchesLoaded());

        ImportReport second = new SnapshotImporter(registry)
                .importSnapshot(options(full.manifestPath(), 1), null);

        assertThat(second.batchesSkipped()).isEqualTo(completedAfterFirst);
        assertThat(second.batchesLoaded()).isGreaterThan(0);

        try (Connection conn = PostgresSupport.connect()) {
            assertThat(count(conn, "block")).isEqualTo(blocksAfterFirst);
            assertThat(count(conn, "address_utxo")).isEqualTo(SnapshotFixture.POINT_BLOCK + 1);
            assertThat(scalarLong(conn, "SELECT count(*) FROM (SELECT hash FROM " + qt("block")
                    + " GROUP BY hash HAVING count(*) > 1) d")).isZero();
        }
    }

    @Test
    void aFailedBatchLeavesNeitherRowsNorAJournalRecord() throws Exception {
        SnapshotFixture.Built built = fixture();

        // Corrupt an extracted file after verification by importing with extraction skipped and an
        // empty work directory: the batch fails, and the transaction must leave nothing behind.
        ImportOptions broken = new ImportOptions(built.manifestPath(), built.archiveDir(),
                root.resolve("empty-work"), PostgresSupport.schemaUrl(), PostgresSupport.user(),
                PostgresSupport.password(), schema, "preprod", 1, 1, 10, 1, "512MB", 0,
                true, false, List.of(), false, true);
        Files.createDirectories(root.resolve("empty-work"));

        assertThatThrownBy(() -> new SnapshotImporter(registry).importSnapshot(broken, null))
                .isInstanceOf(IllegalStateException.class);

        try (Connection conn = PostgresSupport.connect()) {
            assertThat(count(conn, "block")).isZero();
            ImportJournal journal = new ImportJournal(conn, schema);
            assertThat(journal.completedCount(built.manifest().snapshotId())).isZero();
        }
    }

    // ------------------------------------------------------------------ refusals

    @Test
    void refusesANetworkMismatch() throws Exception {
        SnapshotFixture.Built built = fixture();
        ImportOptions wrongNetwork = new ImportOptions(built.manifestPath(), built.archiveDir(),
                root.resolve("work"), PostgresSupport.schemaUrl(), PostgresSupport.user(),
                PostgresSupport.password(), schema, "mainnet", 764824073L, 1, 10, 1, "512MB", 0,
                true, false, List.of(), false, false);

        SnapshotImporter.Preflight pre = new SnapshotImporter(registry).preflight(wrongNetwork);
        assertThat(pre.ok()).isFalse();
        assertThat(pre.blockers()).anySatisfy(b -> assertThat(b).contains("Network mismatch"));
    }

    @Test
    void refusesAMismatchedSchemaFingerprint() throws Exception {
        SnapshotFixture.Built built = fixture();
        SnapshotManifest tampered = new SnapshotManifest(built.manifest().formatVersion(),
                built.manifest().snapshotId(), built.manifest().createdAt(), built.manifest().producer(),
                built.manifest().yaciStoreVersion(), built.manifest().specFormatVersion(),
                built.manifest().duckdbVersion(), built.manifest().ducklakeFormatVersion(),
                built.manifest().point(), built.manifest().genesisHash(), built.manifest().modules(),
                built.manifest().pruningSettings(), "0".repeat(64), built.manifest().flywayFingerprint(),
                built.manifest().tables(), built.manifest().parts(), built.manifest().declaredLossy());
        Path path = built.archiveDir().resolve("tampered.manifest.json");
        new ManifestCodec().writeAtomically(tampered, path);

        SnapshotImporter.Preflight pre = new SnapshotImporter(registry).preflight(options(path, 1));
        assertThat(pre.ok()).isFalse();
        assertThat(pre.blockers()).anySatisfy(b -> assertThat(b).contains("schema fingerprint"));
    }

    @Test
    void refusesASpecificationDigestMismatch() throws Exception {
        SnapshotFixture.Built built = fixture();
        List<SnapshotManifest.TableManifest> tables = built.manifest().tables().stream()
                .map(t -> t.specId().equals("block")
                        ? new SnapshotManifest.TableManifest(t.specId(), t.specVersion(), "0".repeat(64),
                        t.module(), t.kind(), t.restore(), t.reason(), t.sourceRelation(), t.exporterId(),
                        t.targetTable(), t.loadMode(), t.cutoffRule(), t.transformVersion(),
                        t.dependencies(), t.rowCount(), t.key(), t.columnFingerprint(),
                        t.sourceColumns(), t.bounds(), t.files())
                        : t)
                .toList();
        SnapshotManifest tampered = replaceTables(built.manifest(), tables);
        Path path = built.archiveDir().resolve("badspec.manifest.json");
        new ManifestCodec().writeAtomically(tampered, path);

        SnapshotImporter.Preflight pre = new SnapshotImporter(registry).preflight(options(path, 1));
        assertThat(pre.ok()).isFalse();
        assertThat(pre.blockers()).anySatisfy(b -> assertThat(b).contains("has digest"));
    }

    @Test
    void refusesANonEmptySchemaWithNoMatchingJournal() throws Exception {
        SnapshotFixture.Built built = fixture();
        try (Connection conn = PostgresSupport.connect(); Statement st = conn.createStatement()) {
            st.execute("INSERT INTO " + qt("block") + " (hash, number, slot) VALUES ('"
                    + "f".repeat(64) + "', 1, 1)");
        }
        SnapshotImporter.Preflight pre =
                new SnapshotImporter(registry).preflight(options(built.manifestPath(), 1));
        assertThat(pre.ok()).isFalse();
        assertThat(pre.blockers())
                .anySatisfy(b -> assertThat(b).contains("already contains chain data"));
    }

    @Test
    void refusesATamperedArchivePart() throws Exception {
        SnapshotFixture.Built built = fixture();
        Path part = built.archiveDir().resolve(built.manifest().parts().get(0).fileName());
        byte[] bytes = Files.readAllBytes(part);
        bytes[bytes.length / 2] ^= 0x33;
        Files.write(part, bytes);

        SnapshotImporter.Preflight pre =
                new SnapshotImporter(registry).preflight(options(built.manifestPath(), 1));
        assertThat(pre.ok()).isFalse();
        assertThat(pre.blockers()).anySatisfy(b -> assertThat(b).contains("SHA-256 mismatch"));
    }

    @Test
    void abortsWhenFreeDiskIsBelowTheConfiguredFloor() throws Exception {
        SnapshotFixture.Built built = fixture();
        ImportOptions tight = new ImportOptions(built.manifestPath(), built.archiveDir(),
                root.resolve("work"), PostgresSupport.schemaUrl(), PostgresSupport.user(),
                PostgresSupport.password(), schema, "preprod", 1, 1, 10, 1, "512MB",
                Long.MAX_VALUE / 2, true, false, List.of(), false, false);

        SnapshotImporter.Preflight pre = new SnapshotImporter(registry).preflight(tight);
        assertThat(pre.ok()).isFalse();
        assertThat(pre.blockers()).anySatisfy(b -> assertThat(b).contains("free at"));
    }

    // ------------------------------------------------------------------ helpers

    private String qt(String table) {
        return Identifiers.quote(schema) + "." + Identifiers.quote(table);
    }

    private long count(Connection conn, String table) throws SQLException {
        return scalarLong(conn, "SELECT count(*) FROM " + qt(table));
    }

    private static long scalarLong(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : -1;
        }
    }

    private static String scalarString(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        }
    }

    private static SnapshotManifest replaceTables(SnapshotManifest m,
                                                  List<SnapshotManifest.TableManifest> tables) {
        return new SnapshotManifest(m.formatVersion(), m.snapshotId(), m.createdAt(), m.producer(),
                m.yaciStoreVersion(), m.specFormatVersion(), m.duckdbVersion(),
                m.ducklakeFormatVersion(), m.point(), m.genesisHash(), m.modules(),
                m.pruningSettings(), m.schemaFingerprint(), m.flywayFingerprint(), tables, m.parts(),
                m.declaredLossy());
    }
}
