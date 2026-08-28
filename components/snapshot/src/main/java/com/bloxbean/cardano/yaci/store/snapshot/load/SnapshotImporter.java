package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveExtractor;
import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveVerifier;
import com.bloxbean.cardano.yaci.store.snapshot.convert.ConverterRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.handler.HandlerRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.handler.SnapshotHandler;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Loads a verified snapshot into an empty, already-migrated PostgreSQL schema.
 *
 * <p>Import is resumable by construction: work is a content-addressed queue of {@code (table, batch)}
 * items, and each batch commits its rows and its journal record in one transaction. Deleting a
 * partial import is never an automatic recovery action.
 *
 * <p>Admin-managed indexes are never created, dropped, deferred or rebuilt here. The operator runs
 * {@code apply-indexes} afterwards, exactly as after a normal initial sync.
 */
public class SnapshotImporter {

    private static final Logger log = LoggerFactory.getLogger(SnapshotImporter.class);

    /** PostgreSQL advisory lock key, so two imports cannot run against one schema. */
    private static final long ADVISORY_LOCK_KEY = 0x59414349_534E4150L;

    private final SnapshotSpecRegistry registry;
    private final ConverterRegistry converters = new ConverterRegistry();
    private final HandlerRegistry handlers = new HandlerRegistry();

    public SnapshotImporter(SnapshotSpecRegistry registry) {
        this.registry = registry;
    }

    /** Preflight-only view, used by {@code snapshot import-status} and before any write. */
    public record Preflight(SnapshotManifest manifest,
                            List<String> blockers,
                            List<String> warnings,
                            ImportJournal.Run existingRun) {
        public boolean ok() {
            return blockers.isEmpty();
        }
    }

    public Preflight preflight(ImportOptions options) throws SQLException, IOException {
        SnapshotManifest manifest = new ManifestCodec().read(options.manifestPath());
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        checkSpecCompatibility(manifest, blockers);

        if (!options.network().equalsIgnoreCase(manifest.point().network())
                || options.protocolMagic() != manifest.point().protocolMagic()) {
            blockers.add("Network mismatch: the snapshot is " + manifest.point().network() + "/"
                    + manifest.point().protocolMagic() + " but this instance is configured for "
                    + options.network() + "/" + options.protocolMagic()
                    + ". A network mismatch is never overridable.");
        }

        ImportJournal.Run existing = null;
        try (Connection pg = connect(options)) {
            PgSchema schema = new PgSchema(pg, options.schema());
            String fingerprint = schema.fingerprint();
            if (manifest.schemaFingerprint() != null && !manifest.schemaFingerprint().equals(fingerprint)) {
                blockers.add("Target schema fingerprint does not match the snapshot. Create the schema "
                        + "with the Yaci Store release named in the manifest ("
                        + manifest.yaciStoreVersion() + ") using store.sync-auto-start=false.");
            }
            if (manifest.flywayFingerprint() != null
                    && !manifest.flywayFingerprint().equals(schema.flywayFingerprint())) {
                blockers.add("Applied Flyway migrations do not match the snapshot's release");
            }

            ImportJournal journal = new ImportJournal(pg, options.schema());
            existing = journal.currentRun().orElse(null);
            if (existing != null && !existing.snapshotId().equals(manifest.snapshotId())) {
                blockers.add("The schema holds an incomplete import of a different snapshot ("
                        + existing.snapshotId() + ", status " + existing.status()
                        + "). Finish or explicitly clear it first.");
            }

            List<String> nonEmpty = nonEmptyChainTables(pg, options.schema());
            if (!nonEmpty.isEmpty() && existing == null) {
                blockers.add("Target schema already contains chain data in " + nonEmpty
                        + ". Import requires an empty schema, or a matching incomplete import journal. "
                        + "Clearing a non-empty schema is a separate, explicit operation.");
            }

            Set<String> indexes = schema.nonConstraintIndexes();
            if (!indexes.isEmpty()) {
                warnings.add(indexes.size() + " non-constraint index(es) already exist. Loading will be "
                        + "slower; they are left in place. Indexes are normally applied after import with "
                        + "'apply-indexes'.");
            }

            for (String table : schema.baseTables()) {
                if (table.equals("flyway_schema_history") || table.startsWith("_yaci_snapshot_import")) {
                    continue;
                }
                if (registry.byTarget(table).isEmpty()) {
                    blockers.add("Target table '" + table + "' has no snapshot classification. "
                            + "Every table created by a migration needs an explicit specification.");
                }
            }

            blockers.addAll(planEveryTable(manifest, schema));
        }

        ArchiveVerifier.Result verified =
                new ArchiveVerifier().verify(manifest, options.archiveDir(), false);
        if (!verified.ok()) {
            verified.problems().forEach(p -> blockers.add("Archive: " + p));
        }
        if (!manifest.declaredLossy().isEmpty()) {
            warnings.add("This snapshot has " + manifest.declaredLossy().size()
                    + " declared limitation(s); the restored database is not an exact copy.");
        }

        long freeGb = freeDiskGb(options.workDir());
        if (freeGb < options.minFreeDiskGb()) {
            blockers.add("Only " + freeGb + " GiB free at " + options.workDir()
                    + "; the configured minimum is " + options.minFreeDiskGb() + " GiB");
        }

        return new Preflight(manifest, blockers, warnings, existing);
    }

    /**
     * Every spec the manifest names must exist locally at the same version and digest. Mapping rules
     * are executable, so they always come from the local installation, never from the archive.
     */
    private void checkSpecCompatibility(SnapshotManifest manifest, List<String> blockers) {
        for (SnapshotManifest.TableManifest t : manifest.tables()) {
            SnapshotTableSpec local = registry.byId(t.specId()).orElse(null);
            if (local == null) {
                blockers.add("Snapshot references specification '" + t.specId()
                        + "' which is not installed locally");
                continue;
            }
            if (local.specVersion() != t.specVersion()) {
                blockers.add("Specification '" + t.specId() + "' is version " + local.specVersion()
                        + " locally but " + t.specVersion() + " in the snapshot");
            } else if (!local.digest().equals(t.specDigest())) {
                blockers.add("Specification '" + t.specId() + "' has digest " + local.digest().substring(0, 12)
                        + "... locally but " + t.specDigest().substring(0, 12) + "... in the snapshot");
            }
        }
    }

    /**
     * Resolve every table's column mapping before a single row is written.
     *
     * <p>The manifest records the source column types, so the whole mapping can be checked against
     * the live target schema without reading a Parquet file. Without this, a mapping problem in a
     * table late in the load order only surfaces after the earlier tables have been imported.
     *
     * <p>SQL-mode tables are skipped: their output columns come from running the transform, which is
     * checked per batch during the load.
     */
    private List<String> planEveryTable(SnapshotManifest manifest, PgSchema schema) {
        ColumnPlanner planner = new ColumnPlanner(converters);
        List<String> problems = new ArrayList<>();
        for (SnapshotManifest.TableManifest t : manifest.tables()) {
            SnapshotTableSpec spec = registry.byId(t.specId()).orElse(null);
            if (spec == null || spec.restore() != RestoreMode.IMPORT
                    || spec.importSpec().mode() == com.bloxbean.cardano.yaci.store.snapshot.spec.ImportMode.SQL
                    || t.sourceColumns() == null || t.sourceColumns().isEmpty()) {
                continue;
            }
            try {
                if (!schema.tableExists(spec.targetTable())) {
                    continue;
                }
                planner.plan(spec, t.sourceColumns(), schema.table(spec.targetTable()));
            } catch (ColumnPlanner.MappingException e) {
                problems.add(e.getMessage());
            } catch (SQLException e) {
                problems.add("Unable to read target table " + spec.targetTable() + ": " + e.getMessage());
            }
        }
        return problems;
    }

    public ImportReport importSnapshot(ImportOptions options, Consumer<String> progress)
            throws SQLException, IOException {
        Preflight pre = preflight(options);
        if (!pre.ok()) {
            throw new IllegalStateException("Snapshot import is blocked:\n  - "
                    + String.join("\n  - ", pre.blockers()));
        }
        SnapshotManifest manifest = pre.manifest();
        List<String> warnings = new ArrayList<>(pre.warnings());

        long t0 = System.currentTimeMillis();
        ArchiveVerifier.Result verified = new ArchiveVerifier().verify(manifest, options.archiveDir(), true);
        if (!verified.ok()) {
            throw new IllegalStateException("Archive verification failed:\n  - "
                    + String.join("\n  - ", verified.problems()));
        }
        long verifyMillis = System.currentTimeMillis() - t0;

        Path extractRoot = options.workDir().resolve("extracted");
        long t1 = System.currentTimeMillis();
        if (!options.skipExtraction()) {
            extract(manifest, options, extractRoot, progress);
        }
        long extractMillis = System.currentTimeMillis() - t1;

        try (Connection pg = connect(options)) {
            acquireAdvisoryLock(pg);
            ImportJournal journal = new ImportJournal(pg, options.schema());
            journal.createIfAbsent();
            PgSchema schema = new PgSchema(pg, options.schema());
            journal.startRun(new ImportJournal.Run(manifest.snapshotId(),
                    new ManifestCodec().digest(manifest), manifest.point().network(),
                    manifest.point().protocolMagic(), manifest.point().epoch(), manifest.point().slot(),
                    manifest.point().blockNumber(), manifest.point().blockHash(),
                    schema.fingerprint(), ImportJournal.STATUS_LOADING, null));

            int partitionsCreated = prepareTargetPartitions(pg, options, manifest);

            long t2 = System.currentTimeMillis();
            LoadResult loaded = loadTables(options, manifest, extractRoot, journal, schema, progress);
            long loadMillis = System.currentTimeMillis() - t2;

            long t3 = System.currentTimeMillis();
            List<String> handlerResults = runHandlers(pg, options, manifest);
            Map<String, Long> sequences = new SequenceReset(pg, options.schema()).resetAll();
            long controlMillis = System.currentTimeMillis() - t3;

            journal.setStatus(manifest.snapshotId(), ImportJournal.STATUS_VALIDATING,
                    "loaded " + loaded.rows + " rows; awaiting validation");

            if (!options.keepExtracted() && !options.skipExtraction()) {
                deleteRecursively(extractRoot);
            }

            return new ImportReport(manifest.snapshotId(), ImportJournal.STATUS_VALIDATING,
                    loaded.planned, loaded.skipped, loaded.executed, loaded.rows,
                    journal.rowsPerTable(manifest.snapshotId()), handlerResults, sequences,
                    partitionsCreated, manifest.declaredLossy(), warnings,
                    verifyMillis, extractMillis, loadMillis, controlMillis);
        }
    }

    /** Prepare and plan the first batch of every SQL-mode table, so a bad transform fails fast. */
    private void planSqlTransforms(ImportOptions options, SnapshotManifest manifest,
                                   List<SnapshotTableSpec> ordered,
                                   Map<String, List<SnapshotManifest.FileEntry>> filesBySpec,
                                   PgSchema schema, BatchPlanner planner, TableLoader loader)
            throws SQLException {
        List<SnapshotTableSpec> sqlTables = ordered.stream()
                .filter(s -> s.importSpec().mode() == com.bloxbean.cardano.yaci.store.snapshot.spec.ImportMode.SQL)
                .filter(s -> !filesBySpec.getOrDefault(s.id(), List.of()).isEmpty())
                .toList();
        if (sqlTables.isEmpty()) {
            return;
        }
        Path spill = options.workDir().resolve("spill-plan");
        try {
            Files.createDirectories(spill);
        } catch (IOException e) {
            throw new SQLException("Unable to create planning spill directory " + spill, e);
        }
        List<String> problems = new ArrayList<>();
        try (DuckPgSession session = DuckPgSession.open(options, spill, 1)) {
            for (SnapshotTableSpec spec : sqlTables) {
                List<ImportBatch> batches =
                        planner.plan(manifest.snapshotId(), spec, filesBySpec.get(spec.id()));
                if (batches.isEmpty()) {
                    continue;
                }
                Map<String, List<SnapshotManifest.FileEntry>> dependencyFiles = new LinkedHashMap<>();
                for (String dep : spec.importSpec().dependencies()) {
                    dependencyFiles.put(dep, filesBySpec.getOrDefault(dep, List.of()));
                }
                String select = loader.sourceSelect(spec, batches.get(0), manifest.point().slot(),
                        manifest.point().epoch(), dependencyFiles);
                try {
                    loader.planFor(session, spec, select, schema.table(spec.targetTable()),
                            manifest.table(spec.id()).sourceColumns());
                } catch (ColumnPlanner.MappingException e) {
                    problems.add(e.getMessage());
                } catch (SQLException e) {
                    problems.add("transform for '" + spec.id() + "' could not be prepared: "
                            + DuckPgSession.redact(e.getMessage(), options.password()));
                }
            }
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("SQL transforms do not match the schemas:\n  - "
                    + String.join("\n  - ", problems));
        }
    }

    private record LoadResult(long planned, long skipped, long executed, long rows) {}

    private LoadResult loadTables(ImportOptions options, SnapshotManifest manifest, Path extractRoot,
                                  ImportJournal journal, PgSchema schema, Consumer<String> progress)
            throws SQLException {
        Set<String> completed = journal.completedBatchIds(manifest.snapshotId());
        BatchPlanner planner = new BatchPlanner();
        TableLoader loader = new TableLoader(converters, extractRoot);

        List<SnapshotTableSpec> ordered = registry.importedTables();
        Map<String, List<SnapshotManifest.FileEntry>> filesBySpec = new HashMap<>();
        Map<String, Map<String, String>> declaredColumns = new HashMap<>();
        manifest.tables().forEach(t -> {
            filesBySpec.put(t.specId(), t.files());
            declaredColumns.put(t.specId(), t.sourceColumns());
        });

        AtomicLong planned = new AtomicLong();
        AtomicLong skipped = new AtomicLong();
        AtomicLong executed = new AtomicLong();
        AtomicLong rows = new AtomicLong();

        // Resolve every table's mapping against the extracted files before writing a single row.
        // Preflight already planned the DIRECT and MAPPED tables from the manifest's recorded column
        // types; an SQL transform's output types only exist once the transform can be described, so
        // that has to wait until extraction — but it must still happen before the load, not several
        // tables into it.
        planSqlTransforms(options, manifest, ordered, filesBySpec, schema, planner, loader);

        for (SnapshotTableSpec spec : ordered) {
            List<SnapshotManifest.FileEntry> files = filesBySpec.getOrDefault(spec.id(), List.of());
            List<ImportBatch> batches = planner.plan(manifest.snapshotId(), spec, files);
            planned.addAndGet(batches.size());
            if (batches.isEmpty()) {
                continue;
            }

            TargetTable target = schema.table(spec.targetTable());
            Map<String, List<SnapshotManifest.FileEntry>> dependencyFiles = new LinkedHashMap<>();
            for (String dep : spec.importSpec().dependencies()) {
                SnapshotTableSpec depSpec = registry.byId(dep).orElseThrow(
                        () -> new IllegalStateException("Unknown dependency '" + dep + "'"));
                dependencyFiles.put(depSpec.id(), filesBySpec.getOrDefault(depSpec.id(), List.of()));
            }

            ConcurrentLinkedQueue<ImportBatch> queue = new ConcurrentLinkedQueue<>(batches);
            int workers = Math.max(1, Math.min(options.workers(), batches.size()));
            List<Thread> threads = new ArrayList<>();
            List<Exception> failures = new ArrayList<>();

            for (int w = 0; w < workers; w++) {
                Path spill = options.workDir().resolve("spill-" + w);
                Thread thread = Thread.ofPlatform().name("snapshot-import-" + w).unstarted(() -> {
                    try {
                        Files.createDirectories(spill);
                        try (DuckPgSession session = DuckPgSession.open(options, spill, 2)) {
                            ImportBatch batch;
                            while ((batch = queue.poll()) != null) {
                                if (completed.contains(batch.batchId())) {
                                    skipped.incrementAndGet();
                                    continue;
                                }
                                String select = loader.sourceSelect(spec, batch,
                                        manifest.point().slot(), manifest.point().epoch(), dependencyFiles);
                                ColumnPlan plan = loader.planFor(session, spec, select, target,
                                        declaredColumns.get(spec.id()));
                                long n = loader.loadBatch(session, batch, plan, select,
                                        manifest.snapshotId(), options.schema());
                                rows.addAndGet(n);
                                executed.incrementAndGet();
                                if (progress != null) {
                                    progress.accept(spec.id() + " +" + n + " rows");
                                }
                            }
                        }
                    } catch (Exception e) {
                        synchronized (failures) {
                            failures.add(e);
                        }
                        queue.clear();
                    }
                });
                threads.add(thread);
                thread.start();
            }
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Import interrupted", e);
                }
            }
            if (!failures.isEmpty()) {
                Exception first = failures.get(0);
                throw new IllegalStateException("Failed loading '" + spec.id() + "': "
                        + DuckPgSession.redact(first.getMessage(), options.password()), first);
            }
            log.info("Loaded {} ({} batches, {} rows so far)", spec.id(), batches.size(), rows.get());
        }
        return new LoadResult(planned.get(), skipped.get(), executed.get(), rows.get());
    }

    private int prepareTargetPartitions(Connection pg, ImportOptions options, SnapshotManifest manifest)
            throws SQLException {
        PartitionPreparer preparer = new PartitionPreparer(pg, options.schema());
        int created = 0;
        for (SnapshotTableSpec spec : registry.importedTables()) {
            if (spec.importSpec().targetPartitioning() == null) {
                continue;
            }
            SnapshotManifest.TableManifest t = manifest.table(spec.id());
            if (t == null || t.rowCount() == 0) {
                continue;
            }
            String column = spec.importSpec().targetPartitioning().column();
            int from = boundOf(t, column + ".min", 0);
            int to = boundOf(t, column + ".max", manifest.point().epoch());
            created += preparer.prepare(spec, Math.max(0, from), Math.max(to, manifest.point().epoch()));
        }
        return created;
    }

    private static int boundOf(SnapshotManifest.TableManifest t, String key, int fallback) {
        String v = t.bounds() == null ? null : t.bounds().get(key);
        try {
            return v == null ? fallback : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private List<String> runHandlers(Connection pg, ImportOptions options, SnapshotManifest manifest)
            throws SQLException {
        List<String> results = new ArrayList<>();
        boolean previousAutoCommit = pg.getAutoCommit();
        pg.setAutoCommit(false);
        try {
            for (SnapshotTableSpec spec : registry.handlerTables()) {
                SnapshotHandler handler = handlers.get(spec.importSpec().handler());
                SnapshotHandler.Context ctx = new SnapshotHandler.Context(pg, options.schema(),
                        manifest.point(), options.eventPublisherId(), options.cursorBlocksToKeep(), spec);
                results.add(spec.targetTable() + ": " + handler.apply(ctx));
            }
            pg.commit();
        } catch (SQLException e) {
            pg.rollback();
            throw e;
        } finally {
            pg.setAutoCommit(previousAutoCommit);
        }
        return results;
    }

    private void extract(SnapshotManifest manifest, ImportOptions options, Path extractRoot,
                         Consumer<String> progress) throws IOException {
        Map<String, Long> expected = new TreeMap<>();
        manifest.tables().forEach(t -> t.files().forEach(f -> expected.put(f.path(), f.size())));

        long maxEntries = expected.size() + 16L;
        long maxBytes = expected.values().stream().mapToLong(Long::longValue).sum() + (1L << 20);
        ArchiveExtractor extractor = new ArchiveExtractor(extractRoot, maxEntries, maxBytes);

        for (SnapshotManifest.PartManifest part : manifest.parts()) {
            extractor.extract(options.archiveDir().resolve(part.fileName()), expected,
                    progress == null ? null : p -> progress.accept("extract " + p));
        }
        for (SnapshotManifest.TableManifest t : manifest.tables()) {
            for (SnapshotManifest.FileEntry f : t.files()) {
                if (!extractor.verifyFile(f.path(), f.sha256())) {
                    throw new IOException("Extracted file failed its digest check: " + f.path());
                }
            }
        }
    }

    private Connection connect(ImportOptions options) throws SQLException {
        try {
            return DriverManager.getConnection(options.jdbcUrl(), options.user(), options.password());
        } catch (SQLException e) {
            throw new SQLException(DuckPgSession.redact(e.getMessage(), options.password()), e.getSQLState());
        }
    }

    private void acquireAdvisoryLock(Connection pg) throws SQLException {
        try (Statement st = pg.createStatement();
             ResultSet rs = st.executeQuery("SELECT pg_try_advisory_lock(" + ADVISORY_LOCK_KEY + ")")) {
            if (!rs.next() || !rs.getBoolean(1)) {
                throw new SQLException("Another snapshot import holds the advisory lock on this database");
            }
        }
    }

    /** Tables that must be empty before a fresh import, sampled cheaply. */
    private List<String> nonEmptyChainTables(Connection pg, String schema) throws SQLException {
        List<String> out = new ArrayList<>();
        PgSchema s = new PgSchema(pg, schema);
        for (SnapshotTableSpec spec : registry.importedTables()) {
            if (!s.tableExists(spec.targetTable())) {
                continue;
            }
            try (Statement st = pg.createStatement();
                 ResultSet rs = st.executeQuery("SELECT 1 FROM "
                         + com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers.quote(schema) + "."
                         + com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers.quote(spec.targetTable())
                         + " LIMIT 1")) {
                if (rs.next()) {
                    out.add(spec.targetTable());
                }
            }
            if (out.size() >= 5) {
                break;
            }
        }
        return out;
    }

    static long freeDiskGb(Path path) throws IOException {
        Path p = path;
        while (p != null && !Files.exists(p)) {
            p = p.getParent();
        }
        if (p == null) {
            return Long.MAX_VALUE;
        }
        return Files.getFileStore(p).getUsableSpace() / (1024L * 1024 * 1024);
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // Leaving a temporary file behind is preferable to failing a completed import.
                }
            });
        }
    }

    /** Digest of the local specification set, for reporting. */
    public String localSpecDigest() {
        StringBuilder sb = new StringBuilder();
        registry.digests().forEach((id, d) -> sb.append(id).append('=').append(d).append('\n'));
        return Digests.sha256Hex(sb.toString());
    }

    public SnapshotSpecRegistry registry() {
        return registry;
    }

    /** Tables the snapshot deliberately leaves empty, for the import report. */
    public List<String> nonImportedSummary() {
        List<String> out = new ArrayList<>();
        for (SnapshotTableSpec spec : registry.all()) {
            if (spec.restore() != RestoreMode.IMPORT && spec.restore() != RestoreMode.HANDLER) {
                out.add(spec.targetTable() + " [" + spec.restore() + "] " + spec.reason());
            }
        }
        return out;
    }
}
