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
    void start_farFromTip_rangeFetchReprocessesCursorBlock() {
        tipAt(CURSOR_SLOT + storeProperties.getBlockDiffToStartSyncProtocol() + 1);

        startService.start();

        assertThat(rollbackEvent().isRollbackPointReprocessed()).isTrue();
        assertThat(rollbackEvent().generatedKeyRollbackSlot()).isEqualTo(CURSOR_SLOT - 1);
        verify(blockFetchService).startFetch(eq(new Point(CURSOR_SLOT, "cursor_hash")), any());
        verify(blockFetchService, never()).startSync(any());
    }

    @Test
    void start_nearTip_chainSyncDoesNotReprocessCursorBlock() {
        tipAt(CURSOR_SLOT + 10);

        startService.start();

        assertThat(rollbackEvent().isRollbackPointReprocessed()).isFalse();
        assertThat(rollbackEvent().generatedKeyRollbackSlot()).isEqualTo(CURSOR_SLOT);
        verify(blockFetchService).startSync(new Point(CURSOR_SLOT, "cursor_hash"));
        verify(blockFetchService, never()).startFetch(any(), any());
    }

    @Test
    void start_withStopSlot_rangeFetchReprocessesCursorBlock() {
        storeProperties.setSyncStopSlot(CURSOR_SLOT + 10);
        storeProperties.setSyncStopBlockhash("stop_hash");
        tipAt(CURSOR_SLOT + 10);

        startService.start();

        assertThat(rollbackEvent().isRollbackPointReprocessed()).isTrue();
        verify(blockFetchService).startFetch(new Point(CURSOR_SLOT, "cursor_hash"), new Point(CURSOR_SLOT + 10, "stop_hash"));
    }

    private void tipAt(long slot) {
        when(tipFinderService.getTip()).thenReturn(Mono.just(new Tip(new Point(slot, "tip_hash"), 10_000_100L)));
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
