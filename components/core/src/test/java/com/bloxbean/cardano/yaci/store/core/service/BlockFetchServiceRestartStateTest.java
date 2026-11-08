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
        StoreProperties storeProperties = mock(StoreProperties.class);
        // A started session runs a keep-alive thread. With the mock's interval of 0 it would spin and record
        // calls on the mock until the test JVM runs out of memory.
        when(storeProperties.getKeepAliveInterval()).thenReturn(60_000);
        blockFetchService = new BlockFetchService(
                mock(ApplicationEventPublisher.class),
                mock(MetricsService.class),
                blockRangeSync,
                mock(BlockSync.class),
                mock(CursorService.class),
                mock(EraService.class),
                storeProperties,
                mock(GenesisConfig.class),
                mock(ShelleyBlockEventPublisher.class),
                mock(ByronBlockEventPublisher.class),
                mock(com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage.class));
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
    void intersectionNotFoundMarksTheSessionFailed() {
        blockFetchService.intersactNotFound(new com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip(
                new com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point(9, "bb"), 2));

        // a dropped restart request is only tried again while the sync is in the error state
        assertThat(blockFetchService.isError()).isTrue();
        assertThat(blockFetchService.isScheduledToStop()).isFalse();
    }

    @Test
    void aTimedStartThatFailsClosesItsConnection() {
        // The start can also finish just after the time limit, when NodeCalls no longer runs the cancel. Either way
        // the caller treats the start as failed, so the connection must not stay open.
        org.mockito.Mockito.doThrow(new IllegalStateException("connection refused"))
                .when(blockRangeSync).restart(org.mockito.ArgumentMatchers.any());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> blockFetchService.startFetchWithTimeout(
                new com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point(1, "aa"),
                new com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point(9, "bb"), null, 5_000));

        org.mockito.Mockito.verify(blockRangeSync).stop();
        org.mockito.Mockito.verify(blockRangeSync, org.mockito.Mockito.never())
                .fetch(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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
