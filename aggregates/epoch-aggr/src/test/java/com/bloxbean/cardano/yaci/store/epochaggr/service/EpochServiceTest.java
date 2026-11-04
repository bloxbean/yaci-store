package com.bloxbean.cardano.yaci.store.epochaggr.service;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.EpochStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpochServiceTest {
    @Mock
    private BlockStorage blockStorage;
    @Mock
    private EpochStorage epochStorage;
    @InjectMocks
    private EpochService epochService;

    @Test
    void sumsBlockTotalsAndRecomputesWhenStoredBlocksChange() {
        var first = block(100, 4_494_944, 17_000_000);
        var second = block(101, 15_494_944, 8_000_000);
        when(blockStorage.findRecentBlock()).thenReturn(Optional.of(second), Optional.of(first), Optional.of(second));
        when(epochStorage.findByNumber(200)).thenReturn(Optional.of(Epoch.builder().number(200).build()));
        when(blockStorage.findBlocksByEpoch(200)).thenReturn(List.of(first, second), List.of(first), List.of(first, second));

        epochService.aggregateData();
        epochService.aggregateData();
        epochService.aggregateData();

        var captor = ArgumentCaptor.forClass(Epoch.class);
        verify(epochStorage, times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Epoch::getTotalFees)
                .containsExactly(BigInteger.valueOf(19_989_888), BigInteger.valueOf(4_494_944), BigInteger.valueOf(19_989_888));
        assertThat(captor.getAllValues()).extracting(Epoch::getTotalOutput)
                .containsExactly(BigInteger.valueOf(25_000_000), BigInteger.valueOf(17_000_000), BigInteger.valueOf(25_000_000));
        assertThat(captor.getValue().getBlockCount()).isEqualTo(2);
    }

    private Block block(long slot, long fees, long output) {
        return Block.builder().epochNumber(200).slot(slot).blockTime(slot)
                .noOfTxs(2).totalFees(BigInteger.valueOf(fees)).totalOutput(BigInteger.valueOf(output)).build();
    }
}
