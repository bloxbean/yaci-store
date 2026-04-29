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
import static org.mockito.ArgumentMatchers.argThat;
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

            // filename ("test") MUST equal inner subject — the sync now skips mismatches deterministically.
            Mapping mockMapping = new Mapping("test", null, null, null, null, null, null, null);
            when(tokenMappingService.parseMappings(any())).thenReturn(Optional.of(mockMapping));
            when(gitService.getAllMappingDetails(any()))
                    .thenReturn(Map.of("test.json", new MappingUpdateDetails("author@test.com", LocalDateTime.now())));
            when(tokenMetadataService.insertMapping(any(), any(), any()))
                    .thenReturn(InsertOutcome.INSERTED);
            when(tokenMetadataService.insertLogo(any())).thenReturn(InsertOutcome.INSERTED);

            service.synchronizeDatabase();

            verify(tokenMetadataService).insertMapping(any(), any(), any());
            verify(tokenMetadataService).insertLogo(any());
            verify(syncStateRepository).save(any(OffChainSyncState.class));
            assertThat(service.getSyncStatus().getStatus()).isEqualTo(SyncStatusEnum.SYNC_DONE);
            assertThat(service.getSyncStatus().isInitialSyncDone()).isTrue();
        }

        @Test
        void doesNotAdvanceHashOnTransientFailure() {
            // Transient outcomes block the cursor advance so the next sync retries.
            when(syncStateRepository.findTopByOrderByIdDesc())
                    .thenReturn(Optional.of(offChainState(OLD_HASH)));
            when(gitService.cloneCardanoTokenRegistryGitRepository())
                    .thenReturn(Optional.of(Path.of("/tmp/repo")));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(NEW_HASH));

            when(gitService.getChangedFiles(OLD_HASH, NEW_HASH))
                    .thenReturn(List.of(Path.of("/tmp/repo/mappings/fail.json")));

            Mapping mockMapping = new Mapping("fail", null, null, null, null, null, null, null);
            when(tokenMappingService.parseMappings(any())).thenReturn(Optional.of(mockMapping));
            when(gitService.getAllMappingDetails(any()))
                    .thenReturn(Map.of("fail.json", new MappingUpdateDetails("author@test.com", LocalDateTime.now())));
            when(tokenMetadataService.insertMapping(any(), any(), any()))
                    .thenReturn(InsertOutcome.TRANSIENTLY_FAILED);

            service.synchronizeDatabase();

            verify(syncStateRepository, never()).save(any(OffChainSyncState.class));
            assertThat(service.getSyncStatus().getStatus()).isEqualTo(SyncStatusEnum.SYNC_DONE);
        }

        @Test
        void advancesHashWhenAllFailuresArePermanent() {
            // Permanent skips (validation rejected, non-transient DB errors) must
            // NOT block the cursor — otherwise the sync loops forever on bad data.
            when(syncStateRepository.findTopByOrderByIdDesc())
                    .thenReturn(Optional.of(offChainState(OLD_HASH)));
            when(gitService.cloneCardanoTokenRegistryGitRepository())
                    .thenReturn(Optional.of(Path.of("/tmp/repo")));
            when(gitService.getHeadCommitHash()).thenReturn(Optional.of(NEW_HASH));

            when(gitService.getChangedFiles(OLD_HASH, NEW_HASH))
                    .thenReturn(List.of(Path.of("/tmp/repo/mappings/perm.json")));

            Mapping mockMapping = new Mapping("perm", null, null, null, null, null, null, null);
            when(tokenMappingService.parseMappings(any())).thenReturn(Optional.of(mockMapping));
            when(gitService.getAllMappingDetails(any()))
                    .thenReturn(Map.of("perm.json", new MappingUpdateDetails("author@test.com", LocalDateTime.now())));
            when(tokenMetadataService.insertMapping(any(), any(), any()))
                    .thenReturn(InsertOutcome.PERMANENTLY_SKIPPED);

            service.synchronizeDatabase();

            verify(syncStateRepository).save(any(OffChainSyncState.class));
            verify(tokenMetadataService, never()).insertLogo(any());
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

            // filename ("any") matches the inner subject so the new filename-vs-subject
            // filter doesn't short-circuit before we hit the missing-history branch.
            Mapping mapping = new Mapping("any", null, null, null, null, null, null, null);
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

            Mapping mapping = new Mapping("exists", null, null, null, null, null, null, null);
            when(tokenMappingService.parseMappings(any())).thenReturn(Optional.of(mapping));
            when(tokenMetadataService.insertMapping(any(), any(), any()))
                    .thenReturn(InsertOutcome.PERMANENTLY_SKIPPED);

            service.synchronizeDatabase();

            verify(tokenMetadataService).insertMapping(any(), any(), any());
            verify(tokenMetadataService, never()).insertLogo(any());
        }

        @Test
        void skipsFilesWhereFilenameDoesNotMatchInnerSubject() throws IOException {
            // Real-world QA discovery: the testnet registry has many files whose
            // filename doesn't equal the inner `subject` field (typos, spam,
            // legacy entries). Indexing them all would mean last-write-wins by
            // File.listFiles() order and silently swap a token's content. We
            // skip mismatches so each subject maps to exactly one canonical file.
            Files.writeString(repoDir.resolve("legit.json"), "{}");
            Files.writeString(repoDir.resolve("garbage.json"), "{}");
            when(gitService.getAllMappingDetails(any())).thenReturn(Map.of(
                    "legit.json", new MappingUpdateDetails("a@test.com", LocalDateTime.now()),
                    "garbage.json", new MappingUpdateDetails("a@test.com", LocalDateTime.now())));

            // legit.json's inner subject matches the filename → accepted.
            // garbage.json's inner subject claims to be "legit" too → skipped.
            when(tokenMappingService.parseMappings(argThat(f -> f != null && f.getName().equals("legit.json"))))
                    .thenReturn(Optional.of(new Mapping("legit", null, null, null, null, null, null, null)));
            when(tokenMappingService.parseMappings(argThat(f -> f != null && f.getName().equals("garbage.json"))))
                    .thenReturn(Optional.of(new Mapping("legit", null, null, null, null, null, null, null)));
            when(tokenMetadataService.insertMapping(any(), any(), any()))
                    .thenReturn(InsertOutcome.INSERTED);
            when(tokenMetadataService.insertLogo(any())).thenReturn(InsertOutcome.INSERTED);

            service.synchronizeDatabase();

            // Only the legit file's mapping made it through to insertMapping.
            verify(tokenMetadataService, times(1)).insertMapping(any(), any(), any());
            verify(syncStateRepository).save(any(OffChainSyncState.class));
        }
    }

    private static OffChainSyncState offChainState(String hash) {
        OffChainSyncState state = new OffChainSyncState();
        state.setLastCommitHash(hash);
        return state;
    }
}
