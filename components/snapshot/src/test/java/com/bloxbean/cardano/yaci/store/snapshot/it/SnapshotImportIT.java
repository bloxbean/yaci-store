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
import com.bloxbean.cardano.yaci.store.snapshot.util.Progress;
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

    @Test
    void fingerprintIgnoresKnownRewardWorkTablesButStillDetectsSchemaChanges() throws Exception {
        try (var conn = PostgresSupport.connect(); var st = conn.createStatement()) {
            st.execute("SET search_path TO " + Identifiers.quote(schema));
            var pgs = new PgSchema(conn, schema);
            String original = pgs.fingerprint();
            st.execute("CREATE UNLOGGED TABLE ss_max_slot_balances(address TEXT, max_slot BIGINT)");
            st.execute("CREATE TABLE ss_drep_status(address TEXT)");
            assertThat(pgs.fingerprint()).isEqualTo(original);
            st.execute("CREATE UNLOGGED TABLE ss_unknown_state(value BIGINT)");
            assertThat(pgs.fingerprint()).isNotEqualTo(original);
            st.execute("DROP TABLE ss_unknown_state");
            st.execute("ALTER TABLE block ADD COLUMN unexpected BIGINT");
            assertThat(pgs.fingerprint()).isNotEqualTo(original);
        }
    }

    private SnapshotFixture.Built fixture() throws Exception {
        try (Connection conn = PostgresSupport.connect()) {
            PgSchema pgs = new PgSchema(conn, schema);
            return SnapshotFixture.build(root, pgs.fingerprint(), pgs.flywayFingerprint());
        }
    }

    private SnapshotFixture.Built fixtureWithTableSchemas() throws Exception {
        var built = fixture();
        var m = built.manifest();
        try (var conn = PostgresSupport.connect()) {
            var manifest = new SnapshotManifest(m.formatVersion(), m.snapshotId(), m.createdAt(), m.producer(),
                    m.yaciStoreVersion(), m.specFormatVersion(), m.duckdbVersion(), m.ducklakeFormatVersion(),
                    m.point(), m.genesisHash(), m.modules(), m.pruningSettings(), m.schemaFingerprint(),
                    m.flywayFingerprint(), m.tables(), m.parts(), m.declaredLossy(),
                    new PgSchema(conn, schema).tableFingerprints());
            new ManifestCodec().writeAtomically(manifest, built.manifestPath());
            return new SnapshotFixture.Built(built.archiveDir(), built.manifestPath(), manifest);
        }
    }

    @Test
    void scopedRestoreLoadsOnlyEnabledStoresIntoSmallerSchemaAndValidates() throws Exception {
        var built = fixtureWithTableSchemas();
        var selected = registry.selectModules(java.util.Set.of("core", "blocks")).withoutDisabledOptionalTables(setting -> false);
        try (var conn = PostgresSupport.connect(); var st = conn.createStatement()) {
            st.execute("DROP TABLE " + schema + ".address_utxo");
            st.execute("DROP TABLE " + schema + ".adapot");
            st.execute("DROP TABLE " + schema + ".epoch");
        }
        var importer = new SnapshotImporter(selected);
        assertThat(importer.preflight(options(built.manifestPath(), 1)).blockers()).isEmpty();
        importer.importSnapshot(options(built.manifestPath(), 1), null);
        try (var conn = PostgresSupport.connect()) {
            assertThat(count(conn, "block")).isEqualTo(SnapshotFixture.POINT_BLOCK + 1);
            for (var report : new SnapshotValidator(selected).validateAll(conn, schema, built.manifest(), 1, 10)) {
                assertThat(report.passed()).withFailMessage(report.toString()).isTrue();
            }
        }
    }

    @Test
    void scopedRestoreRejectsMissingEnabledTableAndChangedTargetDefinition() throws Exception {
        var built = fixtureWithTableSchemas();
        var importer = new SnapshotImporter(registry.selectModules(java.util.Set.of("core", "blocks")).withoutDisabledOptionalTables(setting -> false));
        try (var conn = PostgresSupport.connect(); var st = conn.createStatement()) {
            st.execute("ALTER TABLE " + schema + ".block ADD COLUMN unexpected INTEGER");
            assertThat(importer.preflight(options(built.manifestPath(), 1)).blockers())
                    .anyMatch(p -> p.contains("selected table 'block'"));
            st.execute("DROP TABLE " + schema + ".block");
            assertThat(importer.preflight(options(built.manifestPath(), 1)).blockers())
                    .anyMatch(p -> p.contains("missing target table 'block'"));
        }
    }

    @Test
    void scopedRestoreLeavesDisabledTablesEmptyAndRejectsScopeChangeOnResume() throws Exception {
        var built = fixtureWithTableSchemas();
        var selected = registry.selectModules(java.util.Set.of("core", "blocks")).withoutDisabledOptionalTables(setting -> false);
        assertThatThrownBy(() -> new SnapshotImporter(selected).importSnapshot(options(built.manifestPath(), 1), p -> {
            if (p.startsWith("block +")) throw new IllegalStateException("interrupt scoped import");
        })).hasMessageContaining("interrupt scoped import");
        var changed = registry.selectModules(java.util.Set.of("core", "blocks", "utxo")).withoutDisabledOptionalTables(setting -> false);
        assertThat(new SnapshotImporter(changed).preflight(options(built.manifestPath(), 1)).blockers())
                .anyMatch(p -> p.contains("Import scope does not match"));
        new SnapshotImporter(selected).importSnapshot(options(built.manifestPath(), 1), null);
        try (var conn = PostgresSupport.connect()) {
            assertThat(count(conn, "address_utxo")).isZero();
            assertThat(count(conn, "adapot")).isZero();
            assertThat(new SnapshotValidator(changed).validateLoad(conn, schema, built.manifest()).passed()).isFalse();
        }
    }

    private ImportOptions options(Path manifest, int workers, java.util.Map<String, String> retention,
                                  java.util.Map<String, String> targetSettings) {
        return new ImportOptions(manifest, manifest.getParent(), root.resolve("work"),
                PostgresSupport.schemaUrl(), PostgresSupport.user(), PostgresSupport.password(),
                schema, "preprod", 1, 1, 10, workers, "512MB", 0,
                true, false, List.of(), false, false, false, retention, targetSettings);
    }

    @Test
    void importsAPublishedSnapshotStreamedFromAPublicLocationResumingABrokenRead() throws Exception {
        var built = fixture();
        com.bloxbean.cardano.yaci.store.snapshot.archive.Checksums.write(built.manifest(), built.manifestPath());
        Path bucket = root.resolve("bucket");
        var local = new com.bloxbean.cardano.yaci.store.snapshot.remote.SnapshotRepository(
                new com.bloxbean.cardano.yaci.store.snapshot.remote.LocalSnapshotStore(bucket));
        local.publish(built.manifestPath(), 2, true, null);

        try (var server = new com.bloxbean.cardano.yaci.store.snapshot.remote.TestHttpServer(bucket, true);
             var http = new com.bloxbean.cardano.yaci.store.snapshot.remote.HttpSnapshotStore(
                     java.net.URI.create(server.url()), java.time.Duration.ofSeconds(2))) {
            var repo = new com.bloxbean.cardano.yaci.store.snapshot.remote.SnapshotRepository(http, java.time.Duration.ZERO);
            var resolved = repo.resolve("preprod", "latest", null,
                    m -> new com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestValidator(registry).validate(m), null);
            Path manifest = repo.downloadMetadata(resolved, root.resolve("work/snapshot"));
            var options = options(manifest, 2).withPartSource(repo.partSource(resolved));
            var importer = new SnapshotImporter(registry);
            assertThat(importer.preflight(options).blockers()).isEmpty();

            List<String> messages = new java.util.concurrent.CopyOnWriteArrayList<>();
            importer.importSnapshot(options, messages::add);

            assertThat(messages).anyMatch(m -> m.contains("Downloading, verifying and extracting"));
            // Each part's broken read resumed from where it stopped instead of starting over.
            assertThat(server.rangeRequests()).isEqualTo(built.manifest().parts().size());
            assertThat(messages).noneMatch(m -> m.contains("Retrying "));
        }
        try (var conn = PostgresSupport.connect()) {
            assertThat(count(conn, "block")).isEqualTo(SnapshotFixture.POINT_BLOCK + 1);
            for (var report : new SnapshotValidator(registry).validateAll(conn, schema, built.manifest(), 1, 10)) {
                assertThat(report.passed()).withFailMessage(report.toString()).isTrue();
            }
        }
    }

    @Test
    void aPrunedSnapshotNeedsATargetThatPrunesTheSameTables() throws Exception {
        var built = fixture();
        var m = built.manifest();
        var pruned = m.withTables(m.tables(), SnapshotManifest.FLAVOR_PRUNED,
                java.util.Map.of("address_utxo.spent-safe-blocks", "2160", "epoch_stake.epochs", "8"));
        new ManifestCodec().writeAtomically(pruned, built.manifestPath());
        var importer = new SnapshotImporter(registry);

        assertThat(importer.preflight(options(built.manifestPath(), 1, java.util.Map.of(),
                java.util.Map.of("store.adapot.epoch-stake-pruning-enabled", "true"))).blockers())
                .anyMatch(b -> b.contains("store.utxo.pruning-enabled=true"))
                .noneMatch(b -> b.contains("epoch-stake-pruning-enabled"));
        assertThat(importer.preflight(options(built.manifestPath(), 1, java.util.Map.of(), java.util.Map.of(
                "store.utxo.pruning-enabled", "true", "store.adapot.epoch-stake-pruning-enabled", "true"))).blockers())
                .isEmpty();
        // A scoped target without the UTxO store does not care how the snapshot pruned it.
        try (var conn = PostgresSupport.connect(); var st = conn.createStatement()) {
            st.execute("DROP TABLE " + schema + ".address_utxo");
        }
        assertThat(new SnapshotImporter(registry.selectModules(java.util.Set.of("core", "blocks", "epoch-aggr", "adapot")).withoutDisabledOptionalTables(setting -> false))
                .preflight(options(built.manifestPath(), 1, java.util.Map.of(),
                        java.util.Map.of("store.adapot.epoch-stake-pruning-enabled", "true"))).blockers())
                .noneMatch(b -> b.contains("store.utxo.pruning-enabled"));
    }

    private SnapshotFixture.Built fixtureWithUtxoHistory() throws Exception {
        try (Connection conn = PostgresSupport.connect()) {
            PgSchema pgs = new PgSchema(conn, schema);
            return SnapshotFixture.build(root, pgs.fingerprint(), pgs.flywayFingerprint(), SnapshotFixture.ALL_TABLES,
                    "it-fixture-snapshot", true);
        }
    }

    @Test
    void aPruningTargetSkipsTheHistoryPartsOfTheSameSnapshot() throws Exception {
        var built = fixtureWithUtxoHistory();
        var split = built.manifest().table("address-utxo").historySplit();
        var pruning = java.util.Map.of("store.utxo.pruning-enabled", "true");
        var importer = new SnapshotImporter(registry);
        var pre = importer.preflight(options(built.manifestPath(), 1, java.util.Map.of(), pruning));
        assertThat(pre.blockers()).isEmpty();
        assertThat(pre.warnings()).anyMatch(w -> w.startsWith("address_utxo: current rows only"));
        assertThat(pre.manifest().retention()).containsEntry("address_utxo.spent-safe-blocks", "2160");
        var selection = importer.selection(pre.archive(), pre.manifest());
        assertThat(selection.files()).filteredOn(f -> f.startsWith("main/address_utxo/"))
                .containsExactly(SnapshotFixture.UTXO_PRUNED_PARTITION + "/pruned_copy.parquet");
        var layout = com.bloxbean.cardano.yaci.store.snapshot.archive.PartLayout.of(built.manifest());
        assertThat(layout.exact()).isTrue();
        assertThat(selection.parts()).hasSizeLessThan(built.manifest().parts().size())
                .noneMatch(p -> layout.files(p.partNumber()).stream()
                        .anyMatch(f -> f.startsWith("main/address_utxo/") && !f.endsWith("pruned_copy.parquet")));

        importer.importSnapshot(options(built.manifestPath(), 1, java.util.Map.of(), pruning), null);

        try (var conn = PostgresSupport.connect()) {
            assertThat(count(conn, "address_utxo")).isEqualTo(split.rowCount());
            for (var report : new SnapshotValidator(registry).validateAll(conn, schema, built.manifest(), 1, 10)) {
                assertThat(report.passed()).withFailMessage(report.toString()).isTrue();
            }
        }
    }

    @Test
    void aFullTargetAndAWiderPruningWindowLoadTheHistoryToo() throws Exception {
        var built = fixtureWithUtxoHistory();
        var importer = new SnapshotImporter(registry);
        var wider = java.util.Map.of("store.utxo.pruning-enabled", "true", "store.utxo.pruning-safe-blocks", "5000");
        var pre = importer.preflight(options(built.manifestPath(), 1, java.util.Map.of(), wider));
        assertThat(pre.blockers()).isEmpty();
        assertThat(pre.warnings()).anyMatch(w -> w.startsWith("address_utxo: full history, because"));
        // A full import never loads the pruned copy, so no output is loaded twice.
        assertThat(importer.selection(pre.archive(), pre.manifest()).files()).noneMatch(f -> f.endsWith("pruned_copy.parquet"));

        importer.importSnapshot(options(built.manifestPath(), 1, java.util.Map.of(), java.util.Map.of()), null);

        try (var conn = PostgresSupport.connect()) {
            assertThat(count(conn, "address_utxo")).isEqualTo(built.manifest().table("address-utxo").rowCount())
                    .isGreaterThan(built.manifest().table("address-utxo").historySplit().rowCount());
            for (var report : new SnapshotValidator(registry).validateAll(conn, schema, built.manifest(), 1, 10)) {
                assertThat(report.passed()).withFailMessage(report.toString()).isTrue();
            }
        }
    }

    @Test
    void transactionCborLoadsByteForByteIntoATargetThatSavesIt() throws Exception {
        var built = fixture();
        var importer = new SnapshotImporter(registry);
        var saves = java.util.Map.of("store.transaction.save-cbor", "true");
        assertThat(importer.preflight(options(built.manifestPath(), 1, java.util.Map.of(), saves)).blockers()).isEmpty();

        importer.importSnapshot(options(built.manifestPath(), 1, java.util.Map.of(), saves), null);

        // Blocks 5 to the point carry one transaction each; the CBOR is 84a4, the block number, then n x ff00c3.
        int rows = SnapshotFixture.POINT_BLOCK - 5 + 1;
        try (var conn = PostgresSupport.connect(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT count(*), count(*) FILTER (WHERE length(cbor_data) = cbor_size),"
                        + " count(*) FILTER (WHERE encode(cbor_data, 'hex') = '84a4' || lpad(to_hex(slot / 20), 8, '0')"
                        + " || repeat('ff00c3', (slot / 20)::int)), min(slot), max(slot)"
                        + " FROM " + Identifiers.quote(schema) + ".transaction_cbor")) {
            rs.next();
            assertThat(rs.getLong(1)).isEqualTo(rows);
            assertThat(rs.getLong(2)).isEqualTo(rows);
            assertThat(rs.getLong(3)).isEqualTo(rows);
            assertThat(rs.getLong(5)).isEqualTo(SnapshotFixture.slotOf(SnapshotFixture.POINT_BLOCK));
        }
        try (var conn = PostgresSupport.connect()) {
            for (var report : new SnapshotValidator(registry).validateAll(conn, schema, built.manifest(), 1, 10)) {
                assertThat(report.passed()).withFailMessage(report.toString()).isTrue();
            }
        }
    }

    @Test
    void aTargetWithoutCborNeverReadsItsPartsAndAnotherFormIsRefused() throws Exception {
        var built = fixture();
        var view = com.bloxbean.cardano.yaci.store.snapshot.manifest.Retention.apply(built.manifest(), java.util.Map.of());
        var layout = com.bloxbean.cardano.yaci.store.snapshot.archive.PartLayout.of(built.manifest());
        assertThat(layout.exact()).isTrue();
        var cborParts = built.manifest().parts().stream().filter(p -> p.tables().contains("transaction-cbor")).toList();
        assertThat(cborParts).isNotEmpty().allMatch(p -> p.tables().equals(List.of("transaction-cbor")));

        var with = new SnapshotImporter(registry).selection(built.manifest(), view);
        var without = new SnapshotImporter(registry.withoutDisabledOptionalTables(setting -> false))
                .selection(built.manifest(), view);
        assertThat(with.parts()).containsAll(cborParts);
        assertThat(without.files()).noneMatch(f -> f.startsWith("main/transaction_cbor/"));
        assertThat(without.parts()).doesNotContainAnyElementsOf(cborParts);

        // The snapshot holds transaction bodies (the source ran without save-full-tx-cbor).
        assertThat(new SnapshotImporter(registry).preflight(options(built.manifestPath(), 1, java.util.Map.of(),
                java.util.Map.of("store.transaction.save-full-tx-cbor", "true"))).blockers())
                .anyMatch(b -> b.contains("transaction_cbor was exported with store.transaction.save-full-tx-cbor=false")
                        && b.contains("target runs with store.transaction.save-full-tx-cbor=true"));
    }

    @Test
    void aTrustedSignatureReplacesAllowUnsignedAndABadOneIsNeverAccepted() throws Exception {
        var built = fixture();
        com.bloxbean.cardano.yaci.store.snapshot.archive.Checksums.write(built.manifest(), built.manifestPath());
        var keys = com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotSignature.generate(root.resolve("keys"), "pub");
        var other = com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotSignature.generate(root.resolve("keys"), "other");
        var pub = com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotSignature.readPublicKey(keys.publicKey());
        var otherPub = com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotSignature.readPublicKey(other.publicKey());
        com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotSignature.sign(built.manifestPath(),
                com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotSignature.readPrivateKey(keys.privateKey()), pub);
        var importer = new SnapshotImporter(registry);
        var strict = new ImportOptions(built.manifestPath(), built.archiveDir(), root.resolve("work"),
                PostgresSupport.schemaUrl(), PostgresSupport.user(), PostgresSupport.password(),
                schema, "preprod", 1, 1, 10, 1, "512MB", 0, false, false, List.of(), false, false);

        var trusted = importer.preflight(strict.withTrustedKeys(List.of(pub)));
        assertThat(trusted.blockers()).isEmpty();
        assertThat(trusted.signature().trusted()).isTrue();
        assertThat(importer.preflight(strict).blockers()).anyMatch(b -> b.contains("store.snapshot.trusted-keys"));
        assertThat(importer.preflight(strict.withTrustedKeys(List.of(otherPub))).blockers())
                .anyMatch(b -> b.contains("signature verification failed"));
        assertThat(importer.preflight(options(built.manifestPath(), 1).withTrustedKeys(List.of(otherPub))).blockers())
                .anyMatch(b -> b.contains("signature verification failed"));
    }

    @Test
    void scopedImportReadsOnlyThePartsThatCarryItsTables() throws Exception {
        var built = fixtureWithTableSchemas();
        var selected = registry.selectModules(java.util.Set.of("core", "blocks")).withoutDisabledOptionalTables(setting -> false);
        try (var conn = PostgresSupport.connect(); var st = conn.createStatement()) {
            st.execute("DROP TABLE " + schema + ".address_utxo");
            st.execute("DROP TABLE " + schema + ".adapot");
            st.execute("DROP TABLE " + schema + ".epoch");
            st.execute("DROP TABLE " + schema + ".epoch_stake CASCADE");
        }
        var importer = new SnapshotImporter(selected);
        var pre = importer.preflight(options(built.manifestPath(), 2));
        assertThat(pre.blockers()).isEmpty();
        var selection = importer.selection(pre.archive(), pre.manifest());
        assertThat(selection.parts()).hasSizeLessThan(built.manifest().parts().size());
        assertThat(selection.files()).allMatch(f -> f.startsWith("main/block/") || f.startsWith("main/rollback/"));
        assertThat(pre.warnings()).anyMatch(w -> w.startsWith("Reading " + selection.parts().size() + " of "));

        List<String> messages = new java.util.concurrent.CopyOnWriteArrayList<>();
        importer.importSnapshot(options(built.manifestPath(), 2), messages::add);
        assertThat(messages).anyMatch(m -> m.contains("Extracted " + selection.files().size() + " files from "
                + selection.parts().size() + " parts"));
        try (var conn = PostgresSupport.connect()) {
            for (var report : new SnapshotValidator(selected).validateAll(conn, schema, built.manifest(), 1, 10)) {
                assertThat(report.passed()).withFailMessage(report.toString()).isTrue();
            }
        }
    }

    @Test
    void aTargetWithoutTheBlocksStoreRebuildsTheSameCursorAndErasFromTheBlockFiles() throws Exception {
        var built = fixtureWithTableSchemas();
        String cursorRows = "SELECT block_number || ':' || block_hash || ':' || slot || ':' || era || ':'"
                + " || prev_block_hash FROM " + qt("cursor_") + " ORDER BY block_number";
        String eraRows = "SELECT era || ':' || start_slot || ':' || block || ':' || block_hash FROM "
                + qt("era") + " ORDER BY era";
        List<String> cursor;
        List<String> eras;
        new SnapshotImporter(registry).importSnapshot(options(built.manifestPath(), 1), null);
        try (var conn = PostgresSupport.connect()) {
            cursor = rows(conn, cursorRows);
            eras = rows(conn, eraRows);
            PostgresSupport.resetSchema(conn);
        }

        var selected = registry.selectModules(java.util.Set.of("core", "epoch-aggr"));
        var importer = new SnapshotImporter(selected);
        var pre = importer.preflight(options(built.manifestPath(), 2));
        assertThat(pre.blockers()).isEmpty();
        // The block files are read for the handlers, though no block row is loaded.
        assertThat(importer.selection(pre.archive(), pre.manifest()).files())
                .anyMatch(f -> f.startsWith("main/block/"));
        importer.importSnapshot(options(built.manifestPath(), 2), null);
        try (var conn = PostgresSupport.connect()) {
            assertThat(count(conn, "block")).isZero();
            assertThat(count(conn, "epoch")).isEqualTo(3);
            assertThat(rows(conn, cursorRows)).hasSize(10).isEqualTo(cursor);
            assertThat(rows(conn, eraRows)).hasSize(3).isEqualTo(eras);
            assertThat(scalarLong(conn, "SELECT count(*) FROM pg_tables WHERE schemaname = '" + schema
                    + "' AND tablename LIKE '\\_yaci\\_snapshot\\_import\\_src\\_%'")).isZero();
            for (var report : new SnapshotValidator(selected).validateAll(conn, schema, built.manifest(), 1, 10)) {
                assertThat(report.passed()).withFailMessage(report.toString()).isTrue();
            }
        }
    }

    @Test
    void epochStakeRetentionLoadsOnlyTheNewestEpochsAndValidates() throws Exception {
        var built = fixture();
        var pruning = java.util.Map.of("store.adapot.epoch-stake-pruning-enabled", "true");
        var retention = java.util.Map.of("epoch_stake.epochs", "2");
        var importer = new SnapshotImporter(registry);
        assertThat(importer.preflight(options(built.manifestPath(), 1, retention, pruning)).blockers()).isEmpty();

        importer.importSnapshot(options(built.manifestPath(), 1, retention, pruning), null);

        try (var conn = PostgresSupport.connect()) {
            assertThat(count(conn, "epoch_stake")).isEqualTo(2L * SnapshotFixture.STAKE_ROWS_PER_EPOCH);
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(
                    "SELECT min(epoch) FROM " + Identifiers.quote(schema) + ".epoch_stake")) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(SnapshotFixture.POINT_EPOCH - 1);
            }
            for (var report : new SnapshotValidator(registry).validateAll(conn, schema, built.manifest(), 1, 10)) {
                assertThat(report.passed()).withFailMessage(report.toString()).isTrue();
            }
        }
    }

    @Test
    void epochStakeRetentionNeedsThePruningServiceOnTheTargetAndTheSameRetentionToResume() throws Exception {
        var built = fixture();
        var retention = java.util.Map.of("epoch_stake.epochs", "2");
        var importer = new SnapshotImporter(registry);
        assertThat(importer.preflight(options(built.manifestPath(), 1, retention, java.util.Map.of())).blockers())
                .anyMatch(b -> b.contains("store.adapot.epoch-stake-pruning-enabled=true"));

        var pruning = java.util.Map.of("store.adapot.epoch-stake-pruning-enabled", "true");
        assertThatThrownBy(() -> importer.importSnapshot(options(built.manifestPath(), 1, retention, pruning), p -> {
            if (p.startsWith("block +")) throw new IllegalStateException("interrupt");
        })).hasMessageContaining("interrupt");
        assertThat(importer.preflight(options(built.manifestPath(), 1, java.util.Map.of(), pruning)).blockers())
                .anyMatch(b -> b.contains("Import retention does not match"));
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
            // The manifest pins only the export contract; the journal keeps the transform that loaded.
            var addressUtxo = registry.byId("address-utxo").orElseThrow();
            assertThat(new ImportJournal(conn, schema).transforms(built.manifest().snapshotId())).hasValueSatisfying(
                    t -> assertThat(t).contains("address-utxo=v" + addressUtxo.importSpec().transformVersion()
                            + ":" + addressUtxo.digest()));

            // MAPPED with a timestamp conversion and three jsonb conversions.
            assertThat(count(conn, "block")).isEqualTo(SnapshotFixture.POINT_BLOCK + 1);
            assertThat(scalarLong(conn, "SELECT block_time FROM " + qt("block")
                    + " WHERE number = 0")).isEqualTo(1700000000L);
            assertThat(scalarString(conn, "SELECT jsonb_typeof(leader_vrf) FROM " + qt("block")
                    + " WHERE number = 0")).isEqualTo("object");

            // Frozen before the rebuild: every page all-visible (only VACUUM sets it), nothing left for autovacuum.
            assertThat(scalarString(conn, "SELECT relpages > 0 AND relallvisible = relpages FROM pg_class"
                    + " WHERE oid = '" + qt("block") + "'::regclass")).isEqualTo("t");

            // MAPPED with a rename: epoch.number comes from the export's epoch column.
            assertThat(count(conn, "epoch")).isEqualTo(3);
            assertThat(scalarLong(conn, "SELECT max(number) FROM " + qt("epoch"))).isEqualTo(2);

            // SQL mode: regrouped one row per (tx_hash, output_index), rebuilt amounts, joined block.
            assertThat(count(conn, "address_utxo")).isEqualTo(SnapshotFixture.POINT_BLOCK + 1);
            assertThat(scalarLong(conn, "SELECT jsonb_array_length(amounts) FROM " + qt("address_utxo")
                    + " WHERE slot = 0")).isEqualTo(4);
            assertThat(scalarString(conn, "SELECT amounts->0->>'unit' FROM " + qt("address_utxo")
                    + " WHERE slot = 0")).isEqualTo("lovelace");
            // The source element order is kept, even where it is not canonical (longname before tok).
            assertThat(scalarString(conn, "SELECT string_agg(e->>'asset_name', ',' ORDER BY o) FROM "
                    + qt("address_utxo") + " u, jsonb_array_elements(u.amounts) WITH ORDINALITY t(e, o)"
                    + " WHERE u.slot = 0")).isEqualTo("lovelace,,longname,tok");
            // The analytics view maps an empty asset name to NULL; the transform restores the empty
            // string the operational writer stores.
            assertThat(scalarLong(conn, "SELECT count(*) FROM " + qt("address_utxo") + " u,"
                    + " jsonb_array_elements(u.amounts) e WHERE e->>'asset_name' = ''")).isPositive();
            assertThat(scalarLong(conn, "SELECT count(*) FROM " + qt("address_utxo") + " u,"
                    + " jsonb_array_elements(u.amounts) e"
                    + " WHERE e->>'unit' <> 'lovelace' AND e->>'asset_name' IS NULL")).isZero();
            assertThat(scalarLong(conn, "SELECT lovelace_amount FROM " + qt("address_utxo")
                    + " WHERE slot = 0")).isEqualTo(1000000L);
            assertThat(scalarLong(conn, "SELECT block FROM " + qt("address_utxo")
                    + " WHERE slot = 0")).isZero();
            // owner_addr_full comes back where the export has it; the older day's file, written before
            // the export had the column, restores it as NULL.
            assertThat(scalarLong(conn, "SELECT count(*) FROM " + qt("address_utxo")
                    + " WHERE owner_addr_full IS NOT NULL")).isOne();
            assertThat(scalarLong(conn, "SELECT length(owner_addr_full) FROM " + qt("address_utxo")
                    + " WHERE slot = " + SnapshotFixture.LONG_ADDRESS_OUTPUT * 20)).isEqualTo(600L);

            // cursor_ tail seeded to the configured depth, ending exactly at the point.
            assertThat(count(conn, "cursor_")).isEqualTo(10);
            assertThat(scalarLong(conn, "SELECT max(block_number) FROM " + qt("cursor_")))
                    .isEqualTo(SnapshotFixture.POINT_BLOCK);
            assertThat(scalarString(conn, "SELECT block_hash FROM " + qt("cursor_")
                    + " ORDER BY slot DESC LIMIT 1"))
                    .isEqualTo(SnapshotFixture.hashOf(SnapshotFixture.POINT_BLOCK));

            // era rebuilt from the first block of each era, with Byron excluded: a Byron row makes
            // EraService.getEraForEpoch() throw and breaks the first reward calculation after a restore.
            assertThat(count(conn, "era")).isEqualTo(3);
            assertThat(scalarLong(conn, "SELECT block FROM " + qt("era") + " WHERE era = 7"))
                    .isEqualTo(20);
            assertThat(scalarLong(conn, "SELECT count(*) FROM " + qt("era") + " WHERE era <= 1"))
                    .isZero();

            // The account balance watermark gates AccountBalanceProcessor; at 0 it skips every
            // calculation and starves the AdaPot stake snapshot.
            assertThat(scalarLong(conn, "SELECT block FROM " + qt("account_config")
                    + " WHERE config_id = 'last_account_balance_processed_block'"))
                    .isEqualTo(SnapshotFixture.POINT_BLOCK);

            // AdaPot jobs marked complete only up to the snapshot epoch.
            assertThat(scalarLong(conn, "SELECT max(epoch) FROM " + qt("adapot_jobs")))
                    .isEqualTo(SnapshotFixture.POINT_EPOCH);
            assertThat(scalarString(conn, "SELECT DISTINCT status FROM " + qt("adapot_jobs")))
                    .isEqualTo("COMPLETED");
        }

        assertThat(report.handlerResults()).hasSize(4);
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
    void importAndValidationAnnounceEachPhaseOnce() throws Exception {
        SnapshotFixture.Built built = fixture();
        List<String> events = new java.util.concurrent.CopyOnWriteArrayList<>();
        new SnapshotImporter(registry).importSnapshot(options(built.manifestPath(), 2), events::add);
        try (Connection conn = PostgresSupport.connect()) {
            new SnapshotValidator(registry).validateAll(conn, schema, built.manifest(), 1, 10,
                    PostgresSupport::connect, 2, events::add);
        }
        events.forEach(System.out::println);

        List<String> banners = events.stream().filter(e -> e.startsWith(Progress.PHASE_PREFIX))
                .map(e -> e.substring(Progress.PHASE_PREFIX.length())).toList();
        assertThat(banners).hasSize(SnapshotImporter.PHASES + SnapshotValidator.PHASES);
        assertThat(banners.get(0)).startsWith("Verifying and extracting " + built.manifest().parts().size() + " parts");
        assertThat(banners.get(1)).startsWith("Preparing");
        assertThat(banners.get(2)).startsWith("Loading " + String.format("%,d", built.manifest().tables().stream()
                .filter(t -> registry.isSelected(t.specId())).mapToLong(SnapshotManifest.TableManifest::rowCount).sum()));
        assertThat(banners.get(3)).startsWith("Freezing");
        assertThat(banners.get(4)).startsWith("Rebuilding");
        assertThat(banners.get(5)).startsWith("Analyzing");
        assertThat(banners.get(6)).startsWith("Control state");
        assertThat(banners.get(7)).startsWith("Schema and load checks");
        assertThat(banners.get(8)).startsWith("Semantic checks");
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

            // Scanning in parallel reports exactly the same checks.
            List<ValidationReport> parallel = new SnapshotValidator(registry).validateAll(conn, schema,
                    built.manifest(), 1, 10, PostgresSupport::connect, 3, m -> { });
            assertThat(parallel).extracting(ValidationReport::checks)
                    .isEqualTo(reports.stream().map(ValidationReport::checks).toList());
            // The rebuilt primary key already proves uniqueness, so no GROUP BY is run.
            assertThat(reports.get(1).checks())
                    .filteredOn(c -> c.name().equals("address-utxo-unique"))
                    .singleElement()
                    .satisfies(c -> assertThat(c.detail()).startsWith("enforced by unique index "));
        }
    }

    // ------------------------------------------------------------------ resume

    @Test
    void aResumedImportSkipsCommittedBatchesAndDoesNotDuplicateRows() throws Exception {
        SnapshotFixture.Built full = fixture();
        String snapshotId = full.manifest().snapshotId();
        // Fail after a committed batch, keeping the original manifest and archive unchanged.
        assertThatThrownBy(() -> new SnapshotImporter(registry)
                .importSnapshot(options(full.manifestPath(), 1), progress -> {
                    if (progress.startsWith("block +")) {
                        throw new IllegalStateException("simulated interruption after block commit");
                    }
                })).isInstanceOf(IllegalStateException.class).hasMessageContaining("simulated interruption");

        long blocksAfterFirst;
        long completedAfterFirst;
        try (Connection conn = PostgresSupport.connect()) {
            blocksAfterFirst = count(conn, "block");
            completedAfterFirst = new ImportJournal(conn, schema).completedCount(snapshotId);
        }
        assertThat(blocksAfterFirst).isEqualTo(SnapshotFixture.POINT_BLOCK + 1);
        assertThat(completedAfterFirst).isPositive();

        ImportReport second = new SnapshotImporter(registry)
                .importSnapshot(options(full.manifestPath(), 1), null);

        assertThat(second.batchesSkipped()).isEqualTo(completedAfterFirst);
        assertThat(second.batchesLoaded()).isGreaterThan(0);

        try (Connection conn = PostgresSupport.connect()) {
            assertThat(count(conn, "block")).isEqualTo(blocksAfterFirst);
            assertThat(count(conn, "address_utxo")).isEqualTo(SnapshotFixture.POINT_BLOCK + 1);
            assertThat(scalarLong(conn, "SELECT count(*) FROM (SELECT hash FROM " + qt("block")
                    + " GROUP BY hash HAVING count(*) > 1) d")).isZero();
            assertThat(scalarLong(conn, "SELECT count(*) FROM (SELECT tx_hash FROM " + qt("address_utxo")
                    + " GROUP BY tx_hash, output_index HAVING count(*) > 1) d")).isZero();
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

    /** A dry run plans the SQL transform's output against the target before anything is extracted. */
    @Test
    void preflightRejectsAnSqlTransformWhoseOutputDoesNotFitTheTarget() throws Exception {
        SnapshotFixture.Built built = fixture();
        try (var conn = PostgresSupport.connect(); var st = conn.createStatement()) {
            st.execute("ALTER TABLE " + Identifiers.quote(schema) + ".address_utxo ALTER COLUMN tx_index TYPE smallint");
        }
        SnapshotImporter.Preflight pre = new SnapshotImporter(registry).preflight(options(built.manifestPath(), 1));
        assertThat(pre.blockers()).anySatisfy(b -> assertThat(b)
                .contains("'address-utxo'").contains("tx_index").contains("narrowing 'INTEGER' to 'smallint'"));
        assertThat(root.resolve("work").resolve("extracted")).doesNotExist();
    }

    /**
     * A manifest pins the export contract, so a release whose import transform differs still loads
     * it, and says so; a different contract at the same spec-version is still refused.
     */
    @Test
    void acceptsAChangedImportTransformButNotAChangedExportContract() throws Exception {
        SnapshotFixture.Built built = fixture();
        SnapshotManifest exportedEarlier = replaceTables(built.manifest(), built.manifest().tables().stream()
                .map(t -> t.specId().equals("address-utxo") ? withDigests(t, "0".repeat(64), t.contractDigest()) : t)
                .toList());
        Path path = built.archiveDir().resolve("earlier.manifest.json");
        new ManifestCodec().writeAtomically(exportedEarlier, path);
        SnapshotImporter.Preflight pre = new SnapshotImporter(registry).preflight(options(path, 1));
        assertThat(pre.blockers()).isEmpty();
        assertThat(pre.warnings()).anySatisfy(w -> assertThat(w)
                .contains("'address-utxo'").contains("export contract matches"));

        SnapshotManifest otherContract = replaceTables(built.manifest(), built.manifest().tables().stream()
                .map(t -> t.specId().equals("address-utxo") ? withDigests(t, t.specDigest(), "0".repeat(64)) : t)
                .toList());
        new ManifestCodec().writeAtomically(otherContract, path);
        assertThat(new SnapshotImporter(registry).preflight(options(path, 1)).blockers())
                .anySatisfy(b -> assertThat(b).contains("'address-utxo'").contains("export contract"));
    }

    private static SnapshotManifest.TableManifest withDigests(SnapshotManifest.TableManifest t, String specDigest,
                                                              String contractDigest) {
        return new SnapshotManifest.TableManifest(t.specId(), t.specVersion(), specDigest,
                t.module(), t.kind(), t.restore(), t.reason(), t.sourceRelation(), t.exporterId(),
                t.targetTable(), t.loadMode(), t.cutoffRule(), t.transformVersion(),
                t.dependencies(), t.rowCount(), t.key(), t.columnFingerprint(),
                t.sourceColumns(), t.bounds(), t.files(), contractDigest);
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

        assertThatThrownBy(() -> new SnapshotImporter(registry)
                .importSnapshot(options(built.manifestPath(), 1), null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Archive verification failed")
                .hasMessageContaining(built.manifest().parts().get(0).fileName());
        try (Connection conn = PostgresSupport.connect()) {
            assertThat(count(conn, "block")).isZero();
            assertThat(new ImportJournal(conn, schema).exists()).isFalse();
        }
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

    @Test
    void refusesMissingAndDuplicateManifestTablesBeforeWriting() throws Exception {
        SnapshotFixture.Built built = fixture();
        List<SnapshotManifest.TableManifest> missing = built.manifest().tables().stream()
                .filter(t -> !t.specId().equals("delegation")).toList();
        new ManifestCodec().writeAtomically(replaceTables(built.manifest(), missing), built.manifestPath());
        assertThat(new SnapshotImporter(registry).preflight(options(built.manifestPath(), 1)).blockers())
                .anyMatch(p -> p.contains("Missing manifest specification 'delegation'"));
        try (Connection conn = PostgresSupport.connect()) {
            assertThat(new SnapshotValidator(registry).validateLoad(conn, schema,
                    replaceTables(built.manifest(), missing)).passed()).isFalse();
            assertThat(new ImportJournal(conn, schema).exists()).isFalse();
        }
        var duplicate = new java.util.ArrayList<>(built.manifest().tables());
        duplicate.add(duplicate.get(0));
        new ManifestCodec().writeAtomically(replaceTables(built.manifest(), duplicate), built.manifestPath());
        assertThat(new SnapshotImporter(registry).preflight(options(built.manifestPath(), 1)).blockers())
                .anyMatch(p -> p.contains("Duplicate manifest specification"));
    }

    @Test
    void resumeAndValidationRequireTheOriginalManifestDigest() throws Exception {
        SnapshotFixture.Built built = fixture();
        new SnapshotImporter(registry).importSnapshot(options(built.manifestPath(), 1), null);
        ManifestCodec codec = new ManifestCodec();
        // Same snapshot ID, files, and spec versions, but a different manifest.
        Files.writeString(built.manifestPath(), codec.toJson(built.manifest())
                .replace("2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z"));
        assertThatThrownBy(() -> new SnapshotImporter(registry)
                .importSnapshot(options(built.manifestPath(), 1), null))
                .hasMessageContaining("manifest digest");
        try (Connection conn = PostgresSupport.connect()) {
            assertThat(new SnapshotValidator(registry).validateLoad(conn, schema,
                    codec.read(built.manifestPath())).passed()).isFalse();
            assertThat(count(conn, "block")).isEqualTo(SnapshotFixture.POINT_BLOCK + 1);
        }
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

    private static List<String> rows(Connection conn, String sql) throws SQLException {
        List<String> out = new java.util.ArrayList<>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.add(rs.getString(1));
            }
        }
        return out;
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
