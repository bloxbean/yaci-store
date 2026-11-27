package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** A table that is exported but has no rows, e.g. the Conway governance tables of a pre-Conway chain. */
class EmptyExportBoundsTest {
    private final SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();

    @Test
    void completedEmptyEpochPartitionsProveTheBounds() {
        var spec = registry.byId("drep-dist").orElseThrow();
        assertThat(ConsistencyPointSelector.emptyExportBounds(spec, "epoch",
                Map.of("epoch=208", 0L, "epoch=209", 0L, "epoch=364", 0L)))
                .hasValueSatisfying(b -> assertThat(b).containsExactly(208L, 364L));
    }

    @Test
    void aJournalClaimingRowsTheCatalogLacksIsNotAnEmptyExport() {
        var spec = registry.byId("drep-dist").orElseThrow();
        assertThat(ConsistencyPointSelector.emptyExportBounds(spec, "epoch",
                Map.of("epoch=508", 0L, "epoch=509", 12L))).isEmpty();
    }

    @Test
    void noJournalOrAnotherColumnProvesNothing() {
        var spec = registry.byId("drep-dist").orElseThrow();
        assertThat(ConsistencyPointSelector.emptyExportBounds(spec, "epoch", Map.of())).isEmpty();
        assertThat(ConsistencyPointSelector.emptyExportBounds(spec, "slot", Map.of("epoch=1", 0L))).isEmpty();
    }

    @Test
    void dailyPartitionedTablesNeverQualify() {
        var spec = registry.byId("block").orElseThrow();
        assertThat(ConsistencyPointSelector.emptyExportBounds(spec, "epoch", Map.of("date=2026-01-01", 0L)))
                .isEmpty();
    }
}
