package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
import com.bloxbean.cardano.yaci.store.events.internal.PreBlockSyncEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Every start of chain sync publishes a {@link PreBlockSyncEvent} before the connection opens: the one-argument
 * startSync, the two-argument one (batchDone at the end of the range fetch), and startSyncWithTimeout (StartService).
 */
class BlockFetchServicePreBlockSyncTest {
    private BlockSync blockSync;
    private ApplicationEventPublisher publisher;
    private BlockFetchService blockFetchService;

    @BeforeEach
    void setUp() {
        blockSync = mock(BlockSync.class);
        publisher = mock(ApplicationEventPublisher.class);
        EraService eraService = mock(EraService.class);
        StoreProperties storeProperties = mock(StoreProperties.class);
        when(storeProperties.getKeepAliveInterval()).thenReturn(60_000);
        when(eraService.getEpochNo(eq(Era.Conway), anyLong())).thenReturn(100);

        blockFetchService = new BlockFetchService(publisher, mock(MetricsService.class), mock(BlockRangeSync.class),
                blockSync, mock(CursorService.class), eraService, storeProperties, mock(GenesisConfig.class),
                mock(ShelleyBlockEventPublisher.class), mock(ByronBlockEventPublisher.class), mock(ErrorStorage.class));
        blockFetchService.setSessionGuard(new SyncSessionGuard());
        ReflectionTestUtils.setField(blockFetchService, "previousEpoch", 100);
        ReflectionTestUtils.setField(blockFetchService, "previousEra", Era.Conway);
    }

    @AfterEach
    void tearDown() {
        blockFetchService.stop();
    }

    @Test
    void startSyncPublishesTheEventBeforeConnecting() {
        blockFetchService.startSync(new Point(100, "aa"));
        assertPublishedBeforeConnecting(new Point(100, "aa"));
    }

    @Test
    void startSyncFromAProcessedBlockPublishesTheEventBeforeConnecting() {
        blockFetchService.startSync(new Point(200, "bb"), 20L);
        assertPublishedBeforeConnecting(new Point(200, "bb"));
    }

    @Test
    void startSyncWithTimeoutPublishesTheEventBeforeConnecting() {
        blockFetchService.startSyncWithTimeout(new Point(300, "cc"), 30L, 0);
        assertPublishedBeforeConnecting(new Point(300, "cc"));
    }

    private void assertPublishedBeforeConnecting(Point from) {
        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        InOrder order = inOrder(publisher, blockSync);
        order.verify(publisher, atLeastOnce()).publishEvent(events.capture());
        order.verify(blockSync).startSync(eq(from), any(BlockChainDataListener.class));

        var preBlockSync = events.getAllValues().stream().filter(PreBlockSyncEvent.class::isInstance)
                .map(PreBlockSyncEvent.class::cast).toList();
        assertThat(preBlockSync).hasSize(1);
        assertThat(preBlockSync.get(0).getFrom()).isEqualTo(from);
    }
}
