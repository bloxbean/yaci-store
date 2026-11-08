package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.helper.listener.BlockChainDataListener;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import com.bloxbean.cardano.yaci.store.events.internal.PreBlockSyncEvent;

/**
 * Merge of #1332 (batchDone under the BlockFetchService lock) and #1271 (PreBlockSyncEvent before chain sync, whose
 * listener can build the unspent table for a long time): the event is published without holding the lock.
 */
class BlockFetchServicePreBlockSyncTest {

    @Test
    void batchDoneStartsChainSyncWithoutHoldingTheLockDuringPreBlockSyncEvent() {
        BlockSync blockSync = mock(BlockSync.class);
        BlockRangeSync blockRangeSync = mock(BlockRangeSync.class);
        CursorService cursorService = mock(CursorService.class);
        StoreProperties storeProperties = mock(StoreProperties.class);
        when(storeProperties.getKeepAliveInterval()).thenReturn(60_000);
        when(storeProperties.isPrimaryInstance()).thenReturn(true);
        when(cursorService.getCursor()).thenReturn(Optional.of(
                Cursor.builder().slot(200L).block(200L).blockHash("h200").prevBlockHash("h199").era(Era.Conway).build()));
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        EraService eraService = mock(EraService.class);
        when(eraService.getEpochNo(eq(Era.Conway), anyLong())).thenReturn(100);
        BlockFetchService service = new BlockFetchService(publisher, mock(MetricsService.class), blockRangeSync,
                blockSync, cursorService, eraService, storeProperties, mock(GenesisConfig.class),
                mock(ShelleyBlockEventPublisher.class), mock(ByronBlockEventPublisher.class), mock(ErrorStorage.class));
        service.setSessionGuard(new SyncSessionGuard());

        var lockHeld = new java.util.concurrent.atomic.AtomicReference<Boolean>();
        doAnswer(inv -> {
            if (inv.getArgument(0) instanceof PreBlockSyncEvent)
                lockHeld.set(Thread.holdsLock(service));
            return null;
        }).when(publisher).publishEvent(any(Object.class));

        service.startFetch(new Point(100, "h100"), new Point(200, "h200"), 100L);
        var captor = ArgumentCaptor.forClass(BlockChainDataListener.class);
        verify(blockRangeSync).restart(captor.capture());

        captor.getValue().batchDone();

        assertThat(lockHeld.get()).isFalse();
        verify(blockRangeSync).stop();
        verify(blockSync).startSync(eq(new Point(200, "h200")), any());
    }
}
