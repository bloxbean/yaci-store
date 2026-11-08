package com.bloxbean.cardano.yaci.store.core.processor;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.service.BlockFetchService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import com.bloxbean.cardano.yaci.store.core.service.InitService;
import com.bloxbean.cardano.yaci.store.core.service.StartService;
import com.bloxbean.cardano.yaci.store.core.service.TipFinderService;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.events.internal.RequiredSyncRestartEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The restart processor with the real start and fetch services, and no health check (the admin health check is
 * optional and off by default). The node is mocked.
 */
class RequiredRestartProcessorRecoveryTest {
    private BlockRangeSync blockRangeSync;
    private BlockSync blockSync;
    private ApplicationEventPublisher fetchPublisher;
    private BlockFetchService blockFetchService;
    private CursorService cursorService;
    private StoreProperties storeProperties;
    private StartService startService;
    private RequiredRestartProcessor processor;

    @BeforeEach
    void setUp() {
        blockRangeSync = mock(BlockRangeSync.class);
        blockSync = mock(BlockSync.class);
        fetchPublisher = mock(ApplicationEventPublisher.class);
        storeProperties = new StoreProperties();
        storeProperties.setAutoRestartBackoffBaseMs(0);
        blockFetchService = new BlockFetchService(fetchPublisher, mock(MetricsService.class), blockRangeSync, blockSync,
                mock(CursorService.class), mock(EraService.class), storeProperties, mock(GenesisConfig.class),
                mock(ShelleyBlockEventPublisher.class), mock(ByronBlockEventPublisher.class));
        cursorService = mock(CursorService.class);
        // Stop the restarted start early; the test only needs to see that a start was attempted
        doThrow(new IllegalStateException("start attempted")).when(cursorService).getStartCursor();

        startService = new StartService(mock(ApplicationEventPublisher.class), blockFetchService,
                mock(TipFinderService.class), mock(GenesisBlockFinder.class), cursorService, mock(InitService.class),
                storeProperties, mock(GenesisConfig.class));
        processor = new RequiredRestartProcessor(startService, storeProperties);
        processor.shutdownWaitMs = 0;
    }

    @AfterEach
    void tearDown() {
        // ends the follow-up checks still pending after a test
        storeProperties.setAutoRestartEnabled(false);
    }

    @Test
    void intersectionNotFoundRightAfterARestartIsNotLost() throws Exception {
        storeProperties.setAutoRestartDebounceWindowMs(200);
        processor.retryCheckDelayMs = 400;
        // A restart has just happened: the next request is inside the debounce window and is dropped
        processor.evaluateRestartAttempt(System.currentTimeMillis(), RequiredSyncRestartEvent.builder().reason("earlier").build());
        // The new session's live connection is open
        blockFetchService.startSync(new Point(1000, "aa"));

        // ... and the node answers that it does not have the start point
        blockFetchService.intersactNotFound(new Tip(new Point(5000, "cc"), 50));
        ArgumentCaptor<Object> event = ArgumentCaptor.forClass(Object.class);
        verify(fetchPublisher).publishEvent(event.capture());
        assertThat(event.getValue()).isInstanceOf(RequiredSyncRestartEvent.class);

        var startAttempted = new CountDownLatch(1);
        doAnswer(inv -> {
            startAttempted.countDown();
            throw new IllegalStateException("start attempted");
        }).when(cursorService).getStartCursor();
        processor.handleRestartInterval((RequiredSyncRestartEvent) event.getValue());

        // The dropped request is tried again once the debounce window has passed
        assertThat(startAttempted.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void aDeliberateStopDuringTheBackoffIsKept() throws Exception {
        storeProperties.setAutoRestartDebounceWindowMs(0);
        storeProperties.setAutoRestartBackoffBaseMs(1000);
        blockFetchService.startSync(new Point(1000, "aa"));

        Thread restart = new Thread(() -> processor.handleRestartInterval(
                RequiredSyncRestartEvent.builder().reason("test").build()));
        restart.start();
        Thread.sleep(200); // inside the 1 s backoff
        startService.stop(); // e.g. the admin API or a balance snapshot
        restart.join(10_000);

        assertThat(restart.isAlive()).isFalse();
        verify(cursorService, never()).getStartCursor();
        // stopped once by the admin, not again by the restart
        verify(blockSync, times(1)).stop();
        assertThat(blockFetchService.isScheduledToStop()).isTrue();
    }

    @Test
    void anUnrelatedRestartRequestStillRestartsWithoutAStopInBetween() throws Exception {
        storeProperties.setAutoRestartDebounceWindowMs(0);
        blockFetchService.startSync(new Point(1000, "aa"));

        processor.handleRestartInterval(RequiredSyncRestartEvent.builder().reason("test").build());

        verify(blockSync, times(1)).stop();
        verify(cursorService).getStartCursor();
    }
}
