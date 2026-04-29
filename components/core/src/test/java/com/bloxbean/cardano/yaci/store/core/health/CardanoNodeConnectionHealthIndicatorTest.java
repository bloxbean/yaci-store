package com.bloxbean.cardano.yaci.store.core.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
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
@DisplayName("CardanoNodeConnectionHealthIndicator")
class CardanoNodeConnectionHealthIndicatorTest {

    @Mock private HealthService healthService;

    @InjectMocks private CardanoNodeConnectionHealthIndicator indicator;

    private static HealthStatus status(boolean alive, boolean receivingBlocks, boolean error, boolean scheduledToStop) {
        return HealthStatus.builder()
                .isConnectionAlive(alive)
                .isReceivingBlocks(receivingBlocks)
                .isError(error)
                .isScheduleToStop(scheduledToStop)
                .build();
    }

    @Test
    void up_whenConnectedAndReceivingBlocks() {
        when(healthService.getHealthStatus()).thenReturn(status(true, true, false, false));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsEntry("connectionAlive", true)
                .containsEntry("receivingBlocks", true)
                .containsEntry("error", false)
                .containsEntry("syncStatus", "Connected");
    }

    @Test
    void down_whenConnectionLost() {
        when(healthService.getHealthStatus()).thenReturn(status(false, false, false, false));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Connection lost or sync error");
    }

    @Test
    void down_whenError() {
        when(healthService.getHealthStatus()).thenReturn(status(true, true, true, false));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Connection lost or sync error");
    }

    @Test
    void outOfService_whenConnectedButNotReceivingBlocks() {
        when(healthService.getHealthStatus()).thenReturn(status(true, false, false, false));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Not receiving blocks");
    }

    @Test
    void outOfService_whenScheduledToStop() {
        when(healthService.getHealthStatus()).thenReturn(status(true, true, false, true));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Scheduled to stop");
    }

    @Test
    void unknown_whenBlockFetcherUninitialised() {
        // HealthService throws NPE before block fetcher is wired up — handled gracefully.
        when(healthService.getHealthStatus()).thenThrow(new NullPointerException("no fetcher yet"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Block fetcher not initialized");
    }
}
