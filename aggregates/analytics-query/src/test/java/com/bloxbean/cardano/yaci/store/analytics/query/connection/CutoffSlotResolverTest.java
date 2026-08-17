package com.bloxbean.cardano.yaci.store.analytics.query.connection;

import com.bloxbean.cardano.yaci.store.analytics.exporter.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporter;
import com.bloxbean.cardano.yaci.store.analytics.exporter.TableExporterRegistry;
import com.bloxbean.cardano.yaci.store.analytics.state.ExportStateService;
import com.bloxbean.cardano.yaci.store.core.service.EraService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
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
    }
}
