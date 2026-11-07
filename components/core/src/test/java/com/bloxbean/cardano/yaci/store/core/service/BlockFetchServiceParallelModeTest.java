package com.bloxbean.cardano.yaci.store.core.service;

import com.bloxbean.cardano.yaci.core.model.Block;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.helper.BlockRangeSync;
import com.bloxbean.cardano.yaci.helper.BlockSync;
import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.metrics.MetricsService;
import com.bloxbean.cardano.yaci.store.core.storage.api.ErrorStorage;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ByronBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.core.service.publisher.ShelleyBlockEventPublisher;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BlockFetchServiceParallelModeTest {
    private static final String ISSUER_VKEY = "a".repeat(64);

    private EraService eraService;
    private ShelleyBlockEventPublisher shelleyPublisher;
    private BlockFetchService blockFetchService;

    @BeforeEach
    void setUp() {
        eraService = mock(EraService.class);
        shelleyPublisher = mock(ShelleyBlockEventPublisher.class);
        StoreProperties storeProperties = mock(StoreProperties.class);
        when(storeProperties.isEnableParallelProcessing()).thenReturn(true);

        blockFetchService = new BlockFetchService(
                mock(ApplicationEventPublisher.class),
                mock(MetricsService.class),
                mock(BlockRangeSync.class),
                mock(BlockSync.class),
                mock(CursorService.class),
                eraService,
                storeProperties,
                mock(GenesisConfig.class),
                shelleyPublisher,
                mock(ByronBlockEventPublisher.class),
                mock(ErrorStorage.class));

        ReflectionTestUtils.setField(blockFetchService, "previousEpoch", 100);
        ReflectionTestUtils.setField(blockFetchService, "previousEra", Era.Conway);
    }

    @Test
    void firstBlockOfEpochIsPublishedSequentiallyWithParallelModeOff() {
        Block block = block(2000L);
        when(eraService.getEpochNo(Era.Conway, 2000L)).thenReturn(101);

        blockFetchService.onBlock(Era.Conway, block, List.of());

        var captor = ArgumentCaptor.forClass(EventMetadata.class);
        verify(shelleyPublisher).processBlocksInParallel();
        verify(shelleyPublisher).publishBlockEvents(captor.capture(), eq(block), any());
        verify(shelleyPublisher, never()).publishBlockEventsInParallel(any(), any(), any());
        assertThat(captor.getValue().isParallelMode()).isFalse();
    }

    private Block block(long slot) {
        Block block = mock(Block.class, RETURNS_DEEP_STUBS);
        when(block.getHeader().getHeaderBody().getSlot()).thenReturn(slot);
        when(block.getHeader().getHeaderBody().getIssuerVkey()).thenReturn(ISSUER_VKEY);
        return block;
    }
}
