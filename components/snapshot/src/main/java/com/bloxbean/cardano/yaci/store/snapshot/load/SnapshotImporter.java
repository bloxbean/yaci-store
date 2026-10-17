package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestValidator;
import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveExtractor;
import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveVerifier;
import com.bloxbean.cardano.yaci.store.snapshot.archive.Parallel;
import com.bloxbean.cardano.yaci.store.snapshot.archive.PartLayout;
import com.bloxbean.cardano.yaci.store.snapshot.convert.ConverterRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.handler.HandlerRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.handler.SnapshotHandler;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestCodec;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.Retention;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotSignature;
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Loads a verified snapshot into an empty, already-migrated PostgreSQL schema.
 *
 * <p>Import is resumable by construction: work is a content-addressed queue of {@code (table, batch)}
 * items, and each batch commits its rows and its journal record in one transaction. Deleting a
 * partial import is never an automatic recovery action.
 *
 * <p>The keys and indexes the migrations created on the loaded tables are dropped for the load and
 * rebuilt after it ({@link DeferredIndexes}). The optional admin-managed indexes are not created
 * here; the operator runs {@code apply-indexes} afterwards, exactly as after a normal initial sync.
 */
public class SnapshotImporter {

    private static final Logger log = LoggerFactory.getLogger(SnapshotImporter.class);

    /** Phase banners {@link #importSnapshot} sends, so a caller can number them. */
    public static final int PHASES = 6;

    /** Reads of one remote part before the import gives up; every attempt starts the part again. */
    static final int PART_ATTEMPTS = 3;

    /** PostgreSQL advisory lock key, so two imports cannot run against one schema. */
    private static final long ADVISORY_LOCK_KEY = 0x59414349_534E4150L;

    private final SnapshotSpecRegistry registry;
    private final ConverterRegistry converters = new ConverterRegistry();
    private final HandlerRegistry handlers = new HandlerRegistry();

    public SnapshotImporter(SnapshotSpecRegistry registry) {
        this.registry = registry;
    }

    /** Preflight-only view, used by {@code snapshot import-status} and before any write. */
    /**
     * @param manifest the import's view: the snapshot as published, reduced by the import's own
     *                 retention ({@link Retention#apply})
     * @param archive  the snapshot as published, which the parts are checked against
     * @param signature whether the snapshot is signed, and by a trusted key
     */
    public record Preflight(SnapshotManifest manifest,
                            List<String> blockers,
                            List<String> warnings,
                            ImportJournal.Run existingRun,
                            SnapshotManifest archive,
                            SnapshotSignature.Verification signature) {
        public Preflight(SnapshotManifest manifest, List<String> blockers, List<String> warnings,
                         ImportJournal.Run existingRun, SnapshotManifest archive) {
            this(manifest, blockers, warnings, existingRun, archive, null);
        }

        public boolean ok() {
            return blockers.isEmpty();
        }
    }

    public Preflight preflight(ImportOptions options) throws SQLException, IOException {
        SnapshotManifest archive = new ManifestCodec().read(options.manifestPath());
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        SnapshotManifest manifest;
        Map<String, String> retentionNotes = new TreeMap<>();
        Map<String, String> retention;
        try {
            retention = Retention.forTarget(archive, options.targetSettings(), options.retention(), retentionNotes);
            manifest = Retention.apply(archive, retention);
        } catch (IllegalArgumentException e) {
            blockers.add(e.getMessage());
            return new Preflight(archive, blockers, warnings, null, archive);
        }

        retentionNotes.forEach((table, note) -> {
            if (registry.byTarget(table).filter(spec -> registry.isSelected(spec.id())).isPresent()) {
                warnings.add(note);
            }
        });
        ManifestValidator validator = new ManifestValidator(registry);
        blockers.addAll(validator.validate(manifest));
        warnings.addAll(validator.importChanges(manifest));
        SnapshotSignature.Verification signature;
        try {
            signature = SnapshotSignature.verify(options.manifestPath(), options.trustedKeys());
        } catch (IOException | RuntimeException e) {
            signature = new SnapshotSignature.Verification(SnapshotSignature.Status.INVALID, null, List.of(e.getMessage()));
        }
        switch (signature.status()) {
            case VERIFIED -> { }
            case INVALID -> blockers.add("Snapshot " + signature.describe()
                    + ". The snapshot is not what its publisher signed; do not import it.");
            case UNSIGNED, UNVERIFIED -> {
                if (options.allowUnsigned()) {
                    warnings.add("Importing a snapshot that is " + signature.describe() + ".");
                } else {
                    blockers.add("The snapshot is " + signature.describe() + ". Configure the publisher's key in"
                            + " store.snapshot.trusted-keys (or --trusted-key), or pass --allow-unsigned to import it"
                            + " without a verified signature.");
                }
            }
        }
        if (!blockers.isEmpty()) {
            return new Preflight(manifest, blockers, warnings, null, archive, signature);
        }
        // Resolve both database connection paths before any extraction or target writes.
        DuckPgSession.pgConnectionString(options);

        if (!options.network().equalsIgnoreCase(manifest.point().network())
                || options.protocolMagic() != manifest.point().protocolMagic()) {
            blockers.add("Network mismatch: the snapshot is " + manifest.point().network() + "/"
                    + manifest.point().protocolMagic() + " but this instance is configured for "
                    + options.network() + "/" + options.protocolMagic()
                    + ". A network mismatch is never overridable.");
        }
        blockers.addAll(retentionBlockers(manifest, options));
        blockers.addAll(matchingSettingBlockers(manifest, options));

        ImportJournal.Run existing = null;
        try (Connection pg = connect(options)) {
            PgSchema schema = new PgSchema(pg, options.schema());
            blockers.addAll(SchemaCompatibility.check(schema, manifest, registry));
            if (registry.isScoped()) {
                warnings.add("Selected tables: " + registry.all().stream().map(SnapshotTableSpec::targetTable).toList());
                warnings.add("Skipped tables: " + registry.knownTables().stream()
                        .filter(s -> !registry.isSelected(s.id())).map(SnapshotTableSpec::targetTable).toList());
            }

            ImportJournal journal = new ImportJournal(pg, options.schema());
            existing = journal.currentRun().orElse(null);
            if (existing != null && (registry.isScoped() || existing.scopeDigest() != null)
                    && !registry.scopeDigest().equals(existing.scopeDigest())) {
                blockers.add("Import scope does not match the journal. Resume with the original enabled stores and specifications.");
            }
            if (existing != null && !java.util.Objects.equals(existing.retention(), Retention.format(retention))) {
                blockers.add("Import retention does not match the journal (" + existing.retention()
                        + "). Resume with the original --epoch-stake-epochs and pruning settings.");
            }
            if (existing != null && !existing.manifestDigest().equals(new ManifestCodec().digest(manifest))) {
                blockers.add("Import journal manifest digest does not match this manifest. "
                        + "Resume requires the exact original manifest.");
            }
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

            for (String table : schema.baseTables()) {
                if (table.equals("flyway_schema_history") || table.equals("_yaci_store_schema_profile")
                        || table.startsWith("_yaci_snapshot_import")) {
                    continue;
                }
                if (registry.byTarget(table).isEmpty()) {
                    blockers.add("Target table '" + table + "' has no snapshot classification. "
                            + "Every table created by a migration needs an explicit specification.");
                }
            }

            blockers.addAll(planEveryTable(manifest, schema, options));

            if (options.tunePostgres()) {
                String denied = PostgresTuning.cannotApply(pg);
                if (denied != null) {
                    blockers.add(denied);
                }
            } else {
                warnings.addAll(PostgresTuning.advice(pg));
            }
            warnings.addAll(PostgresTuning.parallelismAdvice(pg, options.workers()));
        }

        ArchiveVerifier.Selection selection = selection(archive, manifest);
        if (selection.parts().size() < archive.parts().size()) {
            warnings.add(String.format("Reading %d of %d parts (%s of %s); the others carry only skipped tables, epochs or history",
                    selection.parts().size(), archive.parts().size(), Progress.bytes(partBytes(selection.parts())),
                    Progress.bytes(partBytes(archive.parts()))));
        }
        if (options.partSource() == null) {
            ArchiveVerifier.Result verified =
                    new ArchiveVerifier().inspect(archive, options.archiveDir(), selection);
            if (!verified.ok()) {
                verified.problems().forEach(p -> blockers.add("Archive: " + p));
            }
        } else {
            blockers.addAll(remoteParts(options.partSource(), selection));
        }
        if (!registry.selectedLimitations(manifest.declaredLossy()).isEmpty()) {
            warnings.add("This snapshot has " + registry.selectedLimitations(manifest.declaredLossy()).size()
                    + " declared limitation(s); the restored database is not an exact copy.");
        }

        long freeGb = freeDiskGb(options.workDir());
        if (freeGb < options.minFreeDiskGb()) {
            blockers.add("Only " + freeGb + " GiB free at " + options.workDir()
                    + "; the configured minimum is " + options.minFreeDiskGb() + " GiB");
        }

        return new Preflight(manifest, blockers, warnings, existing, archive, signature);
    }

    /**
     * A snapshot that keeps only part of a table's history can only go to a target that prunes that
     * table the same way; otherwise the target would present partial history as complete.
     */
    private List<String> retentionBlockers(SnapshotManifest manifest, ImportOptions options) {
        List<String> problems = new ArrayList<>();
        new TreeMap<>(manifest.retentionOrEmpty()).forEach((key, value) -> {
            String table = Retention.table(key);
            boolean selected = registry.byTarget(table).filter(s -> registry.isSelected(s.id())).isPresent();
            String setting = Retention.requiredSetting(key);
            if (!selected || setting == null) {
                return;
            }
            if (!"true".equalsIgnoreCase(options.targetSettings().getOrDefault(setting, "false"))) {
                problems.add(String.format("This import keeps only part of %s (%s=%s). The target must run with "
                        + "%s=true, or it would present partial history as complete.", table, key, value, setting));
            }
        });
        return problems;
    }

    /**
     * An optional table holds what the source's settings made it hold, e.g. whole transactions or only
     * their bodies in {@code transaction_cbor}; a target that expects the other form is refused.
     */
    private List<String> matchingSettingBlockers(SnapshotManifest manifest, ImportOptions options) {
        List<String> problems = new ArrayList<>();
        Map<String, String> source = manifest.pruningSettings() == null ? Map.of() : manifest.pruningSettings();
        for (SnapshotTableSpec spec : registry.importedTables()) {
            SnapshotManifest.TableManifest t = manifest.table(spec.id());
            if (t == null || t.rowCount() == 0) {
                continue;
            }
            for (String setting : spec.mustMatch()) {
                String exported = source.getOrDefault(setting, "false");
                String wanted = options.targetSettings().getOrDefault(setting, "false");
                if (!exported.equalsIgnoreCase(wanted)) {
                    problems.add(String.format("%s was exported with %s=%s, but the target runs with %s=%s. Use the"
                            + " same setting, or turn %s off on the target.", spec.targetTable(), setting, exported,
                            setting, wanted, spec.targetTable()));
                }
            }
        }
        return problems;
    }

    /**
     * The files this import loads (the selected tables', and the files their transforms read), and
     * the parts that carry them.
     */
    public ArchiveVerifier.Selection selection(SnapshotManifest archive, SnapshotManifest manifest) {
        Set<String> specs = new java.util.TreeSet<>();
        for (SnapshotTableSpec spec : registry.importedTables()) {
            specs.add(spec.id());
            specs.addAll(spec.importSpec().dependencies());
        }
        Set<String> files = new java.util.TreeSet<>();
        manifest.tables().stream().filter(t -> specs.contains(t.specId()) && t.files() != null)
                .forEach(t -> t.files().forEach(f -> files.add(f.path())));
        return new ArchiveVerifier.Selection(PartLayout.of(archive).partsFor(archive, files), files);
    }

    /** Every part the import reads is in the remote store, with the size the manifest declares. */
    private static List<String> remoteParts(com.bloxbean.cardano.yaci.store.snapshot.archive.PartSource source,
                                            ArchiveVerifier.Selection selection) {
        List<String> problems = new ArrayList<>();
        for (SnapshotManifest.PartManifest part : selection.parts()) {
            try {
                var size = source.size(part);
                if (size.isEmpty()) {
                    problems.add("Archive: missing archive part " + part.fileName() + " at " + source.describe());
                } else if (size.getAsLong() != part.size()) {
                    problems.add("Archive: " + part.fileName() + ": size " + size.getAsLong() + " but manifest declares "
                            + part.size());
                }
            } catch (IOException e) {
                problems.add("Archive: " + part.fileName() + ": " + e.getMessage());
            }
        }
        return problems;
    }

    private static long partBytes(List<SnapshotManifest.PartManifest> parts) {
        return parts.stream().mapToLong(SnapshotManifest.PartManifest::size).sum();
    }

    /**
     * Resolve every table's column mapping before a single row is written.
     *
     * <p>The manifest records the source column types, so the whole mapping can be checked against
     * the live target schema without reading a Parquet file. Without this, a mapping problem in a
     * table late in the load order only surfaces after the earlier tables have been imported.
     *
     * <p>An SQL transform's output types only exist once it is bound to inputs, and nothing is
     * extracted yet, so it is described over empty relations of the recorded source types: what
     * {@code --dry-run} checks is what the load will see. The load repeats the check over the
     * extracted files.
     */
    private List<String> planEveryTable(SnapshotManifest manifest, PgSchema schema, ImportOptions options)
            throws SQLException {
        ColumnPlanner planner = new ColumnPlanner(converters);
        List<String> problems = new ArrayList<>();
        try (Connection duck = DuckDb.open(options.memoryLimit(), null, 1)) {
            for (SnapshotManifest.TableManifest t : manifest.tables()) {
                SnapshotTableSpec spec = registry.byId(t.specId()).orElse(null);
                if (spec == null || !registry.isSelected(spec.id()) || spec.restore() != RestoreMode.IMPORT
                        || !recorded(t)) {
                    continue;
                }
                Map<String, String> dependencies = new LinkedHashMap<>();
                for (String dep : spec.importSpec().dependencies()) {
                    SnapshotManifest.TableManifest d = manifest.table(dep);
                    if (d != null && recorded(d)) {
                        dependencies.put(dep, ImportPlanCheck.emptyRelation(d.sourceColumns()));
                    }
                }
                if (dependencies.size() < spec.importSpec().dependencies().size()) {
                    continue; // an older manifest; the load checks the transform over the files
                }
                try {
                    if (!schema.tableExists(spec.targetTable())) {
                        continue;
                    }
                    problems.addAll(ImportPlanCheck.check(duck, planner, spec, t.sourceColumns(),
                            ImportPlanCheck.emptyRelation(t.sourceColumns()), dependencies,
                            schema.table(spec.targetTable()), manifest.point().slot(), manifest.point().epoch()));
                } catch (SQLException e) {
                    problems.add("Unable to read target table " + spec.targetTable() + ": " + e.getMessage());
                }
            }
        }
        return problems;
    }

    private static boolean recorded(SnapshotManifest.TableManifest t) {
        return t.sourceColumns() != null && !t.sourceColumns().isEmpty();
    }

    public ImportReport importSnapshot(ImportOptions options, Consumer<String> progress)
            throws SQLException, IOException {
        Preflight pre = preflight(options);
        if (!pre.ok()) {
            throw new IllegalStateException("Snapshot import is blocked:\n  - "
                    + String.join("\n  - ", pre.blockers()));
        }
        SnapshotManifest manifest = pre.manifest();
        SnapshotManifest archive = pre.archive();
        Map<String, String> retention = Retention.forTarget(archive, options.targetSettings(), options.retention(),
                new TreeMap<>());
        ArchiveVerifier.Selection selection = selection(archive, manifest);
        List<String> warnings = new ArrayList<>(pre.warnings());

        try (Connection pg = connect(options)) {
            acquireAdvisoryLock(pg);
            // Recheck database state after acquiring the lock: another importer may have finished
            // since preflight. Resume is bound to the entire manifest.
            ImportJournal.Run current = new ImportJournal(pg, options.schema()).currentRun().orElse(null);
            if (current != null && (registry.isScoped() || current.scopeDigest() != null)
                    && !registry.scopeDigest().equals(current.scopeDigest())) {
                throw new IllegalStateException("Import scope does not match the journal");
            }
            if (current != null && !java.util.Objects.equals(current.retention(), Retention.format(retention))) {
                throw new IllegalStateException("Import retention does not match the journal");
            }
            if (current != null && !current.manifestDigest().equals(new ManifestCodec().digest(manifest))) {
                throw new IllegalStateException("Import journal manifest digest does not match this manifest");
            }
            if (current == null && !nonEmptyChainTables(pg, options.schema()).isEmpty()) {
                throw new IllegalStateException("Target schema already contains chain data");
            }
            long t0 = System.currentTimeMillis();
            // With extraction, each part and each entry is hashed while it is extracted, in one read;
            // only the structure is checked up front. Without it, the parts are hashed here instead.
            int partThreads = Math.max(1, Math.min(options.workers(), ArchiveVerifier.DEFAULT_THREADS));
            ArchiveVerifier verifier = new ArchiveVerifier(partThreads);
            Progress.phase(progress, String.format("%s %d parts (%s)",
                    options.skipExtraction() ? "Verifying"
                            : options.partSource() != null && options.partSource().remote() ? "Downloading, verifying and extracting"
                            : "Verifying and extracting", selection.parts().size(),
                    Progress.bytes(partBytes(selection.parts()))));
            if (options.skipExtraction() && options.partSource() != null) {
                throw new IllegalArgumentException("Skipping extraction needs the parts in a local directory");
            }
            // A streamed part cannot be checked up front: extraction checks every entry, and afterwards
            // that every selected file arrived.
            ArchiveVerifier.Result verified = options.partSource() != null ? new ArchiveVerifier.Result(true, List.of(), 0)
                    : options.skipExtraction()
                    ? verifier.verify(archive, options.archiveDir(), true, null, selection)
                    : verifier.structure(archive, options.archiveDir(), selection);
            if (!verified.ok()) {
                throw new IllegalStateException("Archive verification failed:\n  - "
                        + String.join("\n  - ", verified.problems()));
            }
            long verifyMillis = System.currentTimeMillis() - t0;

            Path extractRoot = options.workDir().resolve("extracted");
            long t1 = System.currentTimeMillis();
            if (!options.skipExtraction()) {
                extract(archive, selection, options, extractRoot, progress);
            }
            long extractMillis = System.currentTimeMillis() - t1;

            ImportJournal journal = new ImportJournal(pg, options.schema());
            journal.createIfAbsent();
            PgSchema schema = new PgSchema(pg, options.schema());
            journal.startRun(new ImportJournal.Run(manifest.snapshotId(),
                    new ManifestCodec().digest(manifest), manifest.point().network(),
                    manifest.point().protocolMagic(), manifest.point().epoch(), manifest.point().slot(),
                    manifest.point().blockNumber(), manifest.point().blockHash(),
                    schema.fingerprint(), ImportJournal.STATUS_LOADING, null, registry.scopeDigest(),
                    Retention.format(retention)));
            journal.recordTransforms(manifest.snapshotId(), registry.importedTables());

            Progress.phase(progress, "Preparing " + loadedTables(manifest).size()
                    + " tables: deferring keys and indexes, creating partitions");
            // Before the partitions are created, so dropping a parent's index touches fewer of them.
            DeferredIndexes deferred = new DeferredIndexes(pg, options.schema());
            List<DeferredIndexes.Entry> toRebuild =
                    deferred.defer(manifest.snapshotId(), loadedTables(manifest), m -> say(progress, "indexes", m));

            int partitionsCreated = prepareTargetPartitions(pg, options, manifest);
            say(progress, "partitions", "Created " + partitionsCreated + " partition(s)");

            // Off until ANALYZE is done with the tables. Reset when the load fails too: a resumed
            // import switches it off again, and an abandoned one must not leave it off.
            Autovacuum.disable(pg, options.schema(), loadedTables(manifest));
            if (options.tunePostgres()) {
                PostgresTuning.apply(pg, options.schema()).forEach(m -> say(progress, "postgres", "Set " + m));
            }
            LoadResult loaded;
            long loadMillis;
            long indexMillis;
            long analyzeMillis;
            try {
                long t2 = System.currentTimeMillis();
                loaded = loadTables(options, manifest, extractRoot, journal, schema, progress);
                loadMillis = System.currentTimeMillis() - t2;

                // Before the handlers and sequence reset, which read the loaded tables.
                long tIndex = System.currentTimeMillis();
                long keys = toRebuild.stream().filter(DeferredIndexes.Entry::key).count();
                Progress.phase(progress, String.format("Rebuilding %d keys and %d indexes", keys, toRebuild.size() - keys));
                deferred.rebuild(manifest.snapshotId(), options.workers(),
                        DeferredIndexes.postgresMemory(options.memoryLimit()), () -> connect(options),
                        m -> say(progress, "indexes", m));
                List<String> indexProblems = deferred.problems(manifest.snapshotId());
                if (!indexProblems.isEmpty()) {
                    throw new IllegalStateException("Rebuilt keys and indexes do not match the schema:\n  - "
                            + String.join("\n  - ", indexProblems));
                }
                indexMillis = System.currentTimeMillis() - tIndex;

                // Before the handlers and the app's first start, both of which query the loaded tables.
                long tStats = System.currentTimeMillis();
                Progress.phase(progress, String.format("Analyzing %d tables (%d connections)", loadedTables(manifest).size(),
                        Math.max(1, Math.min(options.workers(), loadedTables(manifest).size()))));
                TableStatistics.analyze(options.schema(), loadedTables(manifest),
                        journal.rowsPerTable(manifest.snapshotId()), options.workers(), () -> connect(options),
                        m -> say(progress, "analyze", m));
                analyzeMillis = System.currentTimeMillis() - tStats;
            } catch (Exception e) {
                try {
                    Autovacuum.restore(pg, options.schema(), loadedTables(manifest));
                    PostgresTuning.restore(pg, options.schema());
                } catch (SQLException restore) {
                    e.addSuppressed(restore);
                }
                throw e;
            }
            Autovacuum.restore(pg, options.schema(), loadedTables(manifest));
            PostgresTuning.restore(pg, options.schema());

            long t3 = System.currentTimeMillis();
            Progress.phase(progress, "Control state: " + registry.handlerTables().size()
                    + " handlers, then sequence reset");
            List<String> handlerResults = runHandlers(pg, options, manifest);
            Map<String, Long> sequences = new SequenceReset(pg, options.schema()).resetTables(registry.isScoped()
                    ? registry.all().stream().map(SnapshotTableSpec::targetTable).collect(java.util.stream.Collectors.toSet()) : null);
            long controlMillis = System.currentTimeMillis() - t3;

            journal.setStatus(manifest.snapshotId(), ImportJournal.STATUS_VALIDATING,
                    "loaded " + loaded.rows + " rows; awaiting validation");

            if (!options.keepExtracted() && !options.skipExtraction()) {
                deleteRecursively(extractRoot);
            }

            return new ImportReport(manifest.snapshotId(), ImportJournal.STATUS_VALIDATING,
                    loaded.planned, loaded.skipped, loaded.executed, loaded.rows,
                    journal.rowsPerTable(manifest.snapshotId()), handlerResults, sequences,
                    partitionsCreated, registry.selectedLimitations(manifest.declaredLossy()), warnings,
                    verifyMillis, extractMillis, loadMillis, indexMillis, analyzeMillis, controlMillis);
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

    /** One table's load context, shared by every batch of that table. */
    private record TableWork(SnapshotTableSpec spec, TargetTable target,
                             Map<String, List<SnapshotManifest.FileEntry>> dependencyFiles,
                             int batches, AtomicInteger remaining, AtomicLong rows) {}

    private record WorkItem(TableWork table, ImportBatch batch) {}

    /**
     * Loads every table's batches from one queue, so all workers stay busy across table boundaries
     * and a single large table never runs on one worker while the rest idle. The largest batches go
     * first, which keeps the tail short. Batches are independent: each commits its rows and its
     * journal entry in one transaction, and dependencies are read from Parquet, not from loaded
     * tables.
     */
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

        // Resolve every table's mapping against the extracted files before writing a single row.
        // Preflight already planned the DIRECT and MAPPED tables from the manifest's recorded column
        // types; an SQL transform's output types only exist once the transform can be described, so
        // that has to wait until extraction — but it must still happen before the load, not several
        // tables into it.
        planSqlTransforms(options, manifest, ordered, filesBySpec, schema, planner, loader);

        List<WorkItem> items = new ArrayList<>();
        long skippedBatches = 0;
        long totalBytes = 0;
        for (SnapshotTableSpec spec : ordered) {
            List<ImportBatch> batches = planner.plan(manifest.snapshotId(), spec,
                    filesBySpec.getOrDefault(spec.id(), List.of()));
            if (batches.isEmpty()) {
                continue;
            }
            Map<String, List<SnapshotManifest.FileEntry>> dependencyFiles = new LinkedHashMap<>();
            for (String dep : spec.importSpec().dependencies()) {
                SnapshotTableSpec depSpec = registry.byId(dep).orElseThrow(
                        () -> new IllegalStateException("Unknown dependency '" + dep + "'"));
                dependencyFiles.put(depSpec.id(), filesBySpec.getOrDefault(depSpec.id(), List.of()));
            }
            List<ImportBatch> pending = batches.stream().filter(b -> !completed.contains(b.batchId())).toList();
            skippedBatches += batches.size() - pending.size();
            TableWork table = new TableWork(spec, schema.table(spec.targetTable()), dependencyFiles,
                    batches.size(), new AtomicInteger(pending.size()), new AtomicLong());
            for (ImportBatch batch : pending) {
                items.add(new WorkItem(table, batch));
                totalBytes += batch.bytes();
            }
        }
        items.sort(Comparator.comparingLong((WorkItem w) -> w.batch().bytes()).reversed());

        int planned = items.size() + (int) skippedBatches;
        int workers = Math.max(1, Math.min(options.workers(), items.size()));
        LoadProgress status = new LoadProgress(items.stream()
                .map(w -> new LoadProgress.Planned(w.table().spec().id(), w.batch().bytes())).toList(),
                workers, progress);
        // Rows at the consistency point, from the manifest: the Parquet files also hold rows past it.
        Progress.phase(progress, String.format("Loading %,d rows in %,d batches (%s of Parquet, %d workers)",
                items.stream().map(w -> w.table().spec().id()).distinct()
                        .mapToLong(id -> manifest.table(id).rowCount()).sum(),
                items.size(), Progress.bytes(totalBytes), workers));
        if (skippedBatches > 0) {
            status.say("Resuming: " + skippedBatches + " of " + planned + " batches already loaded");
        }

        status.start();
        ConcurrentLinkedQueue<WorkItem> queue = new ConcurrentLinkedQueue<>(items);
        AtomicLong executed = new AtomicLong();
        AtomicLong rows = new AtomicLong();
        List<Exception> failures = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        for (int w = 0; w < workers; w++) {
            Path spill = options.workDir().resolve("spill-" + w);
            Thread thread = Thread.ofPlatform().name("snapshot-import-" + w).unstarted(() -> {
                WorkItem item = null;
                try {
                    Files.createDirectories(spill);
                    try (DuckPgSession session = DuckPgSession.open(options, spill, 2)) {
                        while ((item = queue.poll()) != null) {
                            TableWork table = item.table();
                            SnapshotTableSpec spec = table.spec();
                            ImportBatch batch = item.batch();
                            long batchStarted = status.started(spec.id());
                            String select = loader.sourceSelect(spec, batch,
                                    manifest.point().slot(), manifest.point().epoch(), table.dependencyFiles());
                            ColumnPlan plan = loader.planFor(session, spec, select, table.target(),
                                    declaredColumns.get(spec.id()));
                            long n = UtxoBatchLoader.supports(spec)
                                    ? UtxoBatchLoader.load(loader, session, batch, plan, manifest,
                                            table.dependencyFiles(), options.schema())
                                    : loader.loadBatch(session, batch, plan, select,
                                            manifest.snapshotId(), options.schema());
                            rows.addAndGet(n);
                            table.rows().addAndGet(n);
                            executed.incrementAndGet();
                            status.finished(spec.id(), n, batch.bytes(), batchStarted);
                            if (progress != null) {
                                progress.accept(spec.id() + " +" + n + " rows");
                            }
                            if (table.remaining().decrementAndGet() == 0) {
                                log.info("Loaded {} ({} batches, {} rows)", spec.id(), table.batches(),
                                        table.rows().get());
                            }
                        }
                    }
                } catch (Exception e) {
                    String spec = item == null ? "?" : item.table().spec().id();
                    synchronized (failures) {
                        failures.add(new IllegalStateException("Failed loading '" + spec + "': "
                                + DuckPgSession.redact(e.getMessage(), options.password()), e));
                    }
                    queue.clear();
                }
            });
            threads.add(thread);
            thread.start();
        }
        try {
            for (Thread t : threads) {
                t.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Import interrupted", e);
        } finally {
            status.close();
        }
        if (!failures.isEmpty()) {
            Exception first = failures.get(0);
            throw new IllegalStateException(first.getMessage(), first.getCause());
        }
        status.done();
        return new LoadResult(planned, skippedBatches, executed.get(), rows.get());
    }

    /** Target tables that receive rows from the archive; their keys and indexes are deferred. */
    private List<String> loadedTables(SnapshotManifest manifest) {
        return registry.importedTables().stream()
                .filter(spec -> {
                    SnapshotManifest.TableManifest t = manifest.table(spec.id());
                    return t != null && t.files() != null && !t.files().isEmpty();
                })
                .map(SnapshotTableSpec::targetTable)
                .distinct()
                .toList();
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

    /**
     * Extracts the selected files from the parts that carry them; entries of skipped tables are read
     * past. Every part is checked against the published manifest, and afterwards every selected
     * file must have been written exactly once.
     */
    private void extract(SnapshotManifest archive, ArchiveVerifier.Selection selection, ImportOptions options,
                         Path extractRoot, Consumer<String> progress) throws IOException {
        Map<String, Long> expected = new TreeMap<>();
        Map<String, String> digests = new HashMap<>();
        archive.tables().forEach(t -> t.files().forEach(f -> {
            expected.put(f.path(), f.size());
            digests.put(f.path(), f.sha256());
        }));

        long maxEntries = expected.size() + 16L;
        long entryBytes = selection.files().stream().mapToLong(f -> expected.getOrDefault(f, 0L)).sum();
        long maxBytes = entryBytes + (1L << 20);
        ArchiveExtractor extractor = new ArchiveExtractor(extractRoot, maxEntries, maxBytes);

        List<SnapshotManifest.PartManifest> parts = selection.parts();
        int threads = Math.max(1, Math.min(options.workers(), ArchiveVerifier.DEFAULT_THREADS));
        say(progress, "extract", threads + " thread(s); each part and entry is hashed while it is extracted");
        long started = System.currentTimeMillis();
        AtomicInteger done = new AtomicInteger();
        AtomicLong doneBytes = new AtomicLong();
        Set<String> written = java.util.concurrent.ConcurrentHashMap.newKeySet();
        try (Progress.Ticker ignored = Progress.every(Progress.INTERVAL_MILLIS, "extract",
                () -> String.format("%d/%d parts  %s", done.get(), parts.size(),
                        Progress.rate(doneBytes.get(), entryBytes, started, System.currentTimeMillis())),
                progress)) {
            com.bloxbean.cardano.yaci.store.snapshot.archive.PartSource source = options.parts();
            int attempts = source.remote() ? PART_ATTEMPTS : 1;
            Parallel.map(parts, threads, part -> {
                ArchiveExtractor.Extracted result = null;
                for (int attempt = 1; result == null; attempt++) {
                    AtomicLong attemptBytes = new AtomicLong();
                    try (var in = source.open(part)) {
                        result = extractor.extract(in, expected, digests, selection.files()::contains, entry -> {
                            long n = expected.getOrDefault(entry, 0L);
                            attemptBytes.addAndGet(n);
                            doneBytes.addAndGet(n);
                        });
                        if (!result.partSha256().equals(part.sha256())) {
                            result = null;
                            throw new IOException("SHA-256 mismatch");
                        }
                    } catch (IOException e) {
                        doneBytes.addAndGet(-attemptBytes.get());
                        if (attempt >= attempts) {
                            // A damaged part usually fails ZIP's own CRC check before its SHA-256 is complete.
                            throw new IllegalStateException("Archive verification failed:\n  - "
                                    + part.fileName() + ": " + e.getMessage(), e);
                        }
                        say(progress, "extract", "Retrying " + part.fileName() + " (" + e.getMessage() + ")");
                    }
                }
                for (String name : result.written()) {
                    if (!written.add(name)) {
                        throw new IllegalStateException("Archive verification failed:\n  - "
                                + name + " appears in more than one part");
                    }
                }
                done.incrementAndGet();
                return result;
            });
        } catch (Parallel.PartFailure e) {
            throw e.getCause() instanceof IOException io ? io : new IOException(e.getMessage(), e.getCause());
        }
        Set<String> missing = new java.util.TreeSet<>(selection.files());
        missing.removeAll(written);
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Archive verification failed:\n  - " + missing.size()
                    + " file(s) declared in the manifest are in none of the parts read, e.g. " + missing.iterator().next());
        }
        say(progress, "extract", String.format("Extracted %d files from %d parts (%s) in %s", written.size(),
                parts.size(), Progress.bytes(entryBytes), Progress.duration(System.currentTimeMillis() - started)));
    }

    private static void say(Consumer<String> progress, String phase, String message) {
        log.info(message);
        if (progress != null) {
            progress.accept("[" + phase + "] " + message);
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
        for (SnapshotTableSpec spec : registry.knownTables()) {
            if (spec.restore() != RestoreMode.IMPORT) continue;
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
