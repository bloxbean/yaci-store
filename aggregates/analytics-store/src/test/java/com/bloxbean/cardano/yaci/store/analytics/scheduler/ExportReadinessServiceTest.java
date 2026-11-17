package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.analytics.config.AnalyticsStoreProperties;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.gap.GapDetectionService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportReadinessServiceTest {

    private static final LocalDate EXPORT_END = LocalDate.of(2026, 11, 14);

    private final GapDetectionService gap = mock(GapDetectionService.class);
    private final EraService eraService = mock(EraService.class);
    private final TableExporter daily = exporter("transaction", PartitionStrategy.DAILY);
    private final TableExporter epochTable = exporter("reward", PartitionStrategy.EPOCH);
    private ExportReadinessService service;

    @BeforeEach
    void setUp() {
        when(gap.getExportEndDate()).thenReturn(EXPORT_END);
        when(gap.getCurrentEpoch()).thenReturn(Optional.of(590));
        service = new ExportReadinessService(gap, eraService, new AnalyticsStoreProperties());
    }

    @Test
    void aDayIsReadyUpToTheExportEndDate() {
        assertThat(service.check(daily, PartitionValue.ofDate(EXPORT_END)).isReady()).isTrue();

        PartitionReadiness today = service.check(daily, PartitionValue.ofDate(EXPORT_END.plusDays(2)));
        assertThat(today.status()).isEqualTo(PartitionReadiness.Status.NOT_FINAL);
        assertThat(today.reason()).contains("date=2026-11-16", "2026-11-14");

        assertThat(service.check(daily, PartitionValue.ofDate(LocalDate.of(2099, 1, 1))).isReady()).isFalse();
        verify(daily, never()).preExportValidation(PartitionValue.ofDate(EXPORT_END.plusDays(2)));
    }

    @Test
    void anEpochThatHasNotStartedIsNotReady() {
        assertThat(service.check(epochTable, PartitionValue.ofEpoch(590)).isReady()).isTrue();
        assertThat(service.check(epochTable, PartitionValue.ofEpoch(591)).status())
                .isEqualTo(PartitionReadiness.Status.NOT_FINAL);
        assertThat(service.check(epochTable, PartitionValue.ofEpoch(Integer.MAX_VALUE)).isReady()).isFalse();
    }

    @Test
    void nothingIsReadyBeforeTheFirstBlock() {
        when(gap.getCurrentEpoch()).thenReturn(Optional.empty());

        assertThat(service.check(epochTable, PartitionValue.ofEpoch(0)).isReady()).isFalse();
    }

    @Test
    void negativeEpochsAndMismatchedPartitionsAreInvalid() {
        assertThatThrownBy(() -> service.check(epochTable, PartitionValue.ofEpoch(-1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.check(epochTable, PartitionValue.ofDate(EXPORT_END)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.check(daily, PartitionValue.ofEpoch(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anUnavailableExporterIsNeverReady() {
        when(epochTable.unavailableReason()).thenReturn(Optional.of("adapot is disabled"));

        PartitionReadiness readiness = service.check(epochTable, PartitionValue.ofEpoch(500));
        assertThat(readiness.status()).isEqualTo(PartitionReadiness.Status.UNAVAILABLE);
        assertThat(readiness.reason()).contains("reward", "adapot is disabled");
    }

    @Test
    void theExportersOwnCheckDecidesTheRestAndFailsClosed() {
        when(epochTable.preExportValidation(PartitionValue.ofEpoch(500))).thenReturn(false);
        assertThat(service.check(epochTable, PartitionValue.ofEpoch(500)).status())
                .isEqualTo(PartitionReadiness.Status.DEPENDENCY_PENDING);

        when(epochTable.preExportValidation(PartitionValue.ofEpoch(501))).thenThrow(new RuntimeException("db down"));
        PartitionReadiness error = service.check(epochTable, PartitionValue.ofEpoch(501));
        assertThat(error.status()).isEqualTo(PartitionReadiness.Status.DEPENDENCY_PENDING);
        assertThat(error.reason()).contains("db down");
    }

    @Test
    void aFailingFinalityCheckMeansNotFinal() {
        when(gap.getExportEndDate()).thenThrow(new RuntimeException("connection reset"));

        assertThat(service.check(daily, PartitionValue.ofDate(EXPORT_END)).status())
                .isEqualTo(PartitionReadiness.Status.NOT_FINAL);
    }

    @Test
    void aFinalizedEpochTableWaitsUntilTheEpochsLastSlotIsFinal() {
        TableExporter mir = exporter("mir", PartitionStrategy.EPOCH);
        when(mir.requiresFinalizedEpoch()).thenReturn(true);
        // Epoch 589 is slots [1000, 2000); one slot per second from time 0
        when(eraService.getFirstNonByronEpoch()).thenReturn(Optional.of(0));
        when(eraService.getShelleyAbsoluteSlot(589, 0)).thenReturn(1000L);
        when(eraService.getShelleyAbsoluteSlot(590, 0)).thenReturn(2000L);
        when(eraService.slotAtOrAfterTime(anyLong())).thenAnswer(i -> i.getArgument(0));
        when(gap.getFinalityWindow()).thenReturn(Duration.ofSeconds(500));

        when(gap.getLatestSyncedBlockTime()).thenReturn(Optional.of(Instant.ofEpochSecond(2100)));
        assertThat(service.check(mir, PartitionValue.ofEpoch(589)).status())
                .isEqualTo(PartitionReadiness.Status.NOT_FINAL);
        verify(mir, never()).preExportValidation(any());

        when(gap.getLatestSyncedBlockTime()).thenReturn(Optional.of(Instant.ofEpochSecond(2500)));
        assertThat(service.check(mir, PartitionValue.ofEpoch(589)).isReady()).isTrue();
    }

    private static TableExporter exporter(String name, PartitionStrategy strategy) {
        TableExporter exporter = mock(TableExporter.class);
        when(exporter.getTableName()).thenReturn(name);
        when(exporter.getPartitionStrategy()).thenReturn(strategy);
        when(exporter.unavailableReason()).thenReturn(Optional.empty());
        when(exporter.preExportValidation(any())).thenReturn(true);
        return exporter;
    }
}
