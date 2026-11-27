package com.bloxbean.cardano.yaci.store.analytics.scheduler;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UniversalExportServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 10);

    private final TableExporterRegistry registry = mock(TableExporterRegistry.class);
    private final EraService eraService = mock(EraService.class);
    private final ExportReadinessService readiness = mock(ExportReadinessService.class);
    private final TableExporter daily = mock(TableExporter.class);
    private final TableExporter epochTable = mock(TableExporter.class);
    private UniversalExportService service;

    @BeforeEach
    void setUp() {
        when(daily.getTableName()).thenReturn("transaction");
        when(daily.getPartitionStrategy()).thenReturn(PartitionStrategy.DAILY);
        when(daily.exportForPartition(any())).thenReturn(true);
        when(epochTable.getTableName()).thenReturn("reward");
        when(epochTable.getPartitionStrategy()).thenReturn(PartitionStrategy.EPOCH);
        when(epochTable.exportForPartition(any())).thenReturn(true);
        when(registry.getExporter("transaction")).thenReturn(daily);
        when(registry.getExporter("reward")).thenReturn(epochTable);
        when(registry.getExportableTablesByStrategy(PartitionStrategy.DAILY)).thenReturn(List.of("transaction"));
        when(registry.getExportableTablesByStrategy(PartitionStrategy.EPOCH)).thenReturn(List.of("reward"));
        when(eraService.getFirstNonByronEpoch()).thenReturn(Optional.of(208));
        when(readiness.check(any(), any())).thenReturn(PartitionReadiness.READY);
        when(readiness.checkFinal(any())).thenReturn(PartitionReadiness.READY);
        when(readiness.checkExporter(any(), any())).thenReturn(PartitionReadiness.READY);
        service = new UniversalExportService(registry, eraService, readiness);
    }

    @Test
    void aManualExportOfAPartitionThatIsNotReadyExportsNothing() {
        PartitionReadiness notFinal = PartitionReadiness.notFinal("date=2026-11-16 is not final yet");
        when(readiness.check(daily, PartitionValue.ofDate(DAY))).thenReturn(notFinal);

        UniversalExportService.PartitionExportResult result = service.exportTable("transaction", PartitionValue.ofDate(DAY));

        assertThat(result.outcome()).isEqualTo(UniversalExportService.Outcome.NOT_READY);
        assertThat(result.readiness()).isEqualTo(notFinal);
        verify(daily, never()).exportForPartition(any());
    }

    @Test
    void aManualExportReportsExportedAndFailed() {
        assertThat(service.exportTable("transaction", PartitionValue.ofDate(DAY)).outcome())
                .isEqualTo(UniversalExportService.Outcome.EXPORTED);

        when(daily.exportForPartition(PartitionValue.ofDate(DAY))).thenReturn(false);
        assertThat(service.exportTable("transaction", PartitionValue.ofDate(DAY)).outcome())
                .isEqualTo(UniversalExportService.Outcome.FAILED);
    }

    @Test
    void theDailyRunExportsNothingForADayThatIsNotFinal() {
        when(readiness.checkFinal(PartitionValue.ofDate(DAY))).thenReturn(PartitionReadiness.notFinal("not final"));

        assertThat(service.exportAllDailyTables(DAY)).isZero();
        verify(daily, never()).exportForPartition(any());
    }

    @Test
    void theEpochRunSkipsTablesThatAreNotReady() {
        when(readiness.checkExporter(epochTable, PartitionValue.ofEpoch(500)))
                .thenReturn(PartitionReadiness.dependencyPending("AdaPot job running"));

        assertThat(service.exportAllEpochTables(500)).isZero();
        verify(epochTable, never()).exportForPartition(any());

        assertThat(service.exportAllEpochTables(501)).isEqualTo(1);
        verify(epochTable).exportForPartition(PartitionValue.ofEpoch(501));
    }

    @Test
    @Timeout(10)
    void anEpochRangeEndingAtIntegerMaxValueTerminates() {
        UniversalExportService.RangeExportResult result =
                service.exportEpochRange("reward", Integer.MAX_VALUE - 1, Integer.MAX_VALUE);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.successCount()).isEqualTo(2);
    }

    @Test
    void rangesReportNotReadyAndFailedPartitions() {
        when(readiness.check(epochTable, PartitionValue.ofEpoch(11)))
                .thenReturn(PartitionReadiness.dependencyPending("not ready"));
        when(epochTable.exportForPartition(PartitionValue.ofEpoch(12))).thenReturn(false);

        UniversalExportService.RangeExportResult result = service.exportEpochRange("reward", 10, 12);

        assertThat(result.total()).isEqualTo(3);
        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.notReady()).containsExactly("epoch=11");
        assertThat(result.failed()).containsExactly("epoch=12");
    }

    @Test
    void rejectsReversedAndNegativeRanges() {
        assertThatThrownBy(() -> service.exportEpochRange("reward", 5, 4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.exportEpochRange("reward", -2, 4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.exportDateRange("transaction", DAY, DAY.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(epochTable, never()).exportForPartition(any());
        verify(daily, never()).exportForPartition(any());
    }
}
