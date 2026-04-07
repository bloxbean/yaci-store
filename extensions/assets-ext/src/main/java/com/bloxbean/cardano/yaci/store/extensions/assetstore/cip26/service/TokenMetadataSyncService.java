package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsStoreProperties;
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
    private final AssetsStoreProperties assetsStoreProperties;

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
            boolean hasFailures = processMappingFiles(filesToProcess, mappingDetailsMap);

            if (hasFailures) {
                log.warn("Some mappings failed to process. Commit hash will not be advanced so failed mappings are retried on next sync.");
            } else if (newHashOpt.isPresent()) {
                OffChainSyncState offChainSyncStateToSave = lastSyncState.orElse(new OffChainSyncState());
                offChainSyncStateToSave.setLastCommitHash(newHashOpt.get());
                offChainSyncStateToSave.setLastSyncedAt(java.time.LocalDateTime.now());
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

    private boolean processMappingFiles(List<File> filesToProcess, Map<String, MappingUpdateDetails> mappingDetailsMap) {
        AtomicBoolean failures = new AtomicBoolean(false);
        int total = filesToProcess.size();
        int processed = 0;
        int inserted = 0;
        int skipped = 0;

        for (File mappingFile : filesToProcess) {
            processed++;
            Optional<Mapping> mapping = tokenMappingService.parseMappings(mappingFile);
            if (mapping.isEmpty()) {
                skipped++;
                continue;
            }

            MappingUpdateDetails updateDetails = mappingDetailsMap.get(mappingFile.getName());
            if (updateDetails == null) {
                skipped++;
                continue;
            }

            try {
                boolean metadataInserted = tokenMetadataService.insertMapping(
                        mapping.get(),
                        updateDetails.updatedAt(),
                        updateDetails.updatedBy());
                if (metadataInserted) {
                    tokenMetadataService.insertLogo(mapping.get());
                    inserted++;
                }
            } catch (Exception e) {
                failures.set(true);
                log.warn("Failed to process token '{}': {}. Continuing with next token.",
                        mapping.get().subject(), e.getMessage());
            }

            if (processed % 500 == 0) {
                log.info("Processing mappings: {}/{} done ({} inserted, {} skipped)",
                        processed, total, inserted, skipped);
            }
        }

        log.info("Mapping processing complete: {}/{} processed, {} inserted, {} skipped, failures={}",
                processed, total, inserted, skipped, failures.get());
        return failures.get();
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
