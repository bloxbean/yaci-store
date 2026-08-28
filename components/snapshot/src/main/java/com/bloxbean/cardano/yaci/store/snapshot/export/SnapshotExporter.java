package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveVerifier;
import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveWriter;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestCodec;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.RestoreMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Packages an already-produced, pinned DuckLake file set into snapshot archives.
 *
 * <p>Export never reruns or replaces an analytics exporter: it reads the immutable files an exporter
 * already wrote, at one pinned catalog snapshot.
 */
public class SnapshotExporter {

    private static final Logger log = LoggerFactory.getLogger(SnapshotExporter.class);

    private final SnapshotSpecRegistry registry;

    public SnapshotExporter(SnapshotSpecRegistry registry) {
        this.registry = registry;
    }

    /** Read-only analysis: candidate point, coverage, lossy mappings, estimated size and blockers. */
    public InspectionReport inspect(ExportOptions options) throws SQLException, IOException {
        try (DuckLakeCatalog catalog = DuckLakeCatalog.open(options.dataDir(), options.workDir(), true)) {
            return inspect(catalog, options);
        }
    }

    InspectionReport inspect(DuckLakeCatalog catalog, ExportOptions options) throws SQLException {
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        long snapshotId = catalog.latestSnapshotId();
        if (snapshotId < 0) {
            blockers.add("The DuckLake catalog contains no snapshot");
        }
        long deletes = snapshotId < 0 ? 0 : catalog.deleteFileCount(snapshotId);
        if (deletes > 0) {
            blockers.add("The pinned catalog snapshot references " + deletes + " deletion file(s). "
                    + "The initial snapshot format requires immutable, append-only Parquet.");
        }

        List<SnapshotTableSpec> imported = registry.importedTables();
        List<SnapshotTableSpec> others = registry.all().stream()
                .filter(s -> s.restore() != RestoreMode.IMPORT)
                .toList();

        List<String> availableRelations = snapshotId < 0 ? List.of() : catalog.relations(snapshotId);
        for (SnapshotTableSpec spec : imported) {
            if (!availableRelations.contains(spec.relation())) {
                blockers.add("Spec '" + spec.id() + "' requires DuckLake relation '" + spec.relation()
                        + "' which is not present");
            }
        }

        ConsistencyPointSelector selector = new ConsistencyPointSelector(catalog);
        ConsistencyPointSelector.Selection selection = snapshotId < 0
                ? new ConsistencyPointSelector.Selection(-1, null, Map.of(), List.of(), -1,
                List.of("No catalog snapshot"))
                : selector.select(imported, snapshotId, options.network(), options.protocolMagic(),
                options.minConfirmations());
        blockers.addAll(selection.blockers());

        List<TablePlan> plans = new ArrayList<>();
        if (selection.point() != null) {
            ExportPlanner planner = new ExportPlanner(catalog);
            for (SnapshotTableSpec spec : imported) {
                if (!availableRelations.contains(spec.relation())) {
                    continue;
                }
                TablePlan plan = planner.plan(spec, snapshotId, selection.completedEpoch(),
                        selection.point().slot());
                plans.add(plan);
                plan.problems().forEach(p -> blockers.add("[" + spec.id() + "] " + p));
                if (plan.rowCount() == 0) {
                    warnings.add("Table '" + spec.id() + "' contributes no rows at the consistency point");
                }
            }
        }

        // Declared limitations, gathered from the specifications themselves so they can never drift
        // from what the importer actually does. address_utxo.owner_addr_full is the known example.
        List<String> lossy = new ArrayList<>();
        for (SnapshotTableSpec spec : registry.all()) {
            if (spec.restore() == RestoreMode.NOT_RESTORED) {
                lossy.add(spec.targetTable() + ": " + spec.reason());
            }
            spec.lossy().forEach((column, reason) ->
                    lossy.add(spec.targetTable() + "." + column + ": " + reason));
        }
        lossy.sort(String::compareTo);

        return new InspectionReport(snapshotId, catalog.duckdbVersion(), catalog.metadata(),
                selection.completedEpoch(), selection.point(), selection.gating(), selection.limitedBy(),
                selection.maxExportedBlock(), plans, others, lossy, blockers, warnings);
    }

    public record ExportResult(Path manifestPath, SnapshotManifest manifest, InspectionReport report,
                               long bytesWritten, long millis) {}

    /**
     * Package the snapshot. Every part is re-read and checksummed after writing, and the manifest is
     * published last with an atomic rename, so a manifest on disk always describes complete parts.
     */
    public ExportResult export(ExportOptions options, Consumer<String> progress) throws SQLException, IOException {
        long started = System.currentTimeMillis();
        try (DuckLakeCatalog catalog = DuckLakeCatalog.open(options.dataDir(), options.workDir(), true)) {
            InspectionReport report = inspect(catalog, options);
            if (!report.canExport()) {
                throw new IllegalStateException("Snapshot export is blocked:\n  - "
                        + String.join("\n  - ", report.blockers()));
            }
            if (!report.declaredLossy().isEmpty() && !options.allowIncomplete()) {
                throw new IllegalStateException("This snapshot has declared limitations and is not "
                        + "production-ready:\n  - " + String.join("\n  - ", report.declaredLossy())
                        + "\nRe-run with --allow-incomplete to package it anyway.");
            }

            String snapshotId = UUID.randomUUID().toString();
            String baseName = String.format("yaci-%s-e%d-%s", options.network(),
                    report.completedEpoch(), snapshotId.substring(0, 8));

            // Pre-scan: one pass over the file set produces the digests, sizes and CRCs the archive
            // writer and manifest both need.
            List<SnapshotManifest.FileEntry> allEntries = new ArrayList<>();
            List<String> owners = new ArrayList<>();
            List<SnapshotManifest.TableManifest> tableManifests = new ArrayList<>();

            for (TablePlan plan : report.plans()) {
                List<SnapshotManifest.FileEntry> entries = new ArrayList<>();
                for (DuckLakeFile f : plan.files()) {
                    Path abs = options.dataDir().resolve(f.relativePath());
                    if (!Files.isRegularFile(abs)) {
                        throw new IOException("Catalog references a missing file: " + f.relativePath());
                    }
                    SnapshotManifest.FileEntry fe = new SnapshotManifest.FileEntry(
                            f.relativePath(), Files.size(abs), Digests.sha256Hex(abs), f.rowCount(), f.partition());
                    entries.add(fe);
                    allEntries.add(fe);
                    owners.add(plan.spec().id());
                    if (progress != null) {
                        progress.accept("digest " + f.relativePath());
                    }
                }
                tableManifests.add(toTableManifest(plan, entries));
            }

            for (SnapshotTableSpec spec : report.nonImportedTables()) {
                tableManifests.add(new SnapshotManifest.TableManifest(
                        spec.id(), spec.specVersion(), spec.digest(), spec.module(), spec.kind().name(),
                        spec.restore().name(), spec.reason(), null, null, spec.targetTable(), null, null,
                        0, List.of(), 0L, List.of(), null, Map.of(), Map.of(), List.of()));
            }

            ArchiveWriter writer = new ArchiveWriter(options.dataDir(), options.outputDir(), baseName,
                    options.partSizeBytes());
            List<SnapshotManifest.PartManifest> parts =
                    writer.write(allEntries, owners, progress == null ? null : p -> progress.accept("pack " + p));

            SnapshotManifest manifest = new SnapshotManifest(
                    ManifestCodec.FORMAT_VERSION,
                    snapshotId,
                    Instant.now().toString(),
                    "yaci-store-admin-cli",
                    options.yaciStoreVersion(),
                    ManifestCodec.SPEC_FORMAT_VERSION,
                    report.duckdbVersion(),
                    report.catalogMetadata().getOrDefault("version", "unknown"),
                    report.point(),
                    options.genesisHash(),
                    options.modules(),
                    options.pruningSettings(),
                    options.schemaFingerprint(),
                    options.flywayFingerprint(),
                    tableManifests,
                    parts,
                    report.declaredLossy());

            // Re-read every part before publishing the manifest.
            ArchiveVerifier.Result verified =
                    new ArchiveVerifier().verify(manifest, options.outputDir(), true);
            if (!verified.ok()) {
                throw new IOException("Written parts failed verification:\n  - "
                        + String.join("\n  - ", verified.problems()));
            }

            Path manifestPath = options.outputDir().resolve(baseName + ".manifest.json");
            ManifestCodec codec = new ManifestCodec();
            codec.writeAtomically(manifest, manifestPath);
            writeChecksums(options.outputDir(), baseName, manifest, manifestPath, codec);

            long bytes = parts.stream().mapToLong(SnapshotManifest.PartManifest::size).sum();
            log.info("Exported snapshot {} ({} parts, {} bytes)", snapshotId, parts.size(), bytes);
            return new ExportResult(manifestPath, manifest, report, bytes, System.currentTimeMillis() - started);
        }
    }

    private SnapshotManifest.TableManifest toTableManifest(TablePlan plan,
                                                           List<SnapshotManifest.FileEntry> entries) {
        SnapshotTableSpec spec = plan.spec();
        SnapshotTableSpec.CutoffRule cut = spec.consistency().cutoff();
        String cutoffRule = cut.type() + (cut.column() == null ? "" : "(" + cut.column()
                + (cut.offset() != 0 ? ", offset " + cut.offset() : "") + ")");
        return new SnapshotManifest.TableManifest(
                spec.id(), spec.specVersion(), spec.digest(), spec.module(), spec.kind().name(),
                spec.restore().name(), spec.reason(), spec.relation(), spec.source().exporterId(),
                spec.targetTable(), spec.importSpec().mode().name(), cutoffRule,
                spec.importSpec().transformVersion(), spec.importSpec().dependencies(),
                plan.rowCount(), spec.validation().key(), plan.columnFingerprint(),
                plan.sourceColumns(), plan.bounds(), entries);
    }

    /**
     * {@code SHA256SUMS} in the usual {@code sha256sum -c} format, so parts can be verified with
     * standard tools before any Yaci Store code runs.
     */
    private void writeChecksums(Path dir, String baseName, SnapshotManifest manifest,
                                Path manifestPath, ManifestCodec codec) throws IOException {
        StringBuilder sb = new StringBuilder();
        Map<String, String> sums = new TreeMap<>();
        manifest.parts().forEach(p -> sums.put(p.fileName(), p.sha256()));
        sums.put(manifestPath.getFileName().toString(), Digests.sha256Hex(manifestPath));
        sums.forEach((name, sha) -> sb.append(sha).append("  ").append(name).append('\n'));
        Files.writeString(dir.resolve("SHA256SUMS"), sb.toString());
    }
}
