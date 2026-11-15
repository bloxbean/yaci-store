package com.bloxbean.cardano.yaci.store.analytics.admin;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionValue;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.writer.DuckLakeWriterService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PartitionRepairServiceTest {

    private static final LocalDate DAY = LocalDate.of(2024, 9, 1);

    private final TableExporterRegistry registry = mock(TableExporterRegistry.class);
    private final ExportStateAdminService adminService = mock(ExportStateAdminService.class);
    private final EraService eraService = mock(EraService.class);
    private final DuckLakeWriterService writer = mock(DuckLakeWriterService.class);
    private final TableExporter exporter = mock(TableExporter.class);
    private PartitionRepairService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(registry.getExporter("stake_address_balance_snapshot")).thenReturn(exporter);
        when(exporter.getPartitionStrategy()).thenReturn(PartitionStrategy.DAILY);
        // One slot per second from time 0
        when(eraService.slotAtOrAfterTime(anyLong())).thenAnswer(i -> i.getArgument(0));
        ObjectProvider<DuckLakeWriterService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(writer);
        service = new PartitionRepairService(registry, adminService, eraService, provider);
    }

    @Test
    void reExportsEveryDayThenPurgesTheEpochZeroPartitionForTheRepairedSlots() {
        when(exporter.exportForPartition(any())).thenReturn(true);
        var epochZero = new DuckLakeWriterService.EpochZeroPartition(1, 1725227090L, 1725227090L, true);
        long start = DAY.atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        long end = DAY.plusDays(2).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
        when(writer.purgeEpochZeroPartition("stake_address_balance_snapshot", start, end)).thenReturn(epochZero);

        PartitionRepairService.RepairResult result =
                service.repairDateRange("stake_address_balance_snapshot", DAY, DAY.plusDays(1));

        InOrder order = inOrder(adminService, exporter, writer);
        for (LocalDate day : List.of(DAY, DAY.plusDays(1))) {
            order.verify(adminService).resetExportState("stake_address_balance_snapshot", "date=" + day);
            order.verify(exporter).exportForPartition(PartitionValue.ofDate(day));
        }
        order.verify(writer).purgeEpochZeroPartition("stake_address_balance_snapshot", start, end);
        assertThat(result).isEqualTo(new PartitionRepairService.RepairResult("stake_address_balance_snapshot",
                DAY, DAY.plusDays(1), 2, 2, List.of(), epochZero));
    }

    @Test
    void keepsTheEpochZeroPartitionWhenADayFails() {
        when(exporter.exportForPartition(PartitionValue.ofDate(DAY))).thenReturn(true);
        when(exporter.exportForPartition(PartitionValue.ofDate(DAY.plusDays(1)))).thenReturn(false);

        PartitionRepairService.RepairResult result =
                service.repairDateRange("stake_address_balance_snapshot", DAY, DAY.plusDays(1));

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failedDates()).containsExactly(DAY.plusDays(1));
        assertThat(result.epochZero()).isNull();
        verify(writer, never()).purgeEpochZeroPartition(anyString(), anyLong(), anyLong());
    }

    @Test
    void rejectsEpochTablesAndReversedRanges() {
        assertThatThrownBy(() -> service.repairDateRange("stake_address_balance_snapshot", DAY, DAY.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);

        TableExporter epochExporter = mock(TableExporter.class);
        when(epochExporter.getPartitionStrategy()).thenReturn(PartitionStrategy.EPOCH);
        when(registry.getExporter("reward")).thenReturn(epochExporter);
        assertThatThrownBy(() -> service.repairDateRange("reward", DAY, DAY))
                .isInstanceOf(IllegalArgumentException.class);
        verify(epochExporter, never()).exportForPartition(any());
    }
}
