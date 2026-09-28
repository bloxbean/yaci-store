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
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
                mock(ByronBlockEventPublisher.class));
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

    private Cursor cursor(long slot, long block, String hash, Era era) {
        return Cursor.builder()
                .slot(slot)
                .block(block)
                .blockHash(hash)
                .era(era)
                .build();
    }
}
