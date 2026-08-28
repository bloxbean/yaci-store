package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CutoffSlotResolverTest {

    @Test
    void cutoffStopsBeforeFirstGapInsteadOfUsingMaximumCompletedPartition() {
        ExportStateService stateService = mock(ExportStateService.class);
        when(stateService.getCompletedPartitions("reward"))
                .thenReturn(Set.of("epoch=10", "epoch=11", "epoch=13"));

        TableExporter exporter = mock(TableExporter.class);
        when(exporter.getPartitionStrategy()).thenReturn(PartitionStrategy.EPOCH);
        TableExporterRegistry registry = mock(TableExporterRegistry.class);
        when(registry.hasExporter("reward")).thenReturn(true);
        when(registry.getExporter("reward")).thenReturn(exporter);

        EraService eraService = mock(EraService.class);
        when(eraService.getShelleyAbsoluteSlot(anyInt(), eq(0)))
                .thenAnswer(invocation -> invocation.<Integer>getArgument(0) * 1_000L);

        CutoffSlotResolver resolver = new CutoffSlotResolver(stateService, eraService, registry);

        // Epoch 11 ends at the start of epoch 12. Epoch 13 must not move the boundary.
        assertEquals(11_999L, resolver.getCutoffSlot("reward"));
        // Both units are exposed: EPOCH tables are federated on the epoch column
        assertEquals(new CutoffSlotResolver.Cutoff(11_999L, 11L, 10_000L, 10L),
                resolver.getCutoff("reward"));
    }

    @Test
    void dailyCutoffCarriesNoEpoch() {
        ExportStateService stateService = mock(ExportStateService.class);
        when(stateService.getCompletedPartitions("block")).thenReturn(Set.of("date=2024-01-01", "date=2024-01-02"));
        TableExporter exporter = mock(TableExporter.class);
        when(exporter.getPartitionStrategy()).thenReturn(PartitionStrategy.DAILY);
        TableExporterRegistry registry = mock(TableExporterRegistry.class);
        when(registry.hasExporter("block")).thenReturn(true);
        when(registry.getExporter("block")).thenReturn(exporter);
        EraService eraService = mock(EraService.class);
        when(eraService.slotFromTime(anyLong())).thenAnswer(inv -> inv.<Long>getArgument(0) - 1_700_000_000L);

        CutoffSlotResolver resolver = new CutoffSlotResolver(stateService, eraService, registry);
        CutoffSlotResolver.Cutoff cutoff = resolver.getCutoff("block");
        assertEquals(-1L, cutoff.epoch());
        assertTrue(cutoff.slot() > 0);
        assertTrue(cutoff.startSlot() < cutoff.slot());
        assertEquals(cutoff.slot(), resolver.getCutoffSlot("block"));
    }

    @Test
    void refreshKeepsLastRangeWhenExportStateReadFails() {
        ExportStateService stateService = mock(ExportStateService.class);
        when(stateService.getCompletedPartitions("reward"))
                .thenReturn(Set.of("epoch=10", "epoch=11"))
                .thenThrow(new RuntimeException("database temporarily unavailable"));
        TableExporter exporter = mock(TableExporter.class);
        when(exporter.getPartitionStrategy()).thenReturn(PartitionStrategy.EPOCH);
        TableExporterRegistry registry = mock(TableExporterRegistry.class);
        when(registry.hasExporter("reward")).thenReturn(true);
        when(registry.getExporter("reward")).thenReturn(exporter);
        EraService eraService = mock(EraService.class);
        when(eraService.getShelleyAbsoluteSlot(anyInt(), eq(0)))
                .thenAnswer(invocation -> invocation.<Integer>getArgument(0) * 1_000L);

        CutoffSlotResolver resolver = new CutoffSlotResolver(stateService, eraService, registry);
        CutoffSlotResolver.Cutoff knownGood = resolver.getCutoff("reward");
        resolver.refresh();

        assertEquals(knownGood, resolver.getCutoff("reward"));
    }
}
