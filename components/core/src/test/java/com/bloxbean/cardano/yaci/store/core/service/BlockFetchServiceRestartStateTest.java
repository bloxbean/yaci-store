package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlockFetchServiceRestartStateTest {
    private BlockFetchService blockFetchService;
    private BlockRangeSync blockRangeSync;

    @BeforeEach
    void setUp() {
        blockRangeSync = mock(BlockRangeSync.class);
        blockFetchService = new BlockFetchService(
                mock(ApplicationEventPublisher.class),
                mock(MetricsService.class),
                blockRangeSync,
                mock(BlockSync.class),
                mock(CursorService.class),
                mock(EraService.class),
                mock(StoreProperties.class),
                mock(GenesisConfig.class),
                mock(ShelleyBlockEventPublisher.class),
                mock(ByronBlockEventPublisher.class));
    }

    @Test
    void failedRestartLooksLikeAnErrorNotADeliberateStop() {
        blockFetchService.stop();
        assertThat(blockFetchService.isScheduledToStop()).isTrue();

        blockFetchService.markRestartFailed();

        assertThat(blockFetchService.isScheduledToStop()).isFalse();
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void startingASessionRecordsItsStartTime() {
        assertThat(blockFetchService.getLastSessionStartTime()).isZero();

        long before = System.currentTimeMillis();
        blockFetchService.startSync(new com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point(1, "aa"));

        assertThat(blockFetchService.getLastSessionStartTime()).isGreaterThanOrEqualTo(before);
    }

    @Test
    void aClientThatWasNeverStartedIsNotRunning() {
        // yaci throws NullPointerException from these getters until the client has been started
        when(blockRangeSync.isRunning()).thenThrow(new NullPointerException("blockFetcher is null"));
        when(blockRangeSync.getLastKeepAliveResponseCookie()).thenThrow(new NullPointerException("blockFetcher is null"));
        when(blockRangeSync.getLastKeepAliveResponseTime()).thenThrow(new NullPointerException("blockFetcher is null"));

        assertThat(blockFetchService.isRunning()).isFalse();
        assertThat(blockFetchService.getLastKeepAliveResponseCookie()).isZero();
        assertThat(blockFetchService.getLastKeepAliveResponseTime()).isZero();
    }
}
