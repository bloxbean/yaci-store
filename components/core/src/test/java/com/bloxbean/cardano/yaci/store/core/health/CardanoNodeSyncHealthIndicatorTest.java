package com.bloxbean.cardano.yaci.store.core.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.common.domain.SyncStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import com.bloxbean.cardano.yaci.store.core.service.SyncStatusService;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("CardanoNodeSyncHealthIndicator")
class CardanoNodeSyncHealthIndicatorTest {

    @Mock private HealthService healthService;
    @Mock private SyncStatusService syncStatusService;

    @InjectMocks private CardanoNodeSyncHealthIndicator indicator;

    private static HealthStatus healthStatus(boolean alive, boolean receivingBlocks, boolean error, boolean scheduledToStop) {
        return HealthStatus.builder()
                .isConnectionAlive(alive)
                .isReceivingBlocks(receivingBlocks)
                .isError(error)
                .isScheduleToStop(scheduledToStop)
                .timeSinceLastBlock(1234L)
                .build();
    }

    private static SyncStatus syncStatus(double percentage, boolean synced) {
        return SyncStatus.builder().syncPercentage(percentage).synced(synced).build();
    }

    @Test
    void up_whenFullySynced() {
        when(healthService.getHealthStatus()).thenReturn(healthStatus(true, true, false, false));
        when(syncStatusService.getSyncStatus()).thenReturn(syncStatus(99.99, true));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("connectionAlive", true)
                .containsEntry("receivingBlocks", true)
                .containsEntry("syncStatus", "Synced")
                .containsEntry("syncPercentage", "99.99%");
    }

    @Test
    void outOfService_whileSyncing() {
        when(healthService.getHealthStatus()).thenReturn(healthStatus(true, true, false, false));
        when(syncStatusService.getSyncStatus()).thenReturn(syncStatus(42.5, false));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails())
                .containsEntry("syncStatus", "Syncing")
                .containsEntry("syncPercentage", "42.50%");
    }

    @Test
    void outOfService_whenScheduledToStop() {
        // Scheduled-to-stop is checked before sync progress — even a fully-synced
        // node reports OUT_OF_SERVICE if it's about to shut down.
        when(healthService.getHealthStatus()).thenReturn(healthStatus(true, true, false, true));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Scheduled to stop");
    }

    @Test
    void down_whenConnectionLostOrError() {
        when(healthService.getHealthStatus()).thenReturn(healthStatus(false, false, false, false));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Connection lost or sync error");
    }

    @Test
    void outOfService_whenNotReceivingBlocks() {
        when(healthService.getHealthStatus()).thenReturn(healthStatus(true, false, false, false));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Not receiving blocks");
    }

    @Test
    void unknown_whenBlockFetcherUninitialised() {
        when(healthService.getHealthStatus()).thenThrow(new NullPointerException("no fetcher yet"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Block fetcher not initialized");
    }
}
