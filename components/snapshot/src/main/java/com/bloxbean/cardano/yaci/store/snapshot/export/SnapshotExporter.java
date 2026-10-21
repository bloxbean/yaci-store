package com.bloxbean.cardano.yaci.store.snapshot.export;

import java.time.ZoneOffset;
import java.time.LocalDate;
import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveVerifier;
import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveWriter;
import com.bloxbean.cardano.yaci.store.snapshot.archive.Parallel;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestCodec;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.RestoreMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import com.bloxbean.cardano.yaci.store.snapshot.util.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
    private boolean verifyParts;

    public SnapshotExporter(SnapshotSpecRegistry registry) {
        this.registry = registry;
    }

    /**
     * Re-reads every written part and checks its digest before publishing the manifest. Off by
     * default: each part is hashed while it is written, and the import checks every part and file
     * digest again while extracting, so on mainnet this pass only repeated 2.5 minutes of hashing.
     */
    public SnapshotExporter verifyParts(boolean verify) {
        this.verifyParts = verify;
        return this;
    }

    /** Phase banners {@link #export} sends, so a caller can number them. */
    public int phases() {
        return verifyParts ? 4 : 3;
    }

    /** As {@link #phases()}, for an export with {@code options}: a history split is a phase of its own. */
    public int phases(ExportOptions options) {
        return phases() + (options.historySplit() ? 1 : 0);
    }

    /** Read-only analysis: candidate point, coverage, lossy mappings, estimated size and blockers. */
    public InspectionReport inspect(ExportOptions options) throws SQLException, IOException {
        try (DuckLakeCatalog catalog = DuckLakeCatalog.open(options.dataDir(), options.workDir(), options.memoryLimit())) {
            return inspect(catalog, options);
        }
    }

    InspectionReport inspect(DuckLakeCatalog catalog, ExportOptions options) throws SQLException {
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> predatingColumns = new ArrayList<>();

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
                options.minConfirmations(), options.targetEpoch());
        blockers.addAll(selection.blockers());

        List<TablePlan> plans = new ArrayList<>();
        Map<String, List<String>> incompletePartitions = new java.util.LinkedHashMap<>();
        if (selection.point() != null) {
            ExportPlanner planner = new ExportPlanner(catalog);
            var firstBlock = catalog.numericBounds("block", "number", snapshotId);
            if (firstBlock.isEmpty() || firstBlock.get()[0] > 1) {
                blockers.add("Block export must include the beginning of the chain; pruned exports are unsupported");
            }
            var timeBounds = catalog.bounds("block", "block_time", snapshotId).orElseThrow(
                    () -> new IllegalStateException("Block export has no block_time bounds"));
            LocalDate firstDay = LocalDate.parse(timeBounds.min().substring(0, 10));
            LocalDate lastDay = Instant.ofEpochSecond(selection.point().blockTime())
                    .atOffset(ZoneOffset.UTC).toLocalDate();
            int firstNonByronEpoch = catalog.firstNonByronEpoch(snapshotId).orElse(-1);
            for (SnapshotTableSpec spec : imported) {
                if (!availableRelations.contains(spec.relation())) {
                    continue;
                }
                ExportCoverage.Result coverage = ExportCoverage.evaluate(spec, catalog.files(spec.relation(), snapshotId),
                        options.completedPartitions().getOrDefault(spec.source().exporterId(), Map.of()),
                        firstDay, lastDay, firstNonByronEpoch, selection.completedEpoch(), selection.point().slot());
                blockers.addAll(coverage.blockers());
                if (!coverage.partitions().isEmpty()) {
                    incompletePartitions.put(spec.id(), coverage.partitions());
                }
                TablePlan plan = planner.plan(spec, snapshotId, selection.completedEpoch(),
                        selection.point().slot());
                plans.add(plan);
                catalog.filesPredatingColumns(spec.relation(), snapshotId).forEach((column, files) ->
                        predatingColumns.add(spec.targetTable() + ": " + files + " of its exported files predate the"
                                + " export column '" + column + "' and restore it as NULL; re-export those"
                                + " partitions to fill it"));
                plan.problems().forEach(p -> blockers.add("[" + spec.id() + "] " + p));
                if (plan.rowCount() == 0) {
                    warnings.add("Table '" + spec.id() + "' contributes no rows at the consistency point");
                }
            }
        }

        // Plan every table against its target in the producing release's schema, and prepare each
        // SQL transform, before declaring the directory exportable. Without a target only the
        // transforms are prepared.
        if (selection.point() != null) {
            ExportPlanner planner = new ExportPlanner(catalog);
            Map<String, TablePlan> byId = new java.util.LinkedHashMap<>();
            plans.forEach(p -> byId.put(p.spec().id(), p));
            for (TablePlan plan : plans) {
                planner.validateImport(plan, byId, options.targetTables().get(plan.spec().targetTable()),
                                selection.completedEpoch(), selection.point().slot())
                        .forEach(problem -> blockers.add("[" + plan.spec().id() + "] " + problem));
            }
        }

        // Declared limitations, gathered from the specifications themselves so they can never drift
        // from what the importer actually does, and from this export's files.
        List<String> lossy = new ArrayList<>(predatingColumns);
        for (SnapshotTableSpec spec : registry.all()) {
            if (spec.restore() == RestoreMode.NOT_RESTORED) {
                lossy.add(spec.targetTable() + ": " + spec.reason());
            }
            spec.lossy().forEach((column, reason) ->
                    lossy.add(spec.targetTable() + "." + column + ": " + reason));
            if (spec.lossyNote() != null) {
                lossy.add(spec.targetTable() + ": " + spec.lossyNote());
            }
        }
        lossy.sort(String::compareTo);

        return new InspectionReport(snapshotId, catalog.duckdbVersion(), catalog.metadata(),
                selection.completedEpoch(), selection.point(), selection.gating(), selection.limitedBy(),
                selection.maxExportedBlock(), plans, others, lossy, blockers, warnings, incompletePartitions);
    }

    /** Export refused by the inspection; the report carries the full blocker details. */
    public static final class BlockedException extends IllegalStateException {
        private final transient InspectionReport report;

        BlockedException(InspectionReport report) {
            super("Snapshot export is blocked:\n  - " + String.join("\n  - ", report.blockers()));
            this.report = report;
        }

        public InspectionReport report() {
            return report;
        }
    }

    public record ExportResult(Path manifestPath, SnapshotManifest manifest, InspectionReport report,
                               long bytesWritten, long millis) {}

    /**
     * Package the snapshot. Every part is re-read and checksummed after writing, and the manifest is
     * published last with an atomic rename, so a manifest on disk always describes complete parts.
     */
    public ExportResult export(ExportOptions options, Consumer<String> progress) throws SQLException, IOException {
        long started = System.currentTimeMillis();
        try (DuckLakeCatalog catalog = DuckLakeCatalog.open(options.dataDir(), options.workDir(), options.memoryLimit())) {
            Progress.phase(progress, "Selecting the consistency point and planning coverage");
            InspectionReport report = inspect(catalog, options);
            if (!report.canExport()) {
                throw new BlockedException(report);
            }
            if (!report.declaredLossy().isEmpty() && !options.allowIncomplete()) {
                throw new IllegalStateException("This snapshot has declared limitations and is not "
                        + "production-ready:\n  - " + String.join("\n  - ", report.declaredLossy())
                        + "\nRe-run with --allow-incomplete to package it anyway.");
            }

            String snapshotId = UUID.randomUUID().toString();
            String baseName = String.format("yaci-%s-e%d-%s", options.network(),
                    report.completedEpoch(), snapshotId.substring(0, 8));

            // The history split adds a pruned copy of the UTxO relations, packaged from the work directory;
            // every original file is still packaged, marked full-only.
            List<TablePlan> planned = report.plans();
            Map<String, Path> rewritten = Map.of();
            Map<String, TieredExport.Tiers> tiers = Map.of();
            if (options.historySplit()) {
                Progress.phase(progress, "Adding the pruned tier: address_utxo and tx_input as a pruning node holds them");
                TieredExport.Result split = new TieredExport(catalog, options.workDir(), options.memoryLimit(), progress)
                        .apply(planned, report.point(), report.ducklakeSnapshotId(), options.utxoSafeBlocks(),
                                options.epochStakeEpochs());
                planned = split.plans();
                rewritten = split.rewritten();
                tiers = split.tiers();
            }
            Map<String, Path> sources = rewritten;
            java.util.function.Function<String, Path> source = rel -> {
                Path p = sources.get(rel);
                return p != null ? p : options.dataDir().resolve(rel);
            };

            // Pre-scan: one read per file produces the SHA-256 for the manifest and the CRC-32 the
            // archive writer needs. Files are independent, so several are read at once.
            int threads = ArchiveVerifier.DEFAULT_THREADS;
            // Grouped by store module, each module's tables in their usual order, so parts can break
            // at module boundaries and a scoped import skips whole parts.
            List<TablePlan> plans = planned.stream()
                    .sorted(java.util.Comparator.comparing((TablePlan pl) -> pl.spec().module())).toList();
            List<DuckLakeFile> scanFiles = plans.stream().flatMap(pl -> pl.files().stream()).toList();
            long scanBytes = scanFiles.stream().mapToLong(DuckLakeFile::sizeBytes).sum();
            Progress.phase(progress, String.format("Hashing %,d files (%s) with %d threads",
                    scanFiles.size(), bytes(scanBytes), threads));
            long scanStart = System.currentTimeMillis();
            AtomicInteger scanned = new AtomicInteger();
            AtomicLong scannedBytes = new AtomicLong();
            List<Digests.FileDigest> digests;
            try (Progress.Ticker ignored = Progress.every(Progress.INTERVAL_MILLIS, "digest",
                    () -> String.format("%,d/%,d files  %s", scanned.get(), scanFiles.size(),
                            Progress.rate(scannedBytes.get(), scanBytes, scanStart, System.currentTimeMillis())),
                    progress)) {
                digests = Parallel.map(scanFiles, threads, f -> {
                    Path abs = source.apply(f.relativePath());
                    if (!Files.isRegularFile(abs)) {
                        throw new IOException("Catalog references a missing file: " + f.relativePath());
                    }
                    Digests.FileDigest d = Digests.sha256AndCrc32(abs);
                    scanned.incrementAndGet();
                    scannedBytes.addAndGet(d.size());
                    return d;
                });
            } catch (Parallel.PartFailure e) {
                throw e.getCause() instanceof IOException io ? io : new IOException(e.getMessage(), e.getCause());
            }
            say(progress, "digest", String.format("Hashed %,d files in %s", scanFiles.size(),
                    Progress.duration(System.currentTimeMillis() - scanStart)));

            List<SnapshotManifest.FileEntry> allEntries = new ArrayList<>();
            List<String> owners = new ArrayList<>();
            List<String> modules = new ArrayList<>();
            Map<String, Long> crcs = new HashMap<>();
            List<SnapshotManifest.TableManifest> tableManifests = new ArrayList<>();
            int at = 0;
            for (TablePlan plan : plans) {
                TieredExport.Tiers tier = tiers.get(plan.spec().id());
                List<SnapshotManifest.FileEntry> entries = new ArrayList<>();
                for (DuckLakeFile f : plan.files()) {
                    Digests.FileDigest d = digests.get(at++);
                    String fileTier = tier == null ? null : tier.of(f.relativePath());
                    SnapshotManifest.FileEntry fe = new SnapshotManifest.FileEntry(
                            f.relativePath(), d.size(), d.sha256(), f.rowCount(), f.partition(), fileTier);
                    entries.add(fe);
                    allEntries.add(fe);
                    owners.add(plan.spec().id());
                    // Each tier, and each optional table (ADR 0006), goes in parts of its own, so an import
                    // that skips it skips whole parts.
                    modules.add(plan.spec().module() + (plan.spec().optional() ? "/" + plan.spec().targetTable() : "")
                            + (fileTier == null ? "" : "/" + fileTier));
                    crcs.put(f.relativePath(), d.crc32());
                }
                tableManifests.add(toTableManifest(plan, entries, tier == null ? null : tier.split()));
            }

            for (SnapshotTableSpec spec : report.nonImportedTables()) {
                tableManifests.add(new SnapshotManifest.TableManifest(
                        spec.id(), spec.specVersion(), spec.digest(), spec.module(), spec.kind().name(),
                        spec.restore().name(), spec.reason(), null, null, spec.targetTable(), null, null,
                        0, List.of(), 0L, List.of(), null, Map.of(), Map.of(), List.of(), spec.contractDigest()));
            }

            ArchiveWriter writer = new ArchiveWriter(source, options.outputDir(), baseName,
                    options.partSizeBytes());
            long packStart = System.currentTimeMillis();
            Progress.phase(progress, String.format("Writing %s into parts of up to %s with %d threads",
                    bytes(scanBytes), bytes(options.partSizeBytes()), threads));
            Map<String, Long> sizes = new HashMap<>();
            allEntries.forEach(fe -> sizes.put(fe.path(), fe.size()));
            AtomicInteger packed = new AtomicInteger();
            AtomicLong packedBytes = new AtomicLong();
            List<SnapshotManifest.PartManifest> parts;
            try (Progress.Ticker ignored = Progress.every(Progress.INTERVAL_MILLIS, "pack",
                    () -> String.format("%,d/%,d files  %s", packed.get(), allEntries.size(),
                            Progress.rate(packedBytes.get(), scanBytes, packStart, System.currentTimeMillis())),
                    progress)) {
                parts = writer.write(allEntries, owners, modules, crcs, threads, path -> {
                    packed.incrementAndGet();
                    packedBytes.addAndGet(sizes.getOrDefault(path, 0L));
                });
            }
            say(progress, "pack", String.format("Wrote %d parts (%s) in %s", parts.size(),
                    bytes(parts.stream().mapToLong(SnapshotManifest.PartManifest::size).sum()),
                    Progress.duration(System.currentTimeMillis() - packStart)));

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
                    report.declaredLossy(), options.tableSchemaFingerprints(), null, null, options.yaciStoreCommit());

            if (verifyParts) {
                verifyWrittenParts(manifest, parts, options, threads, progress);
            }

            Path manifestPath = options.outputDir().resolve(baseName + ".manifest.json");
            ManifestCodec codec = new ManifestCodec();
            codec.writeAtomically(manifest, manifestPath);
            com.bloxbean.cardano.yaci.store.snapshot.archive.Checksums.write(manifest, manifestPath);

            long bytes = parts.stream().mapToLong(SnapshotManifest.PartManifest::size).sum();
            log.info("Exported snapshot {} ({} parts, {} bytes)", snapshotId, parts.size(), bytes);
            return new ExportResult(manifestPath, manifest, report, bytes, System.currentTimeMillis() - started);
        }
    }

    /** Re-reads every written part before the manifest is published. */
    private void verifyWrittenParts(SnapshotManifest manifest, List<SnapshotManifest.PartManifest> parts,
                                    ExportOptions options, int threads, Consumer<String> progress) throws IOException {
        long verifyStart = System.currentTimeMillis();
        long partBytes = parts.stream().mapToLong(SnapshotManifest.PartManifest::size).sum();
        Progress.phase(progress, String.format("Re-reading %d parts (%s)", parts.size(), bytes(partBytes)));
        AtomicLong hashed = new AtomicLong();
        ArchiveVerifier.Result verified;
        try (Progress.Ticker ignored = Progress.every(Progress.INTERVAL_MILLIS, "verify",
                () -> Progress.rate(hashed.get(), partBytes, verifyStart, System.currentTimeMillis()),
                progress)) {
            verified = new ArchiveVerifier(threads).verify(manifest, options.outputDir(), true, hashed::addAndGet);
        }
        say(progress, "verify", String.format("Verified %d parts in %s", parts.size(),
                Progress.duration(System.currentTimeMillis() - verifyStart)));
        if (!verified.ok()) {
            throw new IOException("Written parts failed verification:\n  - "
                    + String.join("\n  - ", verified.problems()));
        }
    }

    private static void say(Consumer<String> progress, String phase, String message) {
        log.info(message);
        if (progress != null) {
            progress.accept("[" + phase + "] " + message);
        }
    }

    private static String bytes(long n) {
        return n >= 1L << 30 ? String.format("%.1f GiB", n / (double) (1L << 30))
                : String.format("%.1f MiB", n / (double) (1L << 20));
    }

    private SnapshotManifest.TableManifest toTableManifest(TablePlan plan, List<SnapshotManifest.FileEntry> entries,
                                                           SnapshotManifest.HistorySplit split) {
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
                plan.sourceColumns(), plan.bounds(), entries, spec.contractDigest(), split);
    }
}
