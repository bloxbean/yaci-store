package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.admin.cli.common.CommandFailedException;
import com.bloxbean.cardano.yaci.store.admin.cli.Groups;
import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveVerifier;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
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
import com.bloxbean.cardano.yaci.store.snapshot.util.Progress;
import com.bloxbean.cardano.yaci.store.snapshot.validate.SnapshotValidator;
import com.bloxbean.cardano.yaci.store.snapshot.validate.ValidationReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
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
                    description = "Scratch directory for DuckDB work files")
            String workDir,
            @Option(longNames = "min-confirmations", defaultValue = "2160",
                    description = "Blocks the point must sit behind the newest exported block")
            long minConfirmations,
            @Option(longNames = "target-epoch", defaultValue = "0",
                    description = "Restore to this completed epoch instead of the newest supported one")
            int targetEpoch,
            @Option(longNames = "memory-limit", defaultValue = DuckLakeCatalog.DEFAULT_MEMORY_LIMIT,
                    description = "DuckDB memory limit for planning; a larger value is faster, never required")
            String memoryLimit,
            @Option(longNames = "spec-file", description = "Additional local specification file(s)")
            String specFile,
            @Option(longNames = "allow-custom-specs", defaultValue = "false",
                    description = "Acknowledge the use of operator-supplied specifications")
            boolean allowCustomSpecs,
            @Option(longNames = "verbose", defaultValue = "false",
                    description = "List every table, and every incomplete partition of a blocker")
            boolean verbose) {
        try {
            SnapshotCliSupport.ensureDirectory(Path.of(workDir));
            SnapshotSpecRegistry registry = support.exportRegistry(specFile, allowCustomSpecs);
            ExportOptions options = support.exportOptions(registry, dataDir, workDir, workDir, "8GiB",
                    targetEpoch, minConfirmations, true, true, memoryLimit);
            InspectionReport report = new SnapshotExporter(registry).inspect(options);
            printInspection(report, verbose);
            printBlockerReport(report, options.workDir(), verbose);
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw failed("snapshot inspect", e);
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
            @Option(longNames = "signing-key",
                    description = "Ed25519 private key (PKCS#8 PEM) to sign the snapshot with (default: store.snapshot.signing-key)")
            String signingKey,
            @Option(longNames = "memory-limit", defaultValue = DuckLakeCatalog.DEFAULT_MEMORY_LIMIT,
                    description = "DuckDB memory limit for planning; a larger value is faster, never required")
            String memoryLimit,
            @Option(longNames = "spec-file", description = "Additional local specification file(s)") String specFile,
            @Option(longNames = "allow-custom-specs", defaultValue = "false",
                    description = "Acknowledge the use of operator-supplied specifications")
            boolean allowCustomSpecs,
            @Option(longNames = "verify", defaultValue = "false",
                    description = "Re-read every written part and check its digest before publishing the manifest")
            boolean verify,
            @Option(longNames = "publish", defaultValue = "false",
                    description = "Upload the snapshot when the export succeeds; it is staged unless --live")
            boolean publish,
            @Option(longNames = "live", defaultValue = "false",
                    description = "With --publish: add it to index.json at once (default: staged, importable by id)")
            boolean live,
            @Option(longNames = "history-split", defaultValue = "true",
                    description = "Package the history of address_utxo, tx_input and epoch_stake in parts of its own,"
                            + " so an import into a target that prunes them skips it")
            boolean historySplit,
            @Option(longNames = "utxo-safe-blocks", defaultValue = "2160",
                    description = "History split: outputs spent, and inputs, before this many blocks before the point are history")
            int utxoSafeBlocks,
            @Option(longNames = "epoch-stake-epochs", defaultValue = "8",
                    description = "History split: epoch_stake epochs older than the newest N are history (at least 4)")
            int epochStakeEpochs,
            @Option(longNames = "remote", description = SnapshotRemoteCommands.REMOTE_HELP) String remote) {
        try {
            SnapshotCliSupport.ensureDirectory(Path.of(workDir));
            SnapshotCliSupport.ensureDirectory(Path.of(output));
            SnapshotSpecRegistry registry = support.exportRegistry(specFile, allowCustomSpecs);
            SnapshotCliSupport.SigningKey key = unsigned ? null : support.signingKey(signingKey);
            if (key == null && !unsigned) {
                throw new CommandFailedException("snapshot export: pass --signing-key (or set store.snapshot.signing-key),"
                        + " or --unsigned to produce an unsigned snapshot", null);
            }
            ExportOptions options = support.exportOptions(registry, dataDir, output, workDir, partSize,
                    targetEpoch, minConfirmations, allowIncomplete, unsigned, memoryLimit)
                    .withHistorySplit(historySplit, utxoSafeBlocks, epochStakeEpochs);

            writeLn(info("Packaging snapshot from %s%s", options.dataDir(), historySplit ? ", history in parts of its own" : ""));
            SnapshotExporter.ExportResult result;
            try {
                SnapshotExporter exporter = new SnapshotExporter(registry).verifyParts(verify);
                result = exporter.export(options, new ProgressPrinter(exporter.phases(options)));
            } catch (SnapshotExporter.BlockedException e) {
                CommandFailedException failure = failed("snapshot export", e);
                printBlockerReport(e.report(), options.workDir(), false);
                throw failure;
            }

            SnapshotManifest m = result.manifest();
            writeLn(success("Snapshot %s written to %s", m.snapshotId(), options.outputDir()));
            writeLn("  Point         : epoch %d, block %d, slot %d", m.point().epoch(),
                    m.point().blockNumber(), m.point().slot());
            writeLn("  Block hash    : %s", m.point().blockHash());
            writeLn("  Tables        : %d, rows %d", m.tables().size(), m.totalRows());
            if (!m.retentionOrEmpty().isEmpty()) {
                writeLn("  Retention     : %s", com.bloxbean.cardano.yaci.store.snapshot.manifest.Retention.format(m.retention()));
            }
            writeLn("  Parts         : %d (%s)", m.parts().size(),
                    SnapshotCliSupport.humanBytes(result.bytesWritten()));
            writeLn("  Manifest      : %s", result.manifestPath().getFileName());
            writeLn("  Duration      : %s", SnapshotCliSupport.humanMillis(result.millis()));
            if (key != null) {
                String keyId = com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotSignature.sign(
                        result.manifestPath(), key.privateKey(), key.publicKey());
                writeLn("  Signed        : key %s (SHA256SUMS.sig)", keyId);
            } else {
                writeLn(warn("  Signed        : no (--unsigned)"));
            }
            if (!registry.selectedLimitations(m.declaredLossy()).isEmpty()) {
                writeLn(warn("Declared limitations (%d):", registry.selectedLimitations(m.declaredLossy()).size()));
                registry.selectedLimitations(m.declaredLossy()).forEach(l -> writeLn("    - %s", l));
            }
            if (publish) {
                try (var store = support.remoteStore(remote)) {
                    SnapshotRemoteCommands.publish(store, result.manifestPath(), 4, live);
                }
            }
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw failed("snapshot export", e);
        }
    }

    @Command(command = "snapshot verify",
            description = "Verify a snapshot manifest and every archive part without touching the database.")
    public void verify(
            @Option(longNames = "manifest", required = true, description = "Path to the manifest JSON")
            String manifest,
            @Option(longNames = "deep", defaultValue = "true",
                    description = "Also scan part entries for traversal, duplicates and coverage")
            boolean deep,
            @Option(longNames = "trusted-key",
                    description = "Publisher public key(s), comma-separated PEM files (default: store.snapshot.trusted-keys)")
            String trustedKey) {
        try {
            Path manifestPath = Path.of(manifest).toAbsolutePath().normalize();
            SnapshotManifest m = new ManifestCodec().read(manifestPath);
            writeLn(info("Snapshot %s (%s, epoch %d)", m.snapshotId(), m.point().network(), m.point().epoch()));
            long total = m.parts().stream().mapToLong(SnapshotManifest.PartManifest::size).sum();
            writeLn(info("Verifying %d parts (%s) with %d threads", m.parts().size(),
                    SnapshotCliSupport.humanBytes(total), Math.min(m.parts().size(), ArchiveVerifier.DEFAULT_THREADS)));
            long started = System.currentTimeMillis();
            AtomicLong hashed = new AtomicLong();
            ArchiveVerifier.Result result;
            try (Progress.Ticker ignored = Progress.every(Progress.INTERVAL_MILLIS, "verify",
                    () -> Progress.rate(hashed.get(), total, started, System.currentTimeMillis()),
                    new ProgressPrinter(0))) {
                result = new ArchiveVerifier().verify(m, manifestPath.getParent(), deep, hashed::addAndGet);
            }
            writeLn("  Parts verified : %d (%s)", m.parts().size(),
                    SnapshotCliSupport.humanBytes(result.bytesVerified()));
            var signature = com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotSignature.verify(manifestPath,
                    support.trustedKeys(trustedKey));
            switch (signature.status()) {
                case VERIFIED -> writeLn(success("Signature: %s", signature.describe()));
                case INVALID -> {
                    writeLn(error("Signature: %s", signature.describe()));
                    throw new CommandFailedException("snapshot verify: signature verification failed", null);
                }
                default -> writeLn(warn("Signature: %s", signature.describe()));
            }
            if (result.ok()) {
                writeLn(success("All parts match the manifest"));
            } else {
                writeLn(error("Verification failed:"));
                result.problems().forEach(p -> writeLn("    - %s", p));
                throw new CommandFailedException("snapshot verify: verification failed", null);
            }
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw failed("snapshot verify", e);
        }
    }

    @Command(command = "snapshot import",
            description = "Import a verified snapshot into the configured empty, migrated schema.")
    public void importSnapshot(
            @Option(longNames = "manifest", description = "Path to the manifest JSON of a local snapshot")
            String manifest,
            @Option(longNames = "snapshot",
                    description = "Instead of --manifest: a published snapshot id (a unique prefix of a live one's;"
                            + " the full id of a staged one), or latest,"
                            + " streamed from --remote")
            String snapshot,
            @Option(longNames = "remote", description = SnapshotRemoteCommands.REMOTE_HELP) String remote,
            @Option(longNames = "work-dir", required = true,
                    description = "Directory for extracted Parquet and per-worker DuckDB spill files")
            String workDir,
            @Option(longNames = "workers", defaultValue = "1", description = "Bounded worker count") int workers,
            @Option(longNames = "memory-limit", defaultValue = ImportOptions.DEFAULT_MEMORY_LIMIT, description = "Per-worker DuckDB memory limit")
            String memoryLimit,
            @Option(longNames = "min-free-disk-gb", defaultValue = "20",
                    description = "Minimum free space required in the work directory at preflight")
            long minFreeDiskGb,
            @Option(longNames = "allow-unsigned", defaultValue = "false",
                    description = "Import a snapshot without a signature verified by a trusted key")
            boolean allowUnsigned,
            @Option(longNames = "trusted-key",
                    description = "Publisher public key(s), comma-separated PEM files (default: store.snapshot.trusted-keys)")
            String trustedKey,
            @Option(longNames = "spec-file", description = "Additional local specification file(s)") String specFile,
            @Option(longNames = "allow-custom-specs", defaultValue = "false",
                    description = "Acknowledge the use of operator-supplied specifications")
            boolean allowCustomSpecs,
            @Option(longNames = "keep-extracted", defaultValue = "false",
                    description = "Keep extracted Parquet after a successful import")
            boolean keepExtracted,
            @Option(longNames = "dry-run", defaultValue = "false",
                    description = "Run preflight checks only; write nothing")
            boolean dryRun,
            @Option(longNames = "validate", defaultValue = "false",
                    description = "After the import, run 'snapshot validate --mark-ready' in the same command")
            boolean validate,
            @Option(longNames = "tune-postgres", defaultValue = "false",
                    description = "Raise max_wal_size, checkpoint_timeout and wal_compression with ALTER SYSTEM"
                            + " for the import and restore them afterwards (needs superuser or ALTER SYSTEM grants)")
            boolean tunePostgres,
            @Option(longNames = "epoch-stake-epochs", defaultValue = "0",
                    description = "Load only the newest N epochs of epoch_stake (at least 4). Default: the target's"
                            + " store.adapot.epoch-stake-safe-epochs when it runs with"
                            + " store.adapot.epoch-stake-pruning-enabled=true, otherwise every epoch")
            int epochStakeEpochs) {
        boolean local = manifest != null && !manifest.isBlank();
        if (local == (snapshot != null && !snapshot.isBlank())) {
            throw new CommandFailedException("snapshot import: pass either --manifest or --snapshot", null);
        }
        com.bloxbean.cardano.yaci.store.snapshot.remote.SnapshotStore store = null;
        try {
            SnapshotCliSupport.ensureDirectory(Path.of(workDir));
            SnapshotSpecRegistry registry = support.importRegistry(specFile, allowCustomSpecs);
            com.bloxbean.cardano.yaci.store.snapshot.archive.PartSource parts = null;
            if (!local) {
                store = support.remoteStore(remote);
                var repo = new com.bloxbean.cardano.yaci.store.snapshot.remote.SnapshotRepository(store);
                // Chosen against this target's schema too, so latest is the newest snapshot it can take.
                com.bloxbean.cardano.yaci.store.snapshot.remote.SnapshotRepository.Resolved resolved;
                try (java.sql.Connection target = support.connect()) {
                    resolved = repo.resolve(support.network(), snapshot,
                            SnapshotRemoteCommands.compatibility(registry, target, support.schema()), new ProgressPrinter(0));
                }
                Path dir = Path.of(workDir).toAbsolutePath().normalize()
                        .resolve("snapshot-" + resolved.entry().snapshotId());
                manifest = repo.downloadMetadata(resolved, dir).toString();
                parts = repo.partSource(resolved);
                SnapshotRemoteCommands.printSnapshot(resolved, store);
                writeLn("  Manifest    : saved to %s", manifest);
            }
            ImportOptions options = support.importOptions(manifest, workDir, workers, memoryLimit,
                    minFreeDiskGb, allowUnsigned, specFile, allowCustomSpecs, keepExtracted, tunePostgres,
                    support.epochStakeEpochs(epochStakeEpochs)).withPartSource(parts).withTrustedKeys(support.trustedKeys(trustedKey));
            SnapshotImporter importer = new SnapshotImporter(registry);

            if (dryRun) {
                SnapshotImporter.Preflight pre = importer.preflight(options);
                printDryRun(pre.manifest(), registry, options, importer.selection(pre.archive(), pre.manifest()));
                if (pre.signature() != null) {
                    writeLn(pre.signature().trusted() ? success("Signature: %s", pre.signature().describe())
                            : warn("Signature: %s", pre.signature().describe()));
                }
                // The table list above already shows the selected and skipped tables.
                pre.warnings().stream()
                        .filter(w -> !w.startsWith("Selected tables: ") && !w.startsWith("Skipped tables: "))
                        .forEach(w -> writeLn(warn(w)));
                if (!pre.ok()) {
                    writeLn(error("Import is blocked:"));
                    pre.blockers().forEach(bl -> writeLn("    - %s", bl));
                } else {
                    writeLn(success("Preflight passed (mappings and transform output types checked against the target; "
                            + "part existence and sizes checked, digests are verified during import). Nothing written."));
                }
                return;
            }

            ProgressPrinter printer = new ProgressPrinter(SnapshotImporter.PHASES + (validate ? 1 : 0));
            ImportReport report = importer.importSnapshot(options, printer);
            printImportReport(report);
            if (!validate) {
                writeLn(info("Next: run 'snapshot validate --manifest %s', then 'apply-indexes'.", manifest));
                return;
            }
            if (!ImportJournal.STATUS_VALIDATING.equals(report.status())) {
                writeLn(error("Import ended with status %s; skipping validation.", report.status()));
                throw new CommandFailedException("snapshot import: status " + report.status(), null);
            }
            SnapshotManifest m = new ManifestCodec().read(Path.of(manifest).toAbsolutePath().normalize());
            printer.accept(Progress.PHASE_PREFIX + "Validation");
            if (!runValidation(m, registry, true, workers, printer.nested("validate"))) {
                throw new CommandFailedException("snapshot import: validation failed", null);
            }
            writeLn(info("Next (optional): run 'apply-indexes'."));
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw failed("snapshot import", e);
        } finally {
            if (store != null) {
                store.close();
            }
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
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw failed("snapshot import-status", e);
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
            boolean markReady,
            @Option(longNames = "workers", defaultValue = "1",
                    description = "Tables scanned in parallel, one database connection each")
            int workers) {
        try {
            SnapshotManifest m = new ManifestCodec().read(Path.of(manifest).toAbsolutePath().normalize());
            if (!runValidation(m, support.importRegistry(specFile, allowCustomSpecs), markReady, workers,
                    new ProgressPrinter(SnapshotValidator.PHASES))) {
                throw new CommandFailedException("snapshot validate: validation failed", null);
            }
        } catch (CommandFailedException e) {
            throw e;
        } catch (Exception e) {
            throw failed("snapshot validate", e);
        }
    }

    /**
     * Prints why a command failed, with its cause chain (the top-level message alone often hides the
     * real error), and returns the exception that makes the process exit non-zero.
     */
    static CommandFailedException failed(String command, Exception e) {
        writeLn(error("%s failed: %s", command, e.getMessage()));
        java.util.Set<String> seen = new java.util.HashSet<>();
        seen.add(String.valueOf(e.getMessage()));
        int depth = 0;
        for (Throwable c = e.getCause(); c != null && depth < 5; c = c.getCause(), depth++) {
            String message = c.getClass().getSimpleName() + ": " + c.getMessage();
            if (c.getMessage() != null && seen.add(c.getMessage())) {
                writeLn("    caused by %s", message);
            }
        }
        writeLn("    (stack trace: LOGGING_LEVEL_COM_BLOXBEAN_CARDANO_YACI_STORE_ADMIN_CLI_SNAPSHOT=DEBUG)");
        log.debug(command + " failed", e);
        return new CommandFailedException(command + " failed", e);
    }

    // ---------------------------------------------------------------- validation

    /**
     * Runs every offline validation level and, when all pass and {@code markReady} is set, marks the
     * import READY and drops the journal.
     *
     * @param progress receives the validator's banners and its status lines, tagged {@code [validate]}
     * @return true when every level passed
     */
    private boolean runValidation(SnapshotManifest m, SnapshotSpecRegistry registry, boolean markReady,
                                  int workers, java.util.function.Consumer<String> progress) throws Exception {
        SnapshotValidator validator = new SnapshotValidator(registry);

        boolean allPassed = true;
        try (Connection conn = support.connect()) {
            List<ValidationReport> reports = validator.validateAll(conn, support.schema(), m,
                    support.eventPublisherId(), support.cursorBlocksToKeep(), support::connect, workers,
                    p -> progress.accept(p.startsWith(Progress.PHASE_PREFIX) ? p : "[validate] " + p));
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
                if (!registry.selectedLimitations(m.declaredLossy()).isEmpty()) {
                    writeLn(warn("The snapshot declares %d limitation(s); this database is not an "
                            + "exact copy of the source:", registry.selectedLimitations(m.declaredLossy()).size()));
                    registry.selectedLimitations(m.declaredLossy()).forEach(l -> writeLn("    - %s", l));
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
        return allPassed;
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

    /**
     * An incomplete-export blocker names only its first few partitions; a backfill needs all of
     * them, so the full lists go to a file in the work directory (and to the console with
     * {@code --verbose}).
     */
    private void printBlockerReport(InspectionReport r, Path workDir, boolean verbose) throws java.io.IOException {
        Path file = writeBlockerReport(r, workDir);
        if (file == null) {
            return;
        }
        Map<String, List<String>> lists = r.truncatedPartitionLists();
        if (verbose) {
            lists.forEach((spec, partitions) -> {
                writeLn(warn("[%s] %d incomplete partition(s):", spec, partitions.size()));
                partitions.forEach(p -> writeLn("    - %s", p));
            });
        }
        writeLn(info("Every incomplete partition (%d in %d table(s)) is listed in %s",
                lists.values().stream().mapToInt(List::size).sum(), lists.size(), file));
    }

    /** Writes the blockers and every truncated partition list; null when no list was truncated. */
    static Path writeBlockerReport(InspectionReport r, Path workDir) throws java.io.IOException {
        Map<String, List<String>> lists = r.truncatedPartitionLists();
        if (lists.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        r.blockers().forEach(b -> sb.append(b).append('\n'));
        lists.forEach((spec, partitions) -> {
            sb.append('\n').append('[').append(spec).append("] ").append(partitions.size())
                    .append(" incomplete partition(s):\n");
            partitions.forEach(p -> sb.append(p).append('\n'));
        });
        Path file = workDir.resolve("snapshot-blockers.txt");
        Files.writeString(file, sb.toString());
        return file;
    }

    /**
     * Restored database size per byte of loaded Parquet, keys and indexes included: preprod restores
     * of 7.5 GiB of Parquet took 59 to 63 GB. A rough guide only; row width and compression vary.
     */
    static final int DATABASE_BYTES_PER_PARQUET_BYTE = 8;

    private void printDryRun(SnapshotManifest m, SnapshotSpecRegistry registry, ImportOptions options,
                             ArchiveVerifier.Selection selection) throws java.io.IOException {
        writeLn(info("Snapshot %s (%s, epoch %d, block %d, slot %d)", m.snapshotId(), m.point().network(),
                m.point().epoch(), m.point().blockNumber(), m.point().slot()));
        writeLn("  Target schema : %s at %s", options.schema(), redactedUrl(options.jdbcUrl()));
        describeImport(m, registry).forEach(l -> writeLn("  %s", l));

        List<String> limitations = registry.selectedLimitations(m.declaredLossy());
        if (!limitations.isEmpty()) {
            writeLn(warn("Declared limitations (%d):", limitations.size()));
            limitations.forEach(l -> writeLn("    - %s", l));
        }

        Map<String, Long> sizes = new java.util.HashMap<>();
        m.tables().forEach(t -> t.files().forEach(f -> sizes.put(f.path(), f.size())));
        long extract = selection.files().stream().mapToLong(f -> sizes.getOrDefault(f, 0L)).sum();
        if (!m.retentionOrEmpty().isEmpty()) {
            writeLn(info("Retention: %s", com.bloxbean.cardano.yaci.store.snapshot.manifest.Retention.format(m.retentionOrEmpty())));
        }
        long free = Files.getFileStore(options.workDir()).getUsableSpace();
        long database = loadedBytes(m, registry) * DATABASE_BYTES_PER_PARQUET_BYTE;
        writeLn(info("Disk space:"));
        String workLine = String.format("  Work directory : %s to extract, plus DuckDB spill; %s free at %s",
                SnapshotCliSupport.humanBytes(extract), SnapshotCliSupport.humanBytes(free), options.workDir());
        writeLn(free < extract ? warn(workLine.trim() + " (not enough to extract)") : workLine);
        writeLn("  PostgreSQL     : roughly %s for the restored database (about %dx the Parquet it loads); "
                        + "check the database volume, which this command cannot see",
                SnapshotCliSupport.humanBytes(database), DATABASE_BYTES_PER_PARQUET_BYTE);
    }

    /** What an import of {@code m} loads, skips and leaves to handlers, from the manifest's counts. */
    static List<String> describeImport(SnapshotManifest m, SnapshotSpecRegistry registry) {
        List<SnapshotManifest.TableManifest> load = new java.util.ArrayList<>();
        List<SnapshotManifest.TableManifest> skipped = new java.util.ArrayList<>();
        List<String> other = new java.util.ArrayList<>();
        for (SnapshotManifest.TableManifest t : m.tables()) {
            var spec = registry.byId(t.specId()).orElse(null);
            String restore = spec != null ? spec.restore().name() : t.restore();
            if (spec != null && !registry.isSelected(spec.id())) {
                skipped.add(t);
            } else if ("IMPORT".equals(restore)) {
                load.add(t);
            } else {
                other.add(t.targetTable() + " (" + restore.toLowerCase(java.util.Locale.ROOT).replace('_', '-') + ")");
            }
        }
        List<String> out = new java.util.ArrayList<>();
        out.add(tableGroup("Load", load));
        if (!skipped.isEmpty()) {
            out.add(tableGroup("Skip (store not enabled in the target)", skipped));
        }
        if (!other.isEmpty()) {
            out.add("Not loaded from Parquet: " + String.join(", ", other));
        }
        return out;
    }

    private static String tableGroup(String title, List<SnapshotManifest.TableManifest> tables) {
        StringBuilder sb = new StringBuilder(String.format("%s: %d table(s), %,d rows, %s", title, tables.size(),
                tables.stream().mapToLong(SnapshotManifest.TableManifest::rowCount).sum(),
                SnapshotCliSupport.humanBytes(tables.stream().mapToLong(SnapshotCommands::bytesOf).sum())));
        tables.stream().sorted(java.util.Comparator.comparingLong(SnapshotCommands::bytesOf).reversed()
                        .thenComparing(SnapshotManifest.TableManifest::targetTable))
                .forEach(t -> sb.append(String.format("%n      %-34s %,16d %10s", t.targetTable(), t.rowCount(),
                        SnapshotCliSupport.humanBytes(bytesOf(t)))));
        return sb.toString();
    }

    private static long bytesOf(SnapshotManifest.TableManifest t) {
        return t.files() == null ? 0 : t.files().stream().mapToLong(SnapshotManifest.FileEntry::size).sum();
    }

    private static long loadedBytes(SnapshotManifest m, SnapshotSpecRegistry registry) {
        return m.tables().stream()
                .filter(t -> registry.byId(t.specId()).filter(s -> registry.isSelected(s.id())
                        && s.restore() == com.bloxbean.cardano.yaci.store.snapshot.spec.RestoreMode.IMPORT).isPresent())
                .mapToLong(SnapshotCommands::bytesOf).sum();
    }

    /** The JDBC URL without its query string or user info, which may carry credentials. */
    static String redactedUrl(String url) {
        int q = url.indexOf('?');
        return (q < 0 ? url : url.substring(0, q)).replaceFirst("//[^/@]*@", "//");
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
        writeLn("  Keys/indexes : %s", SnapshotCliSupport.humanMillis(r.indexMillis()));
        writeLn("  Statistics   : %s", SnapshotCliSupport.humanMillis(r.analyzeMillis()));
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
        writeLn(info("Keys and required indexes were rebuilt. Optional indexes: run 'apply-indexes' after validation."));
    }
}
