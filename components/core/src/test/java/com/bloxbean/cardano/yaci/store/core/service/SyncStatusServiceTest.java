package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.domain.SyncStatus;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @Nested
    class WhenNoCursor {

        @Test
        void returnsZeroBlockAndSlot() {
            when(cursorService.getCursor()).thenReturn(Optional.empty());
            when(storeProperties.getProtocolMagic()).thenReturn(764824073L);

            SyncStatus status = syncStatusService.getSyncStatus();

            assertThat(status.block()).isZero();
            assertThat(status.slot()).isZero();
        }

        @Test
        void returnsUnknownEraAndZeroEpoch() {
            when(cursorService.getCursor()).thenReturn(Optional.empty());
            when(storeProperties.getProtocolMagic()).thenReturn(764824073L);

            SyncStatus status = syncStatusService.getSyncStatus();

            assertThat(status.era()).isEqualTo("Unknown");
            assertThat(status.epoch()).isZero();
        }

        @Test
        void returnsZeroSyncPercentage() {
            when(cursorService.getCursor()).thenReturn(Optional.empty());
            when(storeProperties.getProtocolMagic()).thenReturn(764824073L);

            SyncStatus status = syncStatusService.getSyncStatus();

            assertThat(status.syncPercentage()).isZero();
            assertThat(status.synced()).isFalse();
        }

        @Test
        void returnsConfiguredProtocolMagic() {
            when(cursorService.getCursor()).thenReturn(Optional.empty());
            when(storeProperties.getProtocolMagic()).thenReturn(764824073L);

            SyncStatus status = syncStatusService.getSyncStatus();

            assertThat(status.protocolMagic()).isEqualTo(764824073L);
        }
    }

    @Nested
    class SyncPercentage {

        @Test
        void calculatesPercentageFromCursorAndNetworkTip() {
            withCursorAt(5000L, 100000L, Era.Babbage);
            withNetworkTipAt(10000L, 200000L);

            SyncStatus status = syncStatusService.getSyncStatus();

            assertThat(status.syncPercentage()).isCloseTo(50.0, withinPercentage(1));
            assertThat(status.synced()).isFalse();
        }

        @Test
        void reportsFullySyncedWhenWithinTolerance() {
            withCursorAt(10000L, 200000L, Era.Conway);
            withNetworkTipAt(10005L, 200005L);

            SyncStatus status = syncStatusService.getSyncStatus();

            assertThat(status.synced()).isTrue();
            assertThat(status.syncPercentage()).isCloseTo(100.0, withinPercentage(1));
        }

        @Test
        void fallsBackToCurrentBlockWhenTipUnavailable() {
            withCursorAt(5000L, 100000L, Era.Babbage);
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(Optional.empty());

            SyncStatus status = syncStatusService.getSyncStatus();

            assertThat(status.networkBlock()).isEqualTo(5000L);
            assertThat(status.networkSlot()).isEqualTo(100000L);
            assertThat(status.syncPercentage()).isCloseTo(100.0, withinPercentage(1));
            assertThat(status.synced()).isTrue();
        }

        private org.assertj.core.data.Percentage withinPercentage(double pct) {
            return org.assertj.core.data.Percentage.withPercentage(pct);
        }
    }

    @Nested
    class EraAndEpoch {

        @Test
        void populatesEraAndEpochFromCursor() {
            withCursorAt(5000L, 100000L, Era.Babbage);
            when(eraService.getEpochNo(Era.Babbage, 100000L)).thenReturn(350);
            withNetworkTipAt(10000L, 200000L);

            SyncStatus status = syncStatusService.getSyncStatus();

            assertThat(status.era()).isEqualTo("Babbage");
            assertThat(status.epoch()).isEqualTo(350);
        }

        @Test
        void skipsEpochCalculationForByronEra() {
            withCursorAt(100L, 2000L, Era.Byron);
            withNetworkTipAt(10000L, 200000L);

            SyncStatus status = syncStatusService.getSyncStatus();

            assertThat(status.era()).isEqualTo("Byron");
            assertThat(status.epoch()).isZero();
            verifyNoInteractions(eraService);
        }

        @Test
        void returnsUnknownWhenEraIsNull() {
            Cursor cursor = Cursor.builder()
                    .block(100L).slot(2000L).blockHash("hash").era(null).build();
            when(cursorService.getCursor()).thenReturn(Optional.of(cursor));
            when(storeProperties.getProtocolMagic()).thenReturn(1L);

            SyncStatus status = syncStatusService.getSyncStatus();

            assertThat(status.era()).isEqualTo("Unknown");
            assertThat(status.epoch()).isZero();
        }

        @Test
        void fallsBackToZeroEpochWhenCalculationFails() {
            withCursorAt(5000L, 100000L, Era.Babbage);
            when(eraService.getEpochNo(Era.Babbage, 100000L)).thenThrow(new RuntimeException("era not found"));

            SyncStatus status = syncStatusService.getSyncStatus();

            assertThat(status.epoch()).isZero();
        }
    }

    @Nested
    class TipCaching {

        @Test
        void doesNotRefetchWithinRefreshInterval() {
            withCursorAt(5000L, 100000L, Era.Babbage);
            when(eraService.getEpochNo(any(), anyLong())).thenReturn(350);
            withNetworkTipAt(10000L, 200000L);

            syncStatusService.getSyncStatus();
            syncStatusService.getSyncStatus();

            verify(chainTipService, times(1)).getTipAndCurrentEpoch();
        }

        @Test
        void skipsFetchWhenCursorIsAheadOfCachedTip() {
            // First call: cursor behind tip
            withCursorAt(5000L, 100000L, Era.Babbage);
            when(eraService.getEpochNo(any(), anyLong())).thenReturn(350);
            withNetworkTipAt(10000L, 200000L);

            syncStatusService.getSyncStatus();

            // Second call: cursor has caught up past the cached tip
            Cursor cursor2 = Cursor.builder()
                    .block(10001L).slot(200001L).blockHash("def").era(Era.Babbage).build();
            when(cursorService.getCursor()).thenReturn(Optional.of(cursor2));

            syncStatusService.getSyncStatus();

            verify(chainTipService, times(1)).getTipAndCurrentEpoch();
        }
    }

    // -- helpers --

    private void withCursorAt(long block, long slot, Era era) {
        Cursor cursor = Cursor.builder()
                .block(block).slot(slot).blockHash("hash_" + block).era(era).build();
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor));
        when(storeProperties.getProtocolMagic()).thenReturn(1L);
    }

    private void withNetworkTipAt(long block, long slot) {
        Tip tip = new Tip(new Point(slot, "tip_hash"), block);
        when(chainTipService.getTipAndCurrentEpoch()).thenReturn(Optional.of(new Tuple<>(tip, 400)));
    }
}
