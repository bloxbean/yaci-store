package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.Cip26NetworkDefaults;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.OffChainSyncState;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.MappingUpdateDetails;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.enums.SyncStatusEnum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.SyncStateRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenMetadataSyncService {

    private final GitService gitService;
    private final TokenMetadataService tokenMetadataService;
    private final TokenMappingService tokenMappingService;
    private final SyncStateRepository syncStateRepository;
    private final Cip26NetworkDefaults networkDefaults;
    private final AssetsExtStoreProperties assetsStoreProperties;
    private final Clock clock;

    @Getter
    private SyncStatus syncStatus;

    @PostConstruct
    void initSyncStatus() {
        if (assetsStoreProperties.getCip26().isEnabled()) {
            syncStatus = new SyncStatus(false, SyncStatusEnum.SYNC_NOT_STARTED);
        } else {
            syncStatus = new SyncStatus(true, SyncStatusEnum.SYNC_IN_EXTRA_JOB);
        }
    }

    public void synchronizeDatabase() {
        if (!networkDefaults.isRegistryAvailable()) {
            log.debug("CIP-26 sync skipped — no token registry available for this network");
            return;
        }

        syncStatus.setStatus(SyncStatusEnum.SYNC_IN_PROGRESS);

        Optional<OffChainSyncState> lastSyncState = syncStateRepository.findTopByOrderByIdDesc();
        String lastHash = lastSyncState
                .map(OffChainSyncState::getLastCommitHash).orElse(null);

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

            List<File> filesToProcess = resolveFilesToProcess(lastHash, newHashOpt, repoPathOpt.get());
            log.info("Resolved {} file(s) to process", filesToProcess.size());

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

            if (anyTransientFailure) {
                log.warn("At least one entry hit a transient failure. Commit hash will not be advanced so those entries are retried on next sync.");
            } else if (newHashOpt.isPresent()) {
                OffChainSyncState offChainSyncStateToSave = lastSyncState.orElse(new OffChainSyncState());
                offChainSyncStateToSave.setLastCommitHash(newHashOpt.get());
                offChainSyncStateToSave.setLastSyncedAt(LocalDateTime.now(clock));
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
        AtomicBoolean anyTransient = new AtomicBoolean(false);
        int total = filesToProcess.size();
        int processed = 0;
        int inserted = 0;
        int permanentlySkipped = 0;
        int transientlyFailed = 0;
        int skippedNoMapping = 0;
        int skippedFilenameMismatch = 0;

        for (File mappingFile : filesToProcess) {
            processed++;
            Optional<Mapping> mapping = tokenMappingService.parseMappings(mappingFile);
            if (mapping.isEmpty()) {
                skippedNoMapping++;
                continue;
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
                skippedFilenameMismatch++;
                continue;
            }

            MappingUpdateDetails updateDetails = mappingDetailsMap.get(mappingFile.getName());
            if (updateDetails == null) {
                skippedNoMapping++;
                continue;
            }

            try {
                InsertOutcome metadataOutcome = tokenMetadataService.insertMapping(
                        mapping.get(),
                        updateDetails.updatedAt(),
                        updateDetails.updatedBy());

                switch (metadataOutcome) {
                    case INSERTED -> {
                        // Logo may also be transient — fold its outcome into the same tally.
                        InsertOutcome logoOutcome = tokenMetadataService.insertLogo(mapping.get());
                        if (logoOutcome == InsertOutcome.TRANSIENTLY_FAILED) {
                            anyTransient.set(true);
                            transientlyFailed++;
                        }
                        inserted++;
                    }
                    case TRANSIENTLY_FAILED -> {
                        anyTransient.set(true);
                        transientlyFailed++;
                    }
                    case PERMANENTLY_SKIPPED -> permanentlySkipped++;
                }
            } catch (Exception e) {
                // Defensive: should not normally bubble up — the service classifies
                // its own exceptions. Treat anything that does as transient so it
                // retries; a real bug will keep showing up in logs.
                anyTransient.set(true);
                transientlyFailed++;
                log.warn("Unexpected exception while processing token '{}': {}. Will retry next sync.",
                        mapping.get().subject(), e.getMessage());
            }

            if (processed % 500 == 0) {
                log.info("Processing mappings: {}/{} done " +
                                "(inserted={}, perm-skipped={}, transient={}, no-mapping={}, filename-mismatch={})",
                        processed, total, inserted, permanentlySkipped, transientlyFailed,
                        skippedNoMapping, skippedFilenameMismatch);
            }
        }

        log.info("Mapping processing complete: {}/{} processed " +
                        "(inserted={}, perm-skipped={}, transient={}, no-mapping={}, filename-mismatch={}). " +
                        "Cursor will {} advance.",
                processed, total, inserted, permanentlySkipped, transientlyFailed,
                skippedNoMapping, skippedFilenameMismatch,
                anyTransient.get() ? "NOT" : "");
        return anyTransient.get();
    }

    private static String stripJsonExtension(String fileName) {
        return fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - ".json".length())
                : fileName;
    }

    private List<File> resolveFilesToProcess(String lastHash, Optional<String> newHashOpt, Path repoPath) {
        if (lastHash != null && newHashOpt.isPresent()) {
            log.info("Incremental sync from {} to {}", lastHash, newHashOpt.get());
            List<File> files = gitService.getChangedFiles(lastHash, newHashOpt.get()).stream()
                    .map(Path::toFile).toList();
            log.info("Incremental sync: processing {} changed file(s)", files.size());
            return files;
        }

        log.info("Full sync: processing all files");
        File mappings = repoPath.toFile();
        return Optional.ofNullable(mappings.listFiles())
                .map(Arrays::asList).orElse(List.of());
    }

}
