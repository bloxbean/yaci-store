package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.helper.GenesisBlockFinder;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartServiceTest {
    private static final long CURSOR_SLOT = 115239162;

    @Mock
    private ApplicationEventPublisher publisher;
    @Mock
    private BlockFetchService blockFetchService;
    @Mock
    private TipFinderService tipFinderService;
    @Mock
    private GenesisBlockFinder genesisBlockFinder;
    @Mock
    private CursorService cursorService;
    @Mock
    private InitService initService;
    @Mock
    private GenesisConfig genesisConfig;

    private StoreProperties storeProperties;
    private StartService startService;

    @BeforeEach
    void setUp() {
        storeProperties = new StoreProperties();
        startService = new StartService(publisher, blockFetchService, tipFinderService, genesisBlockFinder,
                cursorService, initService, storeProperties, genesisConfig);

        when(cursorService.getStartCursor()).thenReturn(Optional.of(
                new Cursor(CURSOR_SLOT, "cursor_hash", 10_000_000L, "prev_hash", Era.Babbage)));
    }

    @Test
    void start_farFromTip_rangeFetchSkipsTheProcessedCursorBlock() {
        //The range fetch delivers the cursor block again, but BlockFetchService skips it because it is processed
        //(#1313), so stores keep its rows
        tipAt(CURSOR_SLOT + storeProperties.getBlockDiffToStartSyncProtocol() + 1);

        startService.start();

        assertThat(rollbackEvent().isRollbackPointReprocessed()).isFalse();
        assertThat(rollbackEvent().generatedKeyRollbackSlot()).isEqualTo(CURSOR_SLOT);
        verify(blockFetchService).startFetchWithTimeout(eq(new Point(CURSOR_SLOT, "cursor_hash")), any(), eq(10_000_000L), anyLong());
        verify(blockFetchService, never()).startSyncWithTimeout(any(), any(), anyLong());
    }

    @Test
    void start_fromAPlaceholderCursor_rangeFetchReprocessesTheStartBlock() {
        //A fresh start saved its start point as cursor (no previous hash) but never processed that block
        when(cursorService.getStartCursor()).thenReturn(Optional.of(
                new Cursor(CURSOR_SLOT, "cursor_hash", 10_000_000L, null, Era.Babbage)));
        tipAt(CURSOR_SLOT + storeProperties.getBlockDiffToStartSyncProtocol() + 1);

        startService.start();

        assertThat(rollbackEvent().isRollbackPointReprocessed()).isTrue();
        verify(blockFetchService).startFetchWithTimeout(eq(new Point(CURSOR_SLOT, "cursor_hash")), any(), eq((Long) null), anyLong());
    }

    @Test
    void start_nearTip_chainSyncDoesNotReprocessCursorBlock() {
        tipAt(CURSOR_SLOT + 10);

        startService.start();

        assertThat(rollbackEvent().isRollbackPointReprocessed()).isFalse();
        assertThat(rollbackEvent().generatedKeyRollbackSlot()).isEqualTo(CURSOR_SLOT);
        verify(blockFetchService).startSyncWithTimeout(eq(new Point(CURSOR_SLOT, "cursor_hash")), eq(10_000_000L), anyLong());
        verify(blockFetchService, never()).startFetchWithTimeout(any(), any(), any(), anyLong());
    }

    @Test
    void start_withStopSlot_rangeFetchSkipsTheProcessedCursorBlock() {
        storeProperties.setSyncStopSlot(CURSOR_SLOT + 10);
        storeProperties.setSyncStopBlockhash("stop_hash");
        tipAt(CURSOR_SLOT + 10);

        startService.start();

        assertThat(rollbackEvent().isRollbackPointReprocessed()).isFalse();
        verify(blockFetchService).startFetchWithTimeout(eq(new Point(CURSOR_SLOT, "cursor_hash")), eq(new Point(CURSOR_SLOT + 10, "stop_hash")), eq(10_000_000L), anyLong());
    }

    private void tipAt(long slot) {
        when(tipFinderService.findTip(org.mockito.ArgumentMatchers.anyLong())).thenReturn(new Tip(new Point(slot, "tip_hash"), 10_000_100L));
    }

    private RollbackEvent rollbackEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher, atLeastOnce()).publishEvent(captor.capture());
        return captor.getAllValues().stream()
                .filter(RollbackEvent.class::isInstance)
                .map(RollbackEvent.class::cast)
                .findFirst()
                .orElseThrow();
    }
}
