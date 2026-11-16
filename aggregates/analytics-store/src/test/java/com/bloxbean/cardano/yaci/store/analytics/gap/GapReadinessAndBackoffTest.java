package com.bloxbean.cardano.yaci.store.analytics.gap;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportState;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStatus;
import com.bloxbean.cardano.yaci.store.blocks.domain.Block;
import com.bloxbean.cardano.yaci.store.blocks.storage.BlockStorageReader;
import com.bloxbean.cardano.yaci.store.core.configuration.GenesisConfig;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Gap detection counts only partitions that are ready, and waits before retrying failed ones.
 */
class GapReadinessAndBackoffTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 16, 12, 0);

    private ExportStateService stateService;
    private AnalyticsStoreProperties properties;
    private GapDetectionService gaps;

    @BeforeEach
    void setUp() {
        EraService eraService = mock(EraService.class);
        BlockStorageReader blockStorageReader = mock(BlockStorageReader.class);
        stateService = mock(ExportStateService.class);
        properties = new AnalyticsStoreProperties();
        gaps = new GapDetectionService(mock(GenesisConfig.class), blockStorageReader, stateService, properties,
                eraService);
        gaps.setClock(Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));

        when(eraService.getFirstNonByronEpoch()).thenReturn(Optional.of(208));
        when(blockStorageReader.findRecentBlock()).thenReturn(Optional.of(Block.builder().epochNumber(500).build()));
        when(stateService.getFailedExports(any(), anyInt())).thenReturn(List.of());
    }

    @Test
    void theCurrentAndPreviousEpochOfAnAdaPotGatedTableAreNotGaps() {
        // reward[N] needs AdaPot job N+2: epochs 499 and 500 are missing but cannot be exported yet
        TableExporter reward = epochExporter("reward", epoch -> epoch <= 498);
        when(stateService.getCompletedPartitions("reward")).thenReturn(completedEpochs(208, 498));

        GapDetectionService.TableGaps result = gaps.findGaps(reward);

        assertThat(result.due()).isEmpty();
        assertThat(result.readyMissing()).isZero();
        assertThat(result.notReady()).containsExactly(PartitionValue.ofEpoch(499), PartitionValue.ofEpoch(500));
    }

    @Test
    void withAdaPotDisabledNoGatedEpochIsAGap() {
        TableExporter reward = epochExporter("reward", epoch -> false);
        when(stateService.getCompletedPartitions("reward")).thenReturn(Set.of());

        GapDetectionService.TableGaps result = gaps.findGaps(reward);

        assertThat(result.due()).isEmpty();
        assertThat(result.notReady()).hasSize(500 - 208 + 1);
    }

    @Test
    void aFailingReadinessCheckMeansNotReady() {
        TableExporter epochStake = epochExporter("epoch_stake", epoch -> {
            throw new IllegalStateException("adapot job storage unavailable");
        });
        when(stateService.getCompletedPartitions("epoch_stake")).thenReturn(completedEpochs(208, 499));

        assertThat(gaps.findGaps(epochStake).notReady()).containsExactly(PartitionValue.ofEpoch(500));
    }

    @Test
    void aFailedPartitionWaitsForItsRetry() {
        TableExporter reward = epochExporter("reward", epoch -> true);
        when(stateService.getCompletedPartitions("reward")).thenReturn(completedEpochs(208, 499));
        // Third failure (retry_count 2) three minutes ago: the next retry is due after 4 minutes
        when(stateService.getFailedExports(eq("reward"), anyInt()))
                .thenReturn(List.of(failed("reward", "epoch=500", 2, NOW.minusMinutes(3))));

        GapDetectionService.TableGaps waiting = gaps.findGaps(reward);
        assertThat(waiting.due()).isEmpty();
        assertThat(waiting.backingOff()).containsExactly(PartitionValue.ofEpoch(500));
        assertThat(waiting.readyMissing()).isEqualTo(1);

        gaps.setClock(Clock.fixed(NOW.plusMinutes(1).atZone(ZONE).toInstant(), ZONE));
        assertThat(gaps.findGaps(reward).due()).containsExactly(PartitionValue.ofEpoch(500));
    }

    @Test
    void retryDelayDoublesUpToTheCap() {
        assertThat(gaps.retryDelay(0)).isEqualTo(Duration.ofMinutes(1));
        assertThat(gaps.retryDelay(1)).isEqualTo(Duration.ofMinutes(2));
        assertThat(gaps.retryDelay(3)).isEqualTo(Duration.ofMinutes(8));
        assertThat(gaps.retryDelay(9)).isEqualTo(Duration.ofMinutes(360));
        assertThat(gaps.retryDelay(Integer.MAX_VALUE)).isEqualTo(Duration.ofMinutes(360));

        properties.getContinuousSync().setRetryBackoffInitialMinutes(5);
        properties.getContinuousSync().setRetryBackoffMaxMinutes(30);
        assertThat(gaps.retryDelay(0)).isEqualTo(Duration.ofMinutes(5));
        assertThat(gaps.retryDelay(2)).isEqualTo(Duration.ofMinutes(20));
        assertThat(gaps.retryDelay(3)).isEqualTo(Duration.ofMinutes(30));
    }

    private static TableExporter epochExporter(String table, java.util.function.IntPredicate ready) {
        TableExporter exporter = mock(TableExporter.class);
        when(exporter.getTableName()).thenReturn(table);
        when(exporter.getPartitionStrategy()).thenReturn(PartitionStrategy.EPOCH);
        when(exporter.preExportValidation(any())).thenAnswer(inv ->
                ready.test(((PartitionValue.EpochPartition) inv.getArgument(0)).epoch()));
        return exporter;
    }

    private static Set<String> completedEpochs(int from, int to) {
        Set<String> done = new java.util.HashSet<>();
        for (int e = from; e <= to; e++) {
            done.add("epoch=" + e);
        }
        return done;
    }

    private static ExportState failed(String table, String partition, int retryCount, LocalDateTime at) {
        ExportState state = new ExportState(table, partition);
        state.setStatus(ExportStatus.FAILED);
        state.setRetryCount(retryCount);
        state.setCompletedAt(at);
        return state;
    }
}
