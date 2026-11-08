package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The cursor check and the tip lookup answer, but the live connection opened last accepts the connection and never
 * completes the handshake. No network.
 */
class StartServiceSessionTimeoutTest {
    private final Point from = new Point(1000, "aa");

    private BlockRangeSync blockRangeSync;
    private BlockSync blockSync;
    private BlockFetchService blockFetchService;
    private StoreProperties storeProperties;
    private StartService startService;
    private final CountDownLatch released = new CountDownLatch(1);

    @BeforeEach
    void setUp() {
        blockRangeSync = mock(BlockRangeSync.class);
        blockSync = mock(BlockSync.class);
        storeProperties = new StoreProperties(); // auto-restart is on by default
        blockFetchService = new BlockFetchService(mock(ApplicationEventPublisher.class), mock(MetricsService.class),
                blockRangeSync, blockSync, mock(CursorService.class), mock(EraService.class), storeProperties,
                mock(GenesisConfig.class), mock(ShelleyBlockEventPublisher.class), mock(ByronBlockEventPublisher.class),
                mock(com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage.class));

        CursorService cursorService = mock(CursorService.class);
        when(cursorService.getStartCursor()).thenReturn(Optional.of(new Cursor(from.getSlot(), from.getHash(), 10L, "bb", Era.Babbage)));
        TipFinderService tipFinderService = mock(TipFinderService.class);
        when(tipFinderService.findTip(anyLong())).thenReturn(new Tip(new Point(1_000_000, "cc"), 500));

        startService = new StartService(mock(ApplicationEventPublisher.class), blockFetchService, tipFinderService,
                mock(GenesisBlockFinder.class), cursorService, mock(InitService.class), storeProperties,
                mock(GenesisConfig.class));
        startService.sessionStartTimeoutMs = 300;
    }

    // Like yaci's handshake loop: sleep until interrupted. yaci then logs the failure and returns normally.
    private Object stallUntilInterrupted() {
        try {
            while (true) Thread.sleep(100);
        } catch (InterruptedException e) {
            released.countDown();
            return null;
        }
    }

    @Test
    void aStalledRangeFetchConnectionFailsTheStartInsteadOfHoldingIt() throws Exception {
        doAnswer(inv -> stallUntilInterrupted()).when(blockRangeSync).restart(any(BlockChainDataListener.class));

        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThatThrownBy(() -> startService.start()).isInstanceOf(NodeNotAnsweringException.class));

        assertThat(blockFetchService.isStarting()).isFalse();
        assertThat(startService.isStarted()).isFalse();
        // the stalled connection was closed and its thread released; the fetch was not sent on it
        verify(blockRangeSync, atLeastOnce()).stop();
        assertThat(released.await(5, TimeUnit.SECONDS)).isTrue();
        verify(blockRangeSync, never()).fetch(any(), any());

        // nothing is left holding the startup or the fetch service: a stop goes through
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> startService.stop());
    }

    @Test
    void aStalledSyncConnectionDuringARestartLeavesTheSyncFailedForTheNextRetry() throws Exception {
        storeProperties.setBlockDiffToStartSyncProtocol(Integer.MAX_VALUE); // near the tip: live chain sync
        doAnswer(inv -> stallUntilInterrupted()).when(blockSync).startSync(any(Point.class), any(BlockChainDataListener.class));

        long token = startService.stopForRestart(startService.getStopCount());
        clearInvocations(blockSync);
        assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
                assertThatThrownBy(() -> startService.startAfterRestart(token)).isInstanceOf(NodeNotAnsweringException.class));

        assertThat(blockFetchService.isStarting()).isFalse();
        assertThat(startService.isSyncFailed()).isTrue();
        verify(blockSync, atLeastOnce()).stop();
        assertThat(released.await(5, TimeUnit.SECONDS)).isTrue();
    }
}
