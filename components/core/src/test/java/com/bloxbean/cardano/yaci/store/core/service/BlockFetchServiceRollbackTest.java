package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.bloxbean.cardano.yaci.store.events.internal.RequiredSyncRestartEvent;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BlockFetchServiceRollbackTest {
    private ApplicationEventPublisher publisher;
    private CursorService cursorService;
    private EraService eraService;
    private GenesisConfig genesisConfig;
    private BlockFetchService blockFetchService;

    @BeforeEach
    void setUp() {
        publisher = mock(ApplicationEventPublisher.class);
        cursorService = mock(CursorService.class);
        eraService = mock(EraService.class);
        genesisConfig = mock(GenesisConfig.class);

        blockFetchService = new BlockFetchService(
                publisher,
                mock(MetricsService.class),
                mock(BlockRangeSync.class),
                mock(BlockSync.class),
                cursorService,
                eraService,
                mock(StoreProperties.class),
                genesisConfig,
                mock(ShelleyBlockEventPublisher.class),
                mock(ByronBlockEventPublisher.class),
                mock(ErrorStorage.class));
    }

    @Test
    void shouldRestoreEpochAndEraFromRollbackCursor() {
        var currentCursor = cursor(192844802L, 13695759L, "current", Era.Conway);
        var rollbackCursor = cursor(192844727L, 13695758L, "rollback", Era.Conway);

        when(cursorService.getCursor())
                .thenReturn(Optional.of(currentCursor))
                .thenReturn(Optional.of(rollbackCursor));
        when(eraService.getEpochNo(Era.Conway, rollbackCursor.getSlot())).thenReturn(643);

        ReflectionTestUtils.setField(blockFetchService, "previousEpoch", 644);
        ReflectionTestUtils.setField(blockFetchService, "previousEra", Era.Conway);

        blockFetchService.onRollback(new Point(rollbackCursor.getSlot(), rollbackCursor.getBlockHash()));

        verify(cursorService).rollback(rollbackCursor.getSlot());
        assertThat(ReflectionTestUtils.getField(blockFetchService, "previousEpoch")).isEqualTo(643);
        assertThat(ReflectionTestUtils.getField(blockFetchService, "previousEra")).isEqualTo(Era.Conway);
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(
                blockFetchService, "detectIfNewEpoch", 644, 192845302L)).isTrue();
    }

    @Test
    void shouldClearEpochAndEraWhenRollbackLeavesNoCursor() {
        var currentCursor = cursor(100L, 10L, "current", Era.Conway);

        when(cursorService.getCursor())
                .thenReturn(Optional.of(currentCursor))
                .thenReturn(Optional.empty());

        ReflectionTestUtils.setField(blockFetchService, "previousEpoch", 1);
        ReflectionTestUtils.setField(blockFetchService, "previousEra", Era.Conway);

        blockFetchService.onRollback(new Point(99L, "rollback"));

        assertThat(ReflectionTestUtils.getField(blockFetchService, "previousEpoch")).isNull();
        assertThat(ReflectionTestUtils.getField(blockFetchService, "previousEra")).isNull();
    }

    @Test
    void shouldNotReportEpochChangeAfterRollbackWithinSameEpoch() {
        var currentCursor = cursor(200L, 20L, "current", Era.Conway);
        var rollbackCursor = cursor(100L, 10L, "rollback", Era.Conway);

        when(cursorService.getCursor())
                .thenReturn(Optional.of(currentCursor))
                .thenReturn(Optional.of(rollbackCursor));
        when(eraService.getEpochNo(Era.Conway, rollbackCursor.getSlot())).thenReturn(644);

        blockFetchService.onRollback(new Point(rollbackCursor.getSlot(), rollbackCursor.getBlockHash()));

        assertThat((Boolean) ReflectionTestUtils.invokeMethod(
                blockFetchService, "detectIfNewEpoch", 644, 201L)).isFalse();
    }

    @Test
    void shouldLeaveByronEpochUnsetWithoutCallingEraService() {
        var currentCursor = cursor(100L, 20L, "current", Era.Byron);
        var rollbackCursor = cursor(90L, 19L, "rollback", Era.Byron);

        when(cursorService.getCursor())
                .thenReturn(Optional.of(currentCursor))
                .thenReturn(Optional.of(rollbackCursor));

        ReflectionTestUtils.setField(blockFetchService, "previousEpoch", 1);
        ReflectionTestUtils.setField(blockFetchService, "previousEra", Era.Byron);

        blockFetchService.onRollback(new Point(rollbackCursor.getSlot(), rollbackCursor.getBlockHash()));

        verify(eraService, never()).getEpochNo(Era.Byron, rollbackCursor.getSlot());
        assertThat(ReflectionTestUtils.getField(blockFetchService, "previousEpoch")).isNull();
        assertThat(ReflectionTestUtils.getField(blockFetchService, "previousEra")).isEqualTo(Era.Byron);
    }

    // Preprod and mainnet: k = 2160 blocks, f = 0.05, so the deepest valid rollback is 3k/f = 129600 slots.
    private void mainnetLikeGenesis() {
        when(genesisConfig.getSecurityParam()).thenReturn(2160);
        when(genesisConfig.getActiveSlotsCoeff()).thenReturn(0.05);
    }

    @Test
    void rollbackDeeperThan2160SlotsIsApplied() {
        // 13581 slots (about 3.8 hours, ~680 blocks) is well within k blocks. It used to be ignored because
        // 2160 (k, a number of blocks) was compared with a number of slots.
        mainnetLikeGenesis();
        var currentCursor = cursor(138_221_557L, 5_377_000L, "current", Era.Conway);
        var rollbackCursor = cursor(138_207_976L, 5_376_320L, "rollback", Era.Conway);
        when(cursorService.getCursor())
                .thenReturn(Optional.of(currentCursor))
                .thenReturn(Optional.of(rollbackCursor));

        blockFetchService.onRollback(new Point(rollbackCursor.getSlot(), rollbackCursor.getBlockHash()));

        verify(cursorService).rollback(rollbackCursor.getSlot());
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void rollbackDeeperThanStabilityWindowIsRefusedAndSyncRestarts() {
        mainnetLikeGenesis();
        var currentCursor = cursor(138_221_557L, 5_377_000L, "current", Era.Conway);
        when(cursorService.getCursor()).thenReturn(Optional.of(currentCursor));

        blockFetchService.onRollback(new Point(138_221_557L - 129_601L, "too-old"));

        verify(cursorService, never()).rollback(anyLong());
        assertThat(blockFetchService.isError()).isTrue();
        var captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOfSatisfying(RequiredSyncRestartEvent.class,
                e -> assertThat(e.getReason()).isEqualTo("RollbackTooDeep"));
    }

    @Test
    void rollbackAtTheStabilityWindowLimitIsApplied() {
        mainnetLikeGenesis();
        var currentCursor = cursor(200_000L, 9_000L, "current", Era.Conway);
        when(cursorService.getCursor()).thenReturn(Optional.of(currentCursor));

        blockFetchService.onRollback(new Point(200_000L - 129_600L, "limit"));

        verify(cursorService).rollback(200_000L - 129_600L);
    }

    @Test
    void genesisPointWithoutHashIsIgnoredWithoutRestart() {
        when(cursorService.getCursor()).thenReturn(Optional.of(cursor(500L, 20L, "current", Era.Conway)));

        blockFetchService.onRollback(new Point(0, null));

        verify(cursorService, never()).rollback(anyLong());
        verify(publisher, never()).publishEvent(any());
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void rollbackWithNoCursorDoesNothing() {
        when(cursorService.getCursor()).thenReturn(Optional.empty());

        blockFetchService.onRollback(new Point(100L, "rollback"));

        verify(cursorService, never()).rollback(anyLong());
        verify(publisher, never()).publishEvent(any());
    }

    @Test
    void depthIsCountedInBlocksWhenThePointIsInTheCursorTable() {
        mainnetLikeGenesis();
        var currentCursor = cursor(5_000_000L, 10_000L, "current", Era.Conway);
        when(cursorService.getCursor()).thenReturn(Optional.of(currentCursor));
        // exactly k blocks back, even though it is far more than 3k/f slots back (a long gap in the chain)
        when(cursorService.getCursorByBlockHash("k-back"))
                .thenReturn(Optional.of(cursor(4_000_000L, 10_000L - 2160, "k-back", Era.Conway)));

        blockFetchService.onRollback(new Point(4_000_000L, "k-back"));

        verify(cursorService).rollback(4_000_000L);
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void moreThanKBlocksIsRefusedEvenIfFewSlots() {
        mainnetLikeGenesis();
        var currentCursor = cursor(200_000L, 10_000L, "current", Era.Conway);
        when(cursorService.getCursor()).thenReturn(Optional.of(currentCursor));
        when(cursorService.getCursorByBlockHash("too-deep"))
                .thenReturn(Optional.of(cursor(150_000L, 10_000L - 2161, "too-deep", Era.Conway)));

        blockFetchService.onRollback(new Point(150_000L, "too-deep"));

        verify(cursorService, never()).rollback(anyLong());
        assertThat(blockFetchService.isError()).isTrue();
    }

    @Test
    void sparseDevnetRollbackOfOneBlockAcrossAGapIsApplied() {
        // k = 10, f = 1: 3k/f is only 30 slots, but the rollback is one block, so it is valid
        when(genesisConfig.getSecurityParam()).thenReturn(10);
        when(genesisConfig.getActiveSlotsCoeff()).thenReturn(1.0);
        var currentCursor = cursor(632L, 51L, "current", Era.Conway);
        when(cursorService.getCursor())
                .thenReturn(Optional.of(currentCursor))
                .thenReturn(Optional.of(cursor(100L, 50L, "parent", Era.Conway)));
        when(cursorService.getCursorByBlockHash("parent"))
                .thenReturn(Optional.of(cursor(100L, 50L, "parent", Era.Conway)));

        blockFetchService.onRollback(new Point(100L, "parent"));

        verify(cursorService).rollback(100L);
        assertThat(blockFetchService.isError()).isFalse();
    }

    @Test
    void missingGenesisValuesFallBackToMainnetParameters() {
        assertThat(blockFetchService.maxRollbackSlots()).isEqualTo(129_600L);
    }

    private Cursor cursor(long slot, long block, String hash, Era era) {
        return Cursor.builder()
                .slot(slot)
                .block(block)
                .blockHash(hash)
                .era(era)
                .build();
    }
}
