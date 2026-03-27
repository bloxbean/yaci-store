package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.Cip26NetworkDefaults;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.entity.OffChainSyncState;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.MappingDetails;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.MappingUpdateDetails;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.enums.SyncStatusEnum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.repository.SyncStateRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenMetadataSyncService {

    private final GitService gitService;
    private final TokenMetadataService tokenMetadataService;
    private final TokenMappingService tokenMappingService;
    private final SyncStateRepository syncStateRepository;
    private final Cip26NetworkDefaults networkDefaults;

    @Value("${store.extensions.asset-store.cip26.sync-enabled:false}")
    boolean isSyncEnabled;

    @Getter
    private SyncStatus syncStatus;

    @PostConstruct
    void initSyncStatus() {
        if (isSyncEnabled) {
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

        Optional<Path> repoPathOpt = gitService.cloneCardanoTokenRegistryGitRepository();

        if (repoPathOpt.isPresent()) {

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

            boolean hasFailures = processMappingFiles(filesToProcess);

            if (hasFailures) {
                log.warn("Some mappings failed to process. Commit hash will not be advanced so failed mappings are retried on next sync.");
            } else if (newHashOpt.isPresent()) {
                OffChainSyncState offChainSyncStateToSave = lastSyncState.orElse(new OffChainSyncState());
                offChainSyncStateToSave.setLastCommitHash(newHashOpt.get());
                syncStateRepository.save(offChainSyncStateToSave);
            }

            syncStatus.setStatus(SyncStatusEnum.SYNC_DONE);
            syncStatus.setInitialSyncDone(true);

        } else {
            log.warn("cardano-token-registry could not be cloned");
            syncStatus.setStatus(SyncStatusEnum.SYNC_ERROR);
        }

    }

    private boolean processMappingFiles(List<File> filesToProcess) {
        AtomicBoolean failures = new AtomicBoolean(false);

        filesToProcess.stream()
                .flatMap(this::toMappingDetails)
                .forEach(mappingDetails -> {
                    try {
                        boolean metadataInserted = tokenMetadataService.insertMapping(
                                mappingDetails.mapping(),
                                mappingDetails.mappingUpdateDetails().updatedAt(),
                                mappingDetails.mappingUpdateDetails().updatedBy());
                        if (metadataInserted) {
                            tokenMetadataService.insertLogo(mappingDetails.mapping());
                        }
                    } catch (Exception e) {
                        failures.set(true);
                        log.warn("Failed to process token '{}': {}. Continuing with next token.",
                                mappingDetails.mapping().subject(), e.getMessage());
                    }
                });

        return failures.get();
    }

    private Stream<MappingDetails> toMappingDetails(File mappingFile) {
        Optional<Mapping> mapping = tokenMappingService.parseMappings(mappingFile);
        Optional<MappingUpdateDetails> updateDetails = gitService.getMappingDetails(mappingFile);

        if (mapping.isPresent() && updateDetails.isPresent()) {
            return Stream.of(new MappingDetails(mapping.get(), updateDetails.get()));
        }
        return Stream.empty();
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
