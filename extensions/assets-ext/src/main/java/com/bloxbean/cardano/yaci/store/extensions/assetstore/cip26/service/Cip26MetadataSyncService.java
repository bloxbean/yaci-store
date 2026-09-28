package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.Cip26NetworkDefaults;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.Cip26SyncState;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.ChangedMappings;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.MappingUpdateDetails;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.enums.SyncStatusEnum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.Cip26SyncStateRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class Cip26MetadataSyncService {

    private final GitService gitService;
    private final Cip26MetadataService tokenMetadataService;
    private final TokenMappingService tokenMappingService;
    private final Cip26SyncStateRepository syncStateRepository;
    private final Cip26NetworkDefaults networkDefaults;
    private final AssetsExtStoreProperties assetsStoreProperties;

    @Getter
    private SyncStatus syncStatus;

    @PostConstruct
    void initSyncStatus() {
        if (assetsStoreProperties.getCip26().isEnabled()) {
            syncStatus = new SyncStatus(false, SyncStatusEnum.SYNC_NOT_STARTED);
        } else {
            syncStatus = new SyncStatus(true, SyncStatusEnum.SYNC_DISABLED);
        }
    }

    public void synchronizeDatabase() {
        if (!networkDefaults.isRegistryAvailable()) {
            log.debug("CIP-26 sync skipped — no token registry available for this network");
            return;
        }

        syncStatus.setStatus(SyncStatusEnum.SYNC_IN_PROGRESS);

        Optional<Cip26SyncState> lastSyncState = syncStateRepository.findTopByOrderByIdDesc();
        String lastHash = lastSyncState
                .map(Cip26SyncState::getLastCommitHash).orElse(null);

        long syncStart = System.currentTimeMillis();
        log.info("Starting offchain sync. Last known commit: {}", lastHash != null ? lastHash : "(none — full sync)");

        long cloneStart = System.currentTimeMillis();
        Optional<Path> repoPathOpt = gitService.cloneCardanoTokenRegistryGitRepository();

        if (repoPathOpt.isPresent()) {

            log.info("Repository ready in {} ms", System.currentTimeMillis() - cloneStart);

            Optional<String> newHashOpt = gitService.getHeadCommitHash();
            if (newHashOpt.isEmpty()) {
                log.warn("Could not determine HEAD commit hash after cloning. Falling back to full sync without hash tracking.");
            }

            if (newHashOpt.isPresent() && newHashOpt.get().equals(lastHash)) {
                log.info("No new commits since last sync. Skipping processing.");
                syncStatus.setStatus(SyncStatusEnum.SYNC_DONE);
                syncStatus.setInitialSyncDone(true);
                return;
            }

            PendingChanges pendingChanges = resolvePendingChanges(lastHash, newHashOpt, repoPathOpt.get());
            List<File> filesToProcess = pendingChanges.filesToProcess();
            log.info("Resolved {} file(s) to process, {} subject(s) to delete",
                    filesToProcess.size(), pendingChanges.subjectsToDelete().size());

            // Batch-resolve git metadata for all files in a single history walk
            Set<String> fileNames = filesToProcess.stream()
                    .map(File::getName)
                    .collect(Collectors.toCollection(HashSet::new));
            long gitHistoryStart = System.currentTimeMillis();
            log.info("Resolving git history for {} file(s) in batch...", fileNames.size());
            Map<String, MappingUpdateDetails> mappingDetailsMap = gitService.getAllMappingDetails(fileNames);
            log.info("Git history resolved in {} ms", System.currentTimeMillis() - gitHistoryStart);

            long processStart = System.currentTimeMillis();
            boolean anyTransientFailure = processMappingFiles(filesToProcess, mappingDetailsMap);
            anyTransientFailure |= processDeletions(pendingChanges.subjectsToDelete());

            if (anyTransientFailure) {
                log.warn("At least one entry hit a transient failure. Commit hash will not be advanced so those entries are retried on next sync.");
            } else if (newHashOpt.isPresent()) {
                Cip26SyncState offChainSyncStateToSave = lastSyncState.orElse(new Cip26SyncState());
                offChainSyncStateToSave.setLastCommitHash(newHashOpt.get());
                offChainSyncStateToSave.setLastSyncedAt(LocalDateTime.now());
                syncStateRepository.save(offChainSyncStateToSave);
                log.info("Commit hash advanced to {}", newHashOpt.get());
            }

            log.info("Mapping processing took {} ms", System.currentTimeMillis() - processStart);

            syncStatus.setStatus(SyncStatusEnum.SYNC_DONE);
            syncStatus.setInitialSyncDone(true);
            log.info("Offchain sync complete in {} ms", System.currentTimeMillis() - syncStart);

        } else {
            log.warn("cardano-token-registry could not be cloned");
            syncStatus.setStatus(SyncStatusEnum.SYNC_ERROR);
        }

    }

    /**
     * Returns true iff at least one entry hit a transient (recoverable) failure
     * — those block the cursor advance so the next sync retries them.
     * Permanently-skipped entries (validation rejected, non-transient DB
     * errors) do NOT block: they're documented and we move on, otherwise the
     * sync would loop forever on bad data.
     */
    private boolean processMappingFiles(List<File> filesToProcess, Map<String, MappingUpdateDetails> mappingDetailsMap) {
        Tally tally = new Tally(filesToProcess.size());

        for (File mappingFile : filesToProcess) {
            FileOutcome outcome = processOneMappingFile(mappingFile, mappingDetailsMap);
            tally.record(outcome);
            tally.maybeLogProgress();
        }

        tally.logSummary();
        return tally.anyTransient;
    }

    /**
     * Process a single mapping file end-to-end and classify the result. Pulls the
     * skip/insert/error flow out of the main loop so each branch is a {@code return}
     * rather than a {@code continue}, which keeps the loop's cognitive complexity low.
     */
    private FileOutcome processOneMappingFile(File mappingFile,
                                              Map<String, MappingUpdateDetails> mappingDetailsMap) {
        Optional<Mapping> mapping = tokenMappingService.parseMappings(mappingFile);
        if (mapping.isEmpty()) {
            return FileOutcome.SKIPPED_NO_MAPPING;
        }

        // Filename-vs-inner-subject filter: in the upstream registries, the
        // canonical file for a token is named after its subject. ~90% of files
        // in the testnet registry have a filename that doesn't match the inner
        // `subject` field (typo / spam / orphaned). Indexing those would mean
        // last-write-wins on the same DB row by File.listFiles() order, which
        // is filesystem-dependent and silently picks arbitrary content. By
        // accepting only filename-matches-subject files we guarantee at most
        // one file per subject (filenames are unique) and full determinism.
        String filenameSubject = stripJsonExtension(mappingFile.getName());
        if (!filenameSubject.equals(mapping.get().subject())) {
            log.warn("Skipping '{}': filename does not match inner subject '{}'",
                    mappingFile.getName(), mapping.get().subject());
            return FileOutcome.SKIPPED_FILENAME_MISMATCH;
        }

        MappingUpdateDetails updateDetails = mappingDetailsMap.get(mappingFile.getName());
        if (updateDetails == null) {
            return FileOutcome.SKIPPED_NO_MAPPING;
        }

        try {
            return classifyInsertOutcome(mapping.get(), updateDetails);
        } catch (Exception e) {
            // Defensive: should not normally bubble up — the service classifies
            // its own exceptions. Treat anything that does as transient so it
            // retries; a real bug will keep showing up in logs.
            log.warn("Unexpected exception while processing token '{}': {}. Will retry next sync.",
                    mapping.get().subject(), e.getMessage());
            return FileOutcome.TRANSIENTLY_FAILED;
        }
    }

    private FileOutcome classifyInsertOutcome(Mapping mapping, MappingUpdateDetails updateDetails) {
        InsertOutcome metadataOutcome = tokenMetadataService.insertMapping(
                mapping, updateDetails.updatedAt(), updateDetails.updatedBy());

        return switch (metadataOutcome) {
            case INSERTED -> {
                // Logo may also be transient — fold its outcome into the same tally.
                InsertOutcome logoOutcome = tokenMetadataService.insertLogo(mapping);
                yield logoOutcome == InsertOutcome.TRANSIENTLY_FAILED
                        ? FileOutcome.INSERTED_LOGO_TRANSIENT
                        : FileOutcome.INSERTED;
            }
            case TRANSIENTLY_FAILED -> FileOutcome.TRANSIENTLY_FAILED;
            case PERMANENTLY_SKIPPED -> FileOutcome.PERMANENTLY_SKIPPED;
        };
    }

    /** Per-file outcome categories the tally tracks. */
    private enum FileOutcome {
        INSERTED,
        INSERTED_LOGO_TRANSIENT,
        PERMANENTLY_SKIPPED,
        TRANSIENTLY_FAILED,
        SKIPPED_NO_MAPPING,
        SKIPPED_FILENAME_MISMATCH
    }

    /** Mutable counter bag for the {@link #processMappingFiles} loop. */
    private final class Tally {
        private final int total;
        private int processed;
        private int inserted;
        private int permanentlySkipped;
        private int transientlyFailed;
        private int skippedNoMapping;
        private int skippedFilenameMismatch;
        private boolean anyTransient;

        Tally(int total) {
            this.total = total;
        }

        void record(FileOutcome outcome) {
            processed++;
            switch (outcome) {
                case INSERTED -> inserted++;
                case INSERTED_LOGO_TRANSIENT -> {
                    inserted++;
                    transientlyFailed++;
                    anyTransient = true;
                }
                case TRANSIENTLY_FAILED -> {
                    transientlyFailed++;
                    anyTransient = true;
                }
                case PERMANENTLY_SKIPPED -> permanentlySkipped++;
                case SKIPPED_NO_MAPPING -> skippedNoMapping++;
                case SKIPPED_FILENAME_MISMATCH -> skippedFilenameMismatch++;
            }
        }

        void maybeLogProgress() {
            if (processed % 500 == 0) {
                log.info("Processing mappings: {}/{} done " +
                                "(inserted={}, perm-skipped={}, transient={}, no-mapping={}, filename-mismatch={})",
                        processed, total, inserted, permanentlySkipped, transientlyFailed,
                        skippedNoMapping, skippedFilenameMismatch);
            }
        }

        void logSummary() {
            log.info("Mapping processing complete: {}/{} processed " +
                            "(inserted={}, perm-skipped={}, transient={}, no-mapping={}, filename-mismatch={}). " +
                            "Cursor will {} advance.",
                    processed, total, inserted, permanentlySkipped, transientlyFailed,
                    skippedNoMapping, skippedFilenameMismatch,
                    anyTransient ? "NOT" : "");
        }
    }

    private static String stripJsonExtension(String fileName) {
        return fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - ".json".length())
                : fileName;
    }

    private PendingChanges resolvePendingChanges(String lastHash, Optional<String> newHashOpt, Path repoPath) {
        if (lastHash != null && newHashOpt.isPresent()) {
            log.info("Incremental sync from {} to {}", lastHash, newHashOpt.get());
            ChangedMappings changedMappings = gitService.getChangedMappings(lastHash, newHashOpt.get());
            List<File> files = changedMappings.upsertedFiles().stream()
                    .map(Path::toFile).toList();
            List<String> subjectsToDelete = changedMappings.deletedFileNames().stream()
                    .map(Cip26MetadataSyncService::stripJsonExtension)
                    .toList();
            log.info("Incremental sync: processing {} changed file(s), {} deleted file(s)",
                    files.size(), subjectsToDelete.size());
            return new PendingChanges(files, subjectsToDelete);
        }

        log.info("Full sync: processing all files");
        File mappings = repoPath.toFile();
        List<File> files = Optional.ofNullable(mappings.listFiles())
                .map(Arrays::asList).orElse(List.of());
        return new PendingChanges(files, resolveStaleSubjects(files));
    }

    /**
     * Full-sync reconciliation: subjects present in the local DB whose mapping file no longer
     * exists in the registry were removed upstream (possibly while commit-hash tracking was
     * unavailable) and must be deleted locally.
     */
    private List<String> resolveStaleSubjects(List<File> presentFiles) {
        if (presentFiles.isEmpty()) {
            log.warn("Full sync found no mapping files. Skipping stale-subject cleanup as a safety measure.");
            return List.of();
        }
        Set<String> presentSubjects = new HashSet<>();
        for (File presentFile : presentFiles) {
            presentSubjects.add(stripJsonExtension(presentFile.getName()));
        }
        List<String> staleSubjects = tokenMetadataService.findAllSubjects().stream()
                .filter(subject -> !presentSubjects.contains(subject))
                .toList();
        if (!staleSubjects.isEmpty()) {
            log.info("Full sync: {} stale subject(s) no longer present in the registry will be deleted",
                    staleSubjects.size());
        }
        return staleSubjects;
    }

    /**
     * Returns true iff at least one deletion hit a transient failure — same
     * cursor semantics as {@link #processMappingFiles}: transient failures
     * block the cursor advance so the next sync retries them, while permanent
     * failures are logged and skipped past.
     */
    private boolean processDeletions(List<String> subjectsToDelete) {
        if (subjectsToDelete.isEmpty()) {
            return false;
        }
        boolean anyTransient = false;
        int deleted = 0;
        int permanentlyFailed = 0;
        for (String subject : subjectsToDelete) {
            switch (tokenMetadataService.deleteMapping(subject)) {
                case DELETED -> {
                    deleted++;
                    log.info("Deleted metadata for subject '{}' removed from the registry", subject);
                }
                case PERMANENTLY_FAILED -> permanentlyFailed++;
                case TRANSIENTLY_FAILED -> anyTransient = true;
            }
        }
        log.info("Deletion processing complete: {}/{} deleted (perm-failed={}). Cursor will {} advance.",
                deleted, subjectsToDelete.size(), permanentlyFailed, anyTransient ? "NOT" : "");
        return anyTransient;
    }

    /**
     * The database changes one sync run still has to apply: mapping files to upsert and
     * subjects to delete. How each side is resolved depends on the sync mode:
     *
     * <p><b>Incremental sync</b> (a last processed commit hash is stored and HEAD is known):
     * both sides come from the git tree diff between the two commits. Added/modified mapping
     * files become {@code filesToProcess}; deleted mapping files become {@code subjectsToDelete}
     * (filename minus the {@code .json} extension — for canonical registry entries the filename
     * equals the subject; for the mismatched spam files that were never indexed the resulting
     * delete is a harmless no-op).
     *
     * <p><b>Full sync</b> (first run, or commit-hash tracking unavailable): there is no diff to
     * consult, so {@code filesToProcess} is every file in the mappings folder and
     * {@code subjectsToDelete} is derived by reconciliation — DB subjects with no corresponding
     * mapping file were removed upstream while tracking was lost and must go. An empty mappings
     * folder is treated as a broken clone rather than "everything was deleted", and yields no
     * deletions.
     *
     * <p>Transiently-failed deletions, like transiently-failed upserts, prevent the commit hash
     * from advancing so the work is retried on the next run.
     *
     * @param filesToProcess   mapping files to parse and upsert into the metadata table
     * @param subjectsToDelete subjects whose metadata rows must be removed locally
     */
    private record PendingChanges(List<File> filesToProcess, List<String> subjectsToDelete) {
    }

}
