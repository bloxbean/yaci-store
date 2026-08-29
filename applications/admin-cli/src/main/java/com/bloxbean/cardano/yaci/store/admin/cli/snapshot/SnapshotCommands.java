package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.admin.cli.Groups;
import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveVerifier;
import com.bloxbean.cardano.yaci.store.snapshot.export.ExportOptions;
import com.bloxbean.cardano.yaci.store.snapshot.export.InspectionReport;
import com.bloxbean.cardano.yaci.store.snapshot.export.SnapshotExporter;
import com.bloxbean.cardano.yaci.store.snapshot.export.TablePlan;
import com.bloxbean.cardano.yaci.store.snapshot.load.ImportJournal;
import com.bloxbean.cardano.yaci.store.snapshot.load.ImportOptions;
import com.bloxbean.cardano.yaci.store.snapshot.load.ImportReport;
import com.bloxbean.cardano.yaci.store.snapshot.load.SnapshotImporter;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestCodec;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.validate.SnapshotValidator;
import com.bloxbean.cardano.yaci.store.snapshot.validate.ValidationReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.error;
import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.info;
import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.success;
import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.warn;
import static com.bloxbean.cardano.yaci.store.admin.cli.common.ConsoleWriter.writeLn;

/**
 * Thin Spring Shell adapters over {@code components:snapshot}.
 *
 * <p>These commands contain no table-specific transformation logic: everything table-related lives
 * in the snapshot specifications and the generic executors.
 */
@Command(group = Groups.SNAPSHOT_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class SnapshotCommands {

    private final SnapshotCliSupport support;

    @Command(command = "snapshot inspect",
            description = "Report the consistency point, coverage and blockers for an analytics directory.")
    public void inspect(
            @Option(longNames = "data-dir", description = "Analytics data directory (defaults to the configured export path)")
            String dataDir,
            @Option(longNames = "work-dir", defaultValue = "./.snapshot-work",
                    description = "Scratch directory for the catalog copy")
            String workDir,
            @Option(longNames = "min-confirmations", defaultValue = "2160",
                    description = "Blocks the point must sit behind the newest exported block")
            long minConfirmations,
            @Option(longNames = "target-epoch", defaultValue = "0",
                    description = "Restore to this completed epoch instead of the newest supported one")
            int targetEpoch,
            @Option(longNames = "spec-file", description = "Additional local specification file(s)")
            String specFile,
            @Option(longNames = "allow-custom-specs", defaultValue = "false",
                    description = "Acknowledge the use of operator-supplied specifications")
            boolean allowCustomSpecs,
            @Option(longNames = "verbose", defaultValue = "false", description = "List every table")
            boolean verbose) {
        try {
            SnapshotCliSupport.ensureDirectory(Path.of(workDir));
            SnapshotSpecRegistry registry = support.registry(specFile, allowCustomSpecs);
            ExportOptions options = support.exportOptions(dataDir, workDir, workDir, "8GiB",
                    targetEpoch, minConfirmations, true, true);
            InspectionReport report = new SnapshotExporter(registry).inspect(options);
            printInspection(report, verbose);
        } catch (Exception e) {
            writeLn(error("snapshot inspect failed: %s", e.getMessage()));
            log.debug("inspect failed", e);
        }
    }

    @Command(command = "snapshot export",
            description = "Package the pinned analytics file set into independent, verifiable ZIP parts.")
    public void export(
            @Option(longNames = "data-dir", description = "Analytics data directory") String dataDir,
            @Option(longNames = "output", required = true, description = "Directory to write parts and manifest into")
            String output,
            @Option(longNames = "work-dir", defaultValue = "./.snapshot-work", description = "Scratch directory")
            String workDir,
            @Option(longNames = "part-size", defaultValue = "8GiB", description = "Target size of each ZIP part")
            String partSize,
            @Option(longNames = "min-confirmations", defaultValue = "2160",
                    description = "Blocks the point must sit behind the newest exported block")
            long minConfirmations,
            @Option(longNames = "target-epoch", defaultValue = "0",
                    description = "Restore to this completed epoch instead of the newest supported one")
            int targetEpoch,
            @Option(longNames = "allow-incomplete", defaultValue = "false",
                    description = "Package even though the snapshot has declared limitations")
            boolean allowIncomplete,
            @Option(longNames = "unsigned", defaultValue = "false",
                    description = "Acknowledge that no signature will be produced")
            boolean unsigned,
            @Option(longNames = "spec-file", description = "Additional local specification file(s)") String specFile,
            @Option(longNames = "allow-custom-specs", defaultValue = "false",
                    description = "Acknowledge the use of operator-supplied specifications")
            boolean allowCustomSpecs) {
        try {
            SnapshotCliSupport.ensureDirectory(Path.of(workDir));
            SnapshotCliSupport.ensureDirectory(Path.of(output));
            SnapshotSpecRegistry registry = support.registry(specFile, allowCustomSpecs);
            ExportOptions options = support.exportOptions(dataDir, output, workDir, partSize,
                    targetEpoch, minConfirmations, allowIncomplete, unsigned);

            writeLn(info("Packaging snapshot from %s", options.dataDir()));
            AtomicLong done = new AtomicLong();
            SnapshotExporter.ExportResult result = new SnapshotExporter(registry).export(options, p -> {
                long n = done.incrementAndGet();
                if (n % 500 == 0) {
                    writeLn(info("  %d files processed", n));
                }
            });

            SnapshotManifest m = result.manifest();
            writeLn(success("Snapshot %s written to %s", m.snapshotId(), options.outputDir()));
            writeLn("  Point         : epoch %d, block %d, slot %d", m.point().epoch(),
                    m.point().blockNumber(), m.point().slot());
            writeLn("  Block hash    : %s", m.point().blockHash());
            writeLn("  Tables        : %d, rows %d", m.tables().size(), m.totalRows());
            writeLn("  Parts         : %d (%s)", m.parts().size(),
                    SnapshotCliSupport.humanBytes(result.bytesWritten()));
            writeLn("  Manifest      : %s", result.manifestPath().getFileName());
            writeLn("  Duration      : %s", SnapshotCliSupport.humanMillis(result.millis()));
            if (!m.declaredLossy().isEmpty()) {
                writeLn(warn("Declared limitations (%d):", m.declaredLossy().size()));
                m.declaredLossy().forEach(l -> writeLn("    - %s", l));
            }
        } catch (Exception e) {
            writeLn(error("snapshot export failed: %s", e.getMessage()));
            log.debug("export failed", e);
        }
    }

    @Command(command = "snapshot verify",
            description = "Verify a snapshot manifest and every archive part without touching the database.")
    public void verify(
            @Option(longNames = "manifest", required = true, description = "Path to the manifest JSON")
            String manifest,
            @Option(longNames = "deep", defaultValue = "true",
                    description = "Also scan part entries for traversal, duplicates and coverage")
            boolean deep) {
        try {
            Path manifestPath = Path.of(manifest).toAbsolutePath().normalize();
            SnapshotManifest m = new ManifestCodec().read(manifestPath);
            ArchiveVerifier.Result result =
                    new ArchiveVerifier().verify(m, manifestPath.getParent(), deep);
            writeLn(info("Snapshot %s (%s, epoch %d)", m.snapshotId(), m.point().network(), m.point().epoch()));
            writeLn("  Parts verified : %d (%s)", m.parts().size(),
                    SnapshotCliSupport.humanBytes(result.bytesVerified()));
            if (result.ok()) {
                writeLn(success("All parts match the manifest"));
            } else {
                writeLn(error("Verification failed:"));
                result.problems().forEach(p -> writeLn("    - %s", p));
            }
        } catch (Exception e) {
            writeLn(error("snapshot verify failed: %s", e.getMessage()));
            log.debug("verify failed", e);
        }
    }

    @Command(command = "snapshot import",
            description = "Import a verified snapshot into the configured empty, migrated schema.")
    public void importSnapshot(
            @Option(longNames = "manifest", required = true, description = "Path to the manifest JSON")
            String manifest,
            @Option(longNames = "work-dir", required = true,
                    description = "Directory for extracted Parquet and per-worker DuckDB spill files")
            String workDir,
            @Option(longNames = "workers", defaultValue = "4", description = "Bounded worker count") int workers,
            @Option(longNames = "memory-limit", defaultValue = "4GB", description = "Per-worker DuckDB memory limit")
            String memoryLimit,
            @Option(longNames = "min-free-disk-gb", defaultValue = "20",
                    description = "Abort before free space drops below this")
            long minFreeDiskGb,
            @Option(longNames = "allow-unsigned", defaultValue = "false",
                    description = "Import a snapshot with no signature")
            boolean allowUnsigned,
            @Option(longNames = "spec-file", description = "Additional local specification file(s)") String specFile,
            @Option(longNames = "allow-custom-specs", defaultValue = "false",
                    description = "Acknowledge the use of operator-supplied specifications")
            boolean allowCustomSpecs,
            @Option(longNames = "keep-extracted", defaultValue = "false",
                    description = "Keep extracted Parquet after a successful import")
            boolean keepExtracted,
            @Option(longNames = "dry-run", defaultValue = "false",
                    description = "Run preflight checks only; write nothing")
            boolean dryRun) {
        try {
            SnapshotCliSupport.ensureDirectory(Path.of(workDir));
            SnapshotSpecRegistry registry = support.registry(specFile, allowCustomSpecs);
            ImportOptions options = support.importOptions(manifest, workDir, workers, memoryLimit,
                    minFreeDiskGb, allowUnsigned, specFile, allowCustomSpecs, keepExtracted);
            SnapshotImporter importer = new SnapshotImporter(registry);

            SnapshotImporter.Preflight pre = importer.preflight(options);
            pre.warnings().forEach(w -> writeLn(warn(w)));
            if (!pre.ok()) {
                writeLn(error("Import is blocked:"));
                pre.blockers().forEach(bl -> writeLn("    - %s", bl));
                return;
            }
            if (dryRun) {
                writeLn(success("Preflight passed. Nothing was written (--dry-run)."));
                return;
            }
            if (pre.existingRun() != null) {
                writeLn(info("Resuming import %s (status %s)", pre.existingRun().snapshotId(),
                        pre.existingRun().status()));
            }

            AtomicLong ticks = new AtomicLong();
            ImportReport report = importer.importSnapshot(options, p -> {
                if (ticks.incrementAndGet() % 200 == 0) {
                    writeLn(info("  %s", p));
                }
            });
            printImportReport(report);
            writeLn(info("Next: run 'snapshot validate --manifest %s', then 'apply-indexes'.", manifest));
        } catch (Exception e) {
            writeLn(error("snapshot import failed: %s", e.getMessage()));
            log.debug("import failed", e);
        }
    }

    @Command(command = "snapshot import-status",
            description = "Show the state of any snapshot import recorded in the configured schema.")
    public void importStatus() {
        try (Connection conn = support.connect()) {
            ImportJournal journal = new ImportJournal(conn, support.schema());
            if (!journal.exists()) {
                writeLn(info("No snapshot import journal in schema '%s'.", support.schema()));
                return;
            }
            journal.currentRun().ifPresentOrElse(run -> {
                writeLn(info("Snapshot %s", run.snapshotId()));
                writeLn("  Status  : %s", run.status());
                writeLn("  Network : %s (magic %d)", run.network(), run.protocolMagic());
                writeLn("  Point   : epoch %d, block %d, slot %d", run.pointEpoch(), run.pointBlock(),
                        run.pointSlot());
                if (run.message() != null) {
                    writeLn("  Note    : %s", run.message());
                }
                try {
                    writeLn("  Batches : %d completed", journal.completedCount(run.snapshotId()));
                    journal.rowsPerTable(run.snapshotId())
                            .forEach((t, n) -> writeLn("    %-32s %d", t, n));
                } catch (Exception e) {
                    writeLn(warn("Could not read batch details: %s", e.getMessage()));
                }
            }, () -> writeLn(info("Journal exists but records no run.")));
        } catch (Exception e) {
            writeLn(error("snapshot import-status failed: %s", e.getMessage()));
            log.debug("import-status failed", e);
        }
    }

    @Command(command = "snapshot validate",
            description = "Run the offline validation levels against the imported database.")
    public void validate(
            @Option(longNames = "manifest", required = true, description = "Path to the manifest JSON")
            String manifest,
            @Option(longNames = "spec-file", description = "Additional local specification file(s)") String specFile,
            @Option(longNames = "allow-custom-specs", defaultValue = "false",
                    description = "Acknowledge the use of operator-supplied specifications")
            boolean allowCustomSpecs,
            @Option(longNames = "mark-ready", defaultValue = "false",
                    description = "On success, mark the import READY and drop the temporary journal")
            boolean markReady) {
        try {
            SnapshotManifest m = new ManifestCodec().read(Path.of(manifest).toAbsolutePath().normalize());
            SnapshotSpecRegistry registry = support.registry(specFile, allowCustomSpecs);
            SnapshotValidator validator = new SnapshotValidator(registry);

            boolean allPassed = true;
            try (Connection conn = support.connect()) {
                List<ValidationReport> reports = validator.validateAll(conn, support.schema(), m,
                        support.eventPublisherId(), support.cursorBlocksToKeep());
                for (ValidationReport report : reports) {
                    writeLn(info("Level: %s", report.level()));
                    for (ValidationReport.Check c : report.checks()) {
                        if (c.passed()) {
                            writeLn("    [ok]   %-34s %s", c.name(), c.detail());
                        } else {
                            writeLn(error("    [FAIL] %-34s %s", c.name(), c.detail()));
                        }
                    }
                    allPassed &= report.passed();
                }

                if (allPassed) {
                    writeLn(success("All offline validation levels passed."));
                    if (!m.declaredLossy().isEmpty()) {
                        writeLn(warn("The snapshot declares %d limitation(s); this database is not an "
                                + "exact copy of the source:", m.declaredLossy().size()));
                        m.declaredLossy().forEach(l -> writeLn("    - %s", l));
                    }
                    writeLn(info("Remaining application-acceptance steps:"));
                    SnapshotValidator.applicationAcceptanceChecklist()
                            .forEach(step -> writeLn("    - %s", step));
                    if (markReady) {
                        ImportJournal journal = new ImportJournal(conn, support.schema());
                        journal.setStatus(m.snapshotId(), ImportJournal.STATUS_READY, "validated");
                        journal.drop();
                        writeLn(success("Import marked READY and the temporary journal removed."));
                    }
                } else {
                    writeLn(error("Validation failed. The database must not be treated as ready."));
                }
            }
        } catch (Exception e) {
            writeLn(error("snapshot validate failed: %s", e.getMessage()));
            log.debug("validate failed", e);
        }
    }

    // ---------------------------------------------------------------- output

    private void printInspection(InspectionReport r, boolean verbose) {
        writeLn(info("DuckLake catalog snapshot %d (DuckDB %s)", r.ducklakeSnapshotId(), r.duckdbVersion()));
        if (r.point() != null) {
            writeLn(success("Consistency point: epoch %d, block %d, slot %d",
                    r.completedEpoch(), r.point().blockNumber(), r.point().slot()));
            writeLn("  Block hash     : %s", r.point().blockHash());
            writeLn("  Era            : %d", r.point().era());
            writeLn("  Newest export  : block %d (the point is %d blocks behind it)",
                    r.maxExportedBlock(), r.maxExportedBlock() - r.point().blockNumber());
            writeLn("  Limited by     : %s", String.join(", ", r.limitedBy()));
        } else {
            writeLn(error("No consistency point could be selected"));
        }

        writeLn(info("Coverage: %d table(s), %d file(s), %d row(s), about %s",
                r.plans().size(), r.fileCount(), r.totalRows(),
                SnapshotCliSupport.humanBytes(r.estimatedArchiveBytes())));
        if (verbose) {
            writeLn("  %-30s %10s %10s  %s", "table", "rows", "files", "supports epoch");
            for (TablePlan p : r.plans()) {
                Integer gate = r.gatingEpochs().get(p.spec().id());
                writeLn("  %-30s %10d %10d  %s", p.spec().id(), p.rowCount(), p.files().size(),
                        gate == null ? "-" : gate.toString());
            }
            writeLn(info("Tables not loaded from Parquet:"));
            r.nonImportedTables().forEach(s ->
                    writeLn("  %-30s %-16s %s", s.targetTable(), s.restore(), s.reason()));
        }

        if (!r.declaredLossy().isEmpty()) {
            writeLn(warn("Declared limitations (%d):", r.declaredLossy().size()));
            r.declaredLossy().forEach(l -> writeLn("    - %s", l));
        }
        r.warnings().forEach(w -> writeLn(warn(w)));
        if (r.canExport()) {
            writeLn(success("No blockers. This directory can be exported."));
        } else {
            writeLn(error("Blockers (%d):", r.blockers().size()));
            r.blockers().forEach(bl -> writeLn("    - %s", bl));
        }
    }

    private void printImportReport(ImportReport r) {
        writeLn(success("Import %s finished with status %s", r.snapshotId(), r.status()));
        writeLn("  Batches      : %d planned, %d skipped (already committed), %d loaded",
                r.batchesPlanned(), r.batchesSkipped(), r.batchesLoaded());
        writeLn("  Rows loaded  : %d", r.rowsLoaded());
        writeLn("  Partitions   : %d created", r.partitionsCreated());
        writeLn("  Verify       : %s", SnapshotCliSupport.humanMillis(r.verifyMillis()));
        writeLn("  Extract      : %s", SnapshotCliSupport.humanMillis(r.extractMillis()));
        writeLn("  Heap load    : %s", SnapshotCliSupport.humanMillis(r.loadMillis()));
        writeLn("  Control state: %s", SnapshotCliSupport.humanMillis(r.controlStateMillis()));
        r.handlerResults().forEach(h -> writeLn("    %s", h));
        if (!r.sequencesReset().isEmpty()) {
            writeLn("  Sequences    : %s", r.sequencesReset());
        }
        r.warnings().forEach(w -> writeLn(warn(w)));
        if (!r.declaredLimitations().isEmpty()) {
            writeLn(warn("Declared limitations carried by this snapshot (%d):",
                    r.declaredLimitations().size()));
            r.declaredLimitations().forEach(l -> writeLn("    - %s", l));
        }
        writeLn(info("Index creation is not part of import. Run 'apply-indexes' after validation."));
    }
}
