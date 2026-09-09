package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExportCoverageTest {
    private final SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();
    private final LocalDate start = LocalDate.of(2026, 1, 1);

    @Test
    void sparseTableNeedsCompletionEvidenceEvenWhenItsLatestRowIsOld() {
        var spec = registry.byId("pool-registration").orElseThrow();
        var files = List.of(new DuckLakeFile("main/pool_registration/date=2026-01-01/a.parquet", 3, 100));
        assertThat(ExportCoverage.check(spec, files, Map.of("date=2026-01-01", 3L),
                start, start.plusDays(1), 10, 100)).anyMatch(p -> p.contains("date=2026-01-02"));
        assertThat(ExportCoverage.check(spec, files,
                Map.of("date=2026-01-01", 3L, "date=2026-01-02", 0L),
                start, start.plusDays(1), 10, 100)).isEmpty();
    }

    @Test
    void detectsInteriorGapsAndCatalogRowsMissingFromACompletedExport() {
        var spec = registry.byId("block").orElseThrow();
        var files = List.of(new DuckLakeFile("main/block/date=2026-01-01/a.parquet", 3, 100),
                new DuckLakeFile("main/block/date=2026-01-03/b.parquet", 4, 100));
        assertThat(ExportCoverage.check(spec, files,
                Map.of("date=2026-01-01", 5L, "date=2026-01-03", 4L),
                start, start.plusDays(2), 10, 100))
                .anyMatch(p -> p.contains("date=2026-01-02") && p.contains("export journal records 5"));
    }

    @Test
    void epochCoverageUsesTheRewardOffsetAndIncludesEmptyEarlyEpochs() {
        var spec = registry.byId("reward").orElseThrow();
        assertThat(ExportCoverage.check(spec, List.of(), Map.of("epoch=0", 0L, "epoch=1", 0L, "epoch=2", 0L),
                start, start, 4, 100)).isEmpty();
        assertThat(ExportCoverage.check(spec, List.of(), Map.of("epoch=0", 0L, "epoch=2", 0L),
                start, start, 4, 100)).anyMatch(p -> p.contains("epoch=1"));
    }
}
