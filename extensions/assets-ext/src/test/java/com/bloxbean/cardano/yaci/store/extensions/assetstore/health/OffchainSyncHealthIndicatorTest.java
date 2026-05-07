package com.bloxbean.cardano.yaci.store.extensions.assetstore.health;

import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.model.enums.SyncStatusEnum;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service.Cip26MetadataSyncService;
import com.bloxbean.cardano.yaci.store.extensions.assetstore.cip26.service.SyncStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Pinned mapping from {@link SyncStatusEnum} to Spring Boot {@link Health}.
 *
 * <p>Operators rely on this output for probe-group composition (startup / liveness / readiness),
 * so the enum-to-status mapping is part of the operational contract — not an implementation
 * detail. These tests prevent silent regressions.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OffchainSyncHealthIndicator")
class OffchainSyncHealthIndicatorTest {

    @Mock
    private Cip26MetadataSyncService syncService;

    @InjectMocks
    private OffchainSyncHealthIndicator indicator;

    @BeforeEach
    void resetMocks() {
        // Mockito @Mock + @InjectMocks: nothing extra needed.
    }

    @Test
    void unknownWhenSyncStatusIsNull() {
        // Indicator is registered as soon as Cip26MetadataSyncCronJob is on the context, but
        // the sync service's @PostConstruct may not have run yet for the very first probe call.
        // We surface "Not initialized" rather than NPE'ing.
        when(syncService.getSyncStatus()).thenReturn(null);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Not initialized");
    }

    @Test
    void upOnSyncDone() {
        when(syncService.getSyncStatus()).thenReturn(SyncStatus.builder().isInitialSyncDone(true).status(SyncStatusEnum.SYNC_DONE).build());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Sync done");
    }

    @Test
    void upOnSyncInExtraJob() {
        // External sync job mode: this indicator reports UP because *something else* is keeping
        // the registry data fresh; a yaci-internal "not started" would be misleading.
        when(syncService.getSyncStatus())
                .thenReturn(SyncStatus.builder().isInitialSyncDone(true).status(SyncStatusEnum.SYNC_IN_EXTRA_JOB).build());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Sync will be done in a different job. Status unkown");
    }

    @Test
    void outOfServiceOnSyncInProgress() {
        // Mid-sync — the registry data may be partially stale. Useful to gate readiness probes
        // so traffic isn't routed to a process that's still rebuilding its in-memory caches.
        when(syncService.getSyncStatus())
                .thenReturn(SyncStatus.builder().isInitialSyncDone(false).status(SyncStatusEnum.SYNC_IN_PROGRESS).build());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Sync in progress");
    }

    @Test
    void outOfServiceOnSyncNotStarted() {
        // Pre-first-sync state. Same readiness story as in-progress.
        when(syncService.getSyncStatus())
                .thenReturn(SyncStatus.builder().isInitialSyncDone(false).status(SyncStatusEnum.SYNC_NOT_STARTED).build());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Sync not started");
    }

    @Test
    void downOnSyncError() {
        // Hard failure: most recent sync ended in an exception. DOWN so liveness/alerting picks
        // it up (some probe groups treat OUT_OF_SERVICE as "wait it out" while DOWN is alertable).
        when(syncService.getSyncStatus()).thenReturn(SyncStatus.builder().isInitialSyncDone(false).status(SyncStatusEnum.SYNC_ERROR).build());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Error while syncing");
    }
}
