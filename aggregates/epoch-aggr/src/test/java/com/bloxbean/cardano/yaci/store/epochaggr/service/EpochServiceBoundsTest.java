package com.bloxbean.cardano.yaci.store.epochaggr.service;

import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorage;
import com.bloxbean.cardano.yaci.store.epochaggr.domain.Epoch;
import com.bloxbean.cardano.yaci.store.epochaggr.storage.EpochStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EpochServiceBoundsTest {
    @Mock
    private BlockStorage blockStorage;
    @Mock
    private EpochStorage epochStorage;
    @InjectMocks
    private EpochService epochService;

    @ParameterizedTest
    @MethodSource("blockOrders")
    void computesBoundsIndependentOfBlockOrderAndPreservesTotals(List<Block> blocks) {
        aggregate(blocks);

        var captor = ArgumentCaptor.forClass(Epoch.class);
        verify(epochStorage).save(captor.capture());
        var epoch = captor.getValue();
        assertSoftly(softly -> {
            softly.assertThat(epoch.getNumber()).isEqualTo(200);
            softly.assertThat(epoch.getStartTime()).isEqualTo(1_000);
            softly.assertThat(epoch.getEndTime()).isEqualTo(1_200);
            softly.assertThat(epoch.getMaxSlot()).isEqualTo(120);
            softly.assertThat(epoch.getBlockCount()).isEqualTo(3);
            softly.assertThat(epoch.getTransactionCount()).isEqualTo(6);
            softly.assertThat(epoch.getTotalOutput()).isEqualTo(400);
            softly.assertThat(epoch.getTotalFees()).isEqualTo(40);
        });
    }

    @Test
    void singleBlockDefinesBothTimeBoundsAndMaxSlot() {
        aggregate(List.of(block(100, 1_000, 2, null, null)));

        var captor = ArgumentCaptor.forClass(Epoch.class);
        verify(epochStorage).save(captor.capture());
        var epoch = captor.getValue();
        assertThat(epoch.getStartTime()).isEqualTo(1_000);
        assertThat(epoch.getEndTime()).isEqualTo(1_000);
        assertThat(epoch.getMaxSlot()).isEqualTo(100);
        assertThat(epoch.getBlockCount()).isEqualTo(1);
        assertThat(epoch.getTransactionCount()).isEqualTo(2);
        assertThat(epoch.getTotalOutput()).isZero();
        assertThat(epoch.getTotalFees()).isZero();
    }

    @Test
    void doesNotSaveAnAggregateWhenTheEpochHasNoBlocks() {
        aggregate(List.of());

        verify(epochStorage, never()).save(any());
    }

    private void aggregate(List<Block> blocks) {
        when(blockStorage.findRecentBlock()).thenReturn(Optional.of(Block.builder().epochNumber(200).build()));
        when(epochStorage.findByNumber(200)).thenReturn(Optional.of(Epoch.builder().number(200).build()));
        when(blockStorage.findBlocksByEpoch(200)).thenReturn(blocks);

        epochService.aggregateData();
    }

    private static Stream<List<Block>> blockOrders() {
        var first = block(100, 1_000, 1, 100L, 10L);
        var middle = block(110, 1_100, 2, 300L, null);
        var last = block(120, 1_200, 3, null, 30L);
        return Stream.of(
                List.of(first, middle, last), List.of(first, last, middle),
                List.of(middle, first, last), List.of(middle, last, first),
                List.of(last, first, middle), List.of(last, middle, first));
    }

    private static Block block(long slot, long time, int transactionCount, Long output, Long fees) {
        return Block.builder().epochNumber(200).slot(slot).blockTime(time).noOfTxs(transactionCount)
                .totalOutput(output == null ? null : BigInteger.valueOf(output))
                .totalFees(fees == null ? null : BigInteger.valueOf(fees)).build();
    }
}
