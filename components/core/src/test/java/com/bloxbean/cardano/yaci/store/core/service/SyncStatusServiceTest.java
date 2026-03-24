package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.domain.SyncStatus;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SyncStatusServiceTest {

    @Mock
    private CursorService cursorService;
    @Mock
    private ChainTipService chainTipService;
    @Mock
    private EraService eraService;
    @Mock
    private StoreProperties storeProperties;

    @InjectMocks
    private SyncStatusService syncStatusService;

    @Test
    void getSyncStatus_noCursor_returnsDefaults() {
        when(cursorService.getCursor()).thenReturn(Optional.empty());
        when(storeProperties.getProtocolMagic()).thenReturn(764824073L);

        SyncStatus status = syncStatusService.getSyncStatus();

        assertEquals(0, status.block());
        assertEquals(0, status.slot());
        assertEquals(0, status.epoch());
        assertEquals("Unknown", status.era());
        assertEquals("", status.blockHash());
        assertEquals(0.0, status.syncPercentage());
        assertFalse(status.synced());
        assertEquals(764824073L, status.protocolMagic());
    }

    @Test
    void getSyncStatus_withCursorAndTip_calculatesPercentage() {
        Cursor cursor = Cursor.builder()
                .block(5000L)
                .slot(100000L)
                .blockHash("abc123")
                .era(Era.Babbage)
                .build();
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor));
        when(eraService.getEpochNo(Era.Babbage, 100000L)).thenReturn(350);

        Tip tip = new Tip(new Point(200000L, "tip_hash"), 10000L);
        when(chainTipService.getTipAndCurrentEpoch()).thenReturn(Optional.of(new Tuple<>(tip, 400)));
        when(storeProperties.getProtocolMagic()).thenReturn(1L);

        SyncStatus status = syncStatusService.getSyncStatus();

        assertEquals(5000, status.block());
        assertEquals(100000, status.slot());
        assertEquals(350, status.epoch());
        assertEquals("Babbage", status.era());
        assertEquals("abc123", status.blockHash());
        assertEquals(50.0, status.syncPercentage(), 0.01);
        assertEquals(10000, status.networkBlock());
        assertEquals(200000, status.networkSlot());
        assertFalse(status.synced());
    }

    @Test
    void getSyncStatus_fullySynced() {
        Cursor cursor = Cursor.builder()
                .block(10000L)
                .slot(200000L)
                .blockHash("abc123")
                .era(Era.Conway)
                .build();
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor));
        when(eraService.getEpochNo(Era.Conway, 200000L)).thenReturn(400);

        Tip tip = new Tip(new Point(200005L, "tip_hash"), 10005L);
        when(chainTipService.getTipAndCurrentEpoch()).thenReturn(Optional.of(new Tuple<>(tip, 400)));
        when(storeProperties.getProtocolMagic()).thenReturn(1L);

        SyncStatus status = syncStatusService.getSyncStatus();

        assertTrue(status.synced());
        assertEquals(100.0, status.syncPercentage(), 0.1);
    }

    @Test
    void getSyncStatus_byronEra_epochRemainsZero() {
        Cursor cursor = Cursor.builder()
                .block(100L)
                .slot(2000L)
                .blockHash("byron_hash")
                .era(Era.Byron)
                .build();
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor));
        when(storeProperties.getProtocolMagic()).thenReturn(1L);

        Tip tip = new Tip(new Point(200000L, "tip_hash"), 10000L);
        when(chainTipService.getTipAndCurrentEpoch()).thenReturn(Optional.of(new Tuple<>(tip, 400)));

        SyncStatus status = syncStatusService.getSyncStatus();

        assertEquals(0, status.epoch());
        assertEquals("Byron", status.era());
    }

    @Test
    void getSyncStatus_nullEra_returnsUnknown() {
        Cursor cursor = Cursor.builder()
                .block(100L)
                .slot(2000L)
                .blockHash("hash")
                .era(null)
                .build();
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor));
        when(storeProperties.getProtocolMagic()).thenReturn(1L);

        SyncStatus status = syncStatusService.getSyncStatus();

        assertEquals("Unknown", status.era());
        assertEquals(0, status.epoch());
    }

    @Test
    void getSyncStatus_tipUnavailable_usesCurrentBlockAsNetwork() {
        Cursor cursor = Cursor.builder()
                .block(5000L)
                .slot(100000L)
                .blockHash("abc")
                .era(Era.Babbage)
                .build();
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor));
        when(eraService.getEpochNo(Era.Babbage, 100000L)).thenReturn(350);
        when(chainTipService.getTipAndCurrentEpoch()).thenReturn(Optional.empty());
        when(storeProperties.getProtocolMagic()).thenReturn(1L);

        SyncStatus status = syncStatusService.getSyncStatus();

        assertEquals(5000, status.networkBlock());
        assertEquals(100000, status.networkSlot());
        assertEquals(100.0, status.syncPercentage(), 0.01);
        assertTrue(status.synced());
    }

    @Test
    void getSyncStatus_epochCalculationFails_fallsBackToZero() {
        Cursor cursor = Cursor.builder()
                .block(5000L)
                .slot(100000L)
                .blockHash("abc")
                .era(Era.Babbage)
                .build();
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor));
        when(eraService.getEpochNo(Era.Babbage, 100000L)).thenThrow(new RuntimeException("era not found"));
        when(storeProperties.getProtocolMagic()).thenReturn(1L);

        SyncStatus status = syncStatusService.getSyncStatus();

        assertEquals(0, status.epoch());
    }

    @Test
    void getSyncStatus_tipCaching_doesNotRefetchWithinInterval() {
        Cursor cursor = Cursor.builder()
                .block(5000L)
                .slot(100000L)
                .blockHash("abc")
                .era(Era.Babbage)
                .build();
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor));
        when(eraService.getEpochNo(Era.Babbage, 100000L)).thenReturn(350);
        when(storeProperties.getProtocolMagic()).thenReturn(1L);

        Tip tip = new Tip(new Point(200000L, "tip_hash"), 10000L);
        when(chainTipService.getTipAndCurrentEpoch()).thenReturn(Optional.of(new Tuple<>(tip, 400)));

        // First call fetches tip
        syncStatusService.getSyncStatus();
        // Second call should use cache
        syncStatusService.getSyncStatus();

        verify(chainTipService, times(1)).getTipAndCurrentEpoch();
    }

    @Test
    void getSyncStatus_currentBlockAheadOfCachedTip_returnsCachedWithoutRefetch() {
        // First call: cursor behind tip
        Cursor cursor1 = Cursor.builder()
                .block(5000L).slot(100000L).blockHash("abc").era(Era.Babbage).build();
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor1));
        when(eraService.getEpochNo(any(), anyLong())).thenReturn(350);
        when(storeProperties.getProtocolMagic()).thenReturn(1L);

        Tip tip = new Tip(new Point(200000L, "tip_hash"), 10000L);
        when(chainTipService.getTipAndCurrentEpoch()).thenReturn(Optional.of(new Tuple<>(tip, 400)));

        syncStatusService.getSyncStatus();

        // Second call: cursor has caught up past the cached tip
        Cursor cursor2 = Cursor.builder()
                .block(10001L).slot(200001L).blockHash("def").era(Era.Babbage).build();
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor2));

        syncStatusService.getSyncStatus();

        // Should not have fetched tip again since cursor is ahead
        verify(chainTipService, times(1)).getTipAndCurrentEpoch();
    }
}
