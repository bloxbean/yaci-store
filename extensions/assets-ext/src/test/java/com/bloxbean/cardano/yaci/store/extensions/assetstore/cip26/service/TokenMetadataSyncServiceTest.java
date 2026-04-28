package com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.AssetsExtStoreProperties;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.Cip26NetworkDefaults;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.Mapping;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.MappingUpdateDetails;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.enums.SyncStatusEnum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.model.OffChainSyncState;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.storage.impl.repository.SyncStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenMetadataSyncService")
class TokenMetadataSyncServiceTest {

    private static final String OLD_HASH = "aaaa000000000000000000000000000000000000";
    private static final String NEW_HASH = "bbbb000000000000000000000000000000000000";

    @Mock private GitService gitService;
    @Mock private TokenMetadataService tokenMetadataService;
    @Mock private TokenMappingService tokenMappingService;
    @Mock private SyncStateRepository syncStateRepository;
    @Mock private Cip26NetworkDefaults networkDefaults;

    private AssetsExtStoreProperties assetsStoreProperties;
    private TokenMetadataSyncService service;

    @BeforeEach
    void setUp() {
        assetsStoreProperties = new AssetsExtStoreProperties();
        service = new TokenMetadataSyncService(
                gitService, tokenMetadataService, tokenMappingService,
                syncStateRepository, networkDefaults, assetsStoreProperties, Clock.systemDefaultZone());
    }

    @Nested
    @DisplayName("initSyncStatus")
    class InitSyncStatus {

        @Test
        void setsNotStartedWhenCip26Enabled() {
            assetsStoreProperties.getCip26().setEnabled(true);
            service.initSyncStatus();

            assertThat(service.getSyncStatus().getStatus()).isEqualTo(SyncStatusEnum.SYNC_NOT_STARTED);
            assertThat(service.getSyncStatus().isInitialSyncDone()).isFalse();
        }

        @Test
        void setsExtraJobWhenCip26Disabled() {
            assetsStoreProperties.getCip26().setEnabled(false);
            service.initSyncStatus();

            assertThat(service.getSyncStatus().getStatus()).isEqualTo(SyncStatusEnum.SYNC_IN_EXTRA_JOB);
            assertThat(service.getSyncStatus().isInitialSyncDone()).isTrue();
        }
    }

    @Nested
    @DisplayName("synchronizeDatabase — skipped")
    class SyncSkipped {

        @BeforeEach
        void initStatus() {
            assetsStoreProperties.getCip26().setEnabled(true);
            service.initSyncStatus();
        }

        @Test
        void skipsWhenRegistryNotAvailable() {
            when(networkDefaults.isRegistryAvailable()).thenReturn(false);

            service.synchronizeDatabase();

            verifyNoInteractions(gitService);
        }

        @Test
        void skipsProcessingWhenNoNewCommits() {
            when(networkDefaults.isRegistryAvailable()).thenReturn(true);
            when(syncStateRepository.findTopByOrderByIdDesc())
                    .thenReturn(Optional.of(offChainState(OLD_HASH)));
            when(gitService.cloneCardanoTokenRegistryGitRepository())
                    .thenReturn(Optional.of(Path.of("/tmp/repo")));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(OLD_HASH));

            service.synchronizeDatabase();

            verify(gitService, never()).getChangedFiles(any(), any());
            assertThat(service.getSyncStatus().getStatus()).isEqualTo(SyncStatusEnum.SYNC_DONE);
        }

        @Test
        void setsErrorWhenCloneFails() {
            when(networkDefaults.isRegistryAvailable()).thenReturn(true);
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
            when(gitService.cloneCardanoTokenRegistryGitRepository()).thenReturn(Optional.empty());

            service.synchronizeDatabase();

            assertThat(service.getSyncStatus().getStatus()).isEqualTo(SyncStatusEnum.SYNC_ERROR);
        }
    }

    @Nested
    @DisplayName("synchronizeDatabase — incremental sync")
    class IncrementalSync {

        @BeforeEach
        void initStatus() {
            assetsStoreProperties.getCip26().setEnabled(true);
            service.initSyncStatus();
            when(networkDefaults.isRegistryAvailable()).thenReturn(true);
        }

        @Test
        void processesChangedFilesAndAdvancesHash() {
            when(syncStateRepository.findTopByOrderByIdDesc())
                    .thenReturn(Optional.of(offChainState(OLD_HASH)));
            when(gitService.cloneCardanoTokenRegistryGitRepository())
                    .thenReturn(Optional.of(Path.of("/tmp/repo")));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(NEW_HASH));

            when(gitService.getChangedFiles(OLD_HASH, NEW_HASH))
                    .thenReturn(List.of(Path.of("/tmp/repo/mappings/test.json")));

            Mapping mockMapping = new Mapping("test-subject", null, null, null, null, null, null, null);
            when(tokenMappingService.parseMappings(any())).thenReturn(Optional.of(mockMapping));
            when(gitService.getAllMappingDetails(any()))
                    .thenReturn(Map.of("test.json", new MappingUpdateDetails("author@test.com", LocalDateTime.now())));
            when(tokenMetadataService.insertMapping(any(), any(), any())).thenReturn(true);

            service.synchronizeDatabase();

            verify(tokenMetadataService).insertMapping(any(), any(), any());
            verify(tokenMetadataService).insertLogo(any());
            verify(syncStateRepository).save(any(OffChainSyncState.class));
            assertThat(service.getSyncStatus().getStatus()).isEqualTo(SyncStatusEnum.SYNC_DONE);
            assertThat(service.getSyncStatus().isInitialSyncDone()).isTrue();
        }

        @Test
        void doesNotAdvanceHashWhenProcessingFails() {
            when(syncStateRepository.findTopByOrderByIdDesc())
                    .thenReturn(Optional.of(offChainState(OLD_HASH)));
            when(gitService.cloneCardanoTokenRegistryGitRepository())
                    .thenReturn(Optional.of(Path.of("/tmp/repo")));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(NEW_HASH));

            when(gitService.getChangedFiles(OLD_HASH, NEW_HASH))
                    .thenReturn(List.of(Path.of("/tmp/repo/mappings/fail.json")));

            Mapping mockMapping = new Mapping("fail-subject", null, null, null, null, null, null, null);
            when(tokenMappingService.parseMappings(any())).thenReturn(Optional.of(mockMapping));
            when(gitService.getAllMappingDetails(any()))
                    .thenReturn(Map.of("fail.json", new MappingUpdateDetails("author@test.com", LocalDateTime.now())));
            when(tokenMetadataService.insertMapping(any(), any(), any()))
                    .thenThrow(new RuntimeException("DB error"));

            service.synchronizeDatabase();

            verify(syncStateRepository, never()).save(any(OffChainSyncState.class));
            assertThat(service.getSyncStatus().getStatus()).isEqualTo(SyncStatusEnum.SYNC_DONE);
        }
    }

    @Nested
    @DisplayName("synchronizeDatabase — full sync")
    class FullSync {

        @BeforeEach
        void initStatus() {
            assetsStoreProperties.getCip26().setEnabled(true);
            service.initSyncStatus();
            when(networkDefaults.isRegistryAvailable()).thenReturn(true);
        }

        @Test
        void runsFullSyncWhenNoLastHash() {
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
            when(gitService.cloneCardanoTokenRegistryGitRepository())
                    .thenReturn(Optional.of(Path.of("/tmp/repo")));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(NEW_HASH));
            when(gitService.getAllMappingDetails(any())).thenReturn(Map.of());

            service.synchronizeDatabase();

            // Full sync doesn't call getChangedFiles — uses listFiles instead
            verify(gitService, never()).getChangedFiles(any(), any());
            assertThat(service.getSyncStatus().getStatus()).isEqualTo(SyncStatusEnum.SYNC_DONE);
        }
    }

    @Nested
    @DisplayName("processMappingFiles — edge cases")
    class ProcessMappingFiles {

        @TempDir
        Path repoDir;

        @BeforeEach
        void initStatus() {
            assetsStoreProperties.getCip26().setEnabled(true);
            service.initSyncStatus();
            when(networkDefaults.isRegistryAvailable()).thenReturn(true);
            when(syncStateRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
            when(gitService.cloneCardanoTokenRegistryGitRepository())
                    .thenReturn(Optional.of(repoDir));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(NEW_HASH));
        }

        @Test
        void skipsFileWhenMappingParsingFails() throws IOException {
            Files.writeString(repoDir.resolve("bad.json"), "{}");
            when(gitService.getAllMappingDetails(any())).thenReturn(
                    Map.of("bad.json", new MappingUpdateDetails("a@test.com", LocalDateTime.now())));
            when(tokenMappingService.parseMappings(any())).thenReturn(Optional.empty());

            service.synchronizeDatabase();

            verifyNoInteractions(tokenMetadataService);
            // Hash still advanced — parsing failures are skipped, not errors
            verify(syncStateRepository).save(any(OffChainSyncState.class));
        }

        @Test
        void skipsFileWhenGitHistoryMissing() throws IOException {
            Files.writeString(repoDir.resolve("any.json"), "{}");
            when(gitService.getAllMappingDetails(any())).thenReturn(Map.of()); // no history resolved

            Mapping mapping = new Mapping("subject", null, null, null, null, null, null, null);
            when(tokenMappingService.parseMappings(any())).thenReturn(Optional.of(mapping));

            service.synchronizeDatabase();

            verifyNoInteractions(tokenMetadataService);
            verify(syncStateRepository).save(any(OffChainSyncState.class));
        }

        @Test
        void skipsLogoInsertWhenMetadataNotInserted() throws IOException {
            Files.writeString(repoDir.resolve("exists.json"), "{}");
            when(gitService.getAllMappingDetails(any())).thenReturn(
                    Map.of("exists.json", new MappingUpdateDetails("a@test.com", LocalDateTime.now())));

            Mapping mapping = new Mapping("subject", null, null, null, null, null, null, null);
            when(tokenMappingService.parseMappings(any())).thenReturn(Optional.of(mapping));
            when(tokenMetadataService.insertMapping(any(), any(), any())).thenReturn(false);

            service.synchronizeDatabase();

            verify(tokenMetadataService).insertMapping(any(), any(), any());
            verify(tokenMetadataService, never()).insertLogo(any());
        }
    }

    private static OffChainSyncState offChainState(String hash) {
        OffChainSyncState state = new OffChainSyncState();
        state.setLastCommitHash(hash);
        return state;
    }
}
