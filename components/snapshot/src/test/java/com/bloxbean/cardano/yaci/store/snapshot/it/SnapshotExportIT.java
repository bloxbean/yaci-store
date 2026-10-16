package com.bloxbean.cardano.yaci.store.snapshot.it;

import com.bloxbean.cardano.yaci.store.snapshot.export.ExportOptions;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.export.SnapshotExporter;
import com.bloxbean.cardano.yaci.store.snapshot.load.TargetTable;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecLoader;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Uses the installed DuckLake extension, alongside the opt-in PostgreSQL import qualification. */
@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class SnapshotExportIT {
    @TempDir Path root;

    private SnapshotTableSpec spec(String name, boolean gating) {
        String yaml = """
                snapshot-table:
                  id: %s
                  spec-version: 1
                  module: blocks
                  kind: CHAIN_DATA
                  restore: IMPORT
                  source:
                    exporter-id: %s
                    ducklake-relation: %s
                    partition: {strategy: DAILY, column: block_time}
                  consistency:
                    cutoff: {type: SLOT_LTE, column: slot}
                    completed-epoch: %s
                  import:
                    target-table: %s
                    mode: DIRECT
                  validation:
                    key: [hash]
                    bounds: [slot]
                """.formatted(name, name, name,
                gating ? "{type: MAX_EPOCH_OFFSET, column: epoch, offset: -1}" : "{type: NONE}", name);
        return new SnapshotSpecLoader().load(yaml.getBytes(StandardCharsets.UTF_8), "test");
    }

    @Test
    void packagesACompleteCatalogButRejectsMissingSparsePartitionEvidence() throws Exception {
        try (var conn = DriverManager.getConnection("jdbc:duckdb:"); var st = conn.createStatement()) {
            st.execute("LOAD ducklake");
            st.execute("ATTACH 'ducklake:" + root.resolve("ducklake.catalog.db")
                    + "' AS lake (DATA_PATH '" + root + "')");
            st.execute("CALL lake.set_option('data_inlining_row_limit', 0)");
            st.execute("CREATE TABLE lake.block(hash VARCHAR, number BIGINT, slot BIGINT, prev_hash VARCHAR,"
                    + "era INTEGER, epoch INTEGER, block_time TIMESTAMPTZ, date DATE)");
            st.execute("ALTER TABLE lake.block SET PARTITIONED BY (date)");
            st.execute("INSERT INTO lake.block VALUES "
                    + "('a',0,1,NULL,7,0,'2026-01-01 12:00:00+00','2026-01-01'),"
                    + "('b',1,2,'a',7,1,'2026-01-02 12:00:00+00','2026-01-02'),"
                    + "('c',2,3,'b',7,2,'2026-01-03 12:00:00+00','2026-01-03')");
            st.execute("CREATE TABLE lake.delegation(hash VARCHAR, slot BIGINT, block_time TIMESTAMPTZ, date DATE)");
            st.execute("ALTER TABLE lake.delegation SET PARTITIONED BY (date)");
            st.execute("INSERT INTO lake.delegation VALUES ('d',1,'2026-01-01 12:00:00+00','2026-01-01')");
        }
        try (var catalog = DuckLakeCatalog.open(root, root.resolve("work"))) {
            assertThat(catalog.firstNonByronEpoch(catalog.latestSnapshotId())).hasValue(0);
        }
        var registry = mock(SnapshotSpecRegistry.class);
        List<SnapshotTableSpec> specs = List.of(spec("block", true), spec("delegation", false));
        when(registry.importedTables()).thenReturn(specs);
        when(registry.all()).thenReturn(specs);
        Map<String, Long> block = Map.of("date=2026-01-01", 1L, "date=2026-01-02", 1L);
        var complete = options(Map.of("block", block, "delegation",
                Map.of("date=2026-01-01", 1L, "date=2026-01-02", 0L)));
        var exporter = new SnapshotExporter(registry);
        var report = exporter.inspect(complete);
        assertThat(report.blockers()).isEmpty();
        // Planned against the producing release's tables before anything is packaged.
        String blockColumns = "hash varchar(64), number bigint, slot bigint, prev_hash varchar(64), era integer,"
                + " epoch integer, block_time timestamp with time zone, date date";
        assertThat(exporter.inspect(withTargets(complete, target("block", blockColumns))).blockers()).isEmpty();
        assertThat(exporter.inspect(withTargets(complete,
                        target("block", blockColumns.replace("hash varchar(64)", "hash integer")))).blockers())
                .anySatisfy(b -> assertThat(b).startsWith("[block]").contains("column 'hash'"));
        assertThat(report.point().epoch()).isEqualTo(1);
        var result = exporter.export(complete, null);
        assertThat(result.manifest().totalRows()).isEqualTo(3);
        assertThat(result.manifestPath()).exists();
        var incomplete = options(Map.of("block", block, "delegation", Map.of("date=2026-01-01", 1L)));
        var blocked = exporter.inspect(incomplete);
        assertThat(blocked.blockers())
                .anyMatch(p -> p.contains("delegation") && p.contains("date=2026-01-02"));
        assertThat(blocked.incompletePartitions())
                .containsEntry("delegation", List.of("date=2026-01-02 has no completed export record"));
        assertThatThrownBy(() -> exporter.export(incomplete, null)).hasMessageContaining("incomplete export")
                .isInstanceOfSatisfying(SnapshotExporter.BlockedException.class,
                        e -> assertThat(e.report().incompletePartitions()).containsKey("delegation"));
    }

    @Test
    void derivesEpochStartFromPinnedBlocksRatherThanByronOrLaterExportJournalEntries() throws Exception {
        try (var conn = DriverManager.getConnection("jdbc:duckdb:"); var st = conn.createStatement()) {
            st.execute("LOAD ducklake");
            st.execute("ATTACH 'ducklake:" + root.resolve("ducklake.catalog.db")
                    + "' AS lake (DATA_PATH '" + root + "')");
            st.execute("CALL lake.set_option('data_inlining_row_limit', 0)");
            st.execute("CREATE TABLE lake.block(number BIGINT, era INTEGER, epoch INTEGER)");
            st.execute("INSERT INTO lake.block VALUES (0,1,0), (1,1,3)");
        }
        long byronSnapshot;
        try (var catalog = DuckLakeCatalog.open(root, root.resolve("work"))) {
            byronSnapshot = catalog.latestSnapshotId();
            assertThat(catalog.firstNonByronEpoch(byronSnapshot)).isEmpty();
        }
        try (var conn = DriverManager.getConnection("jdbc:duckdb:"); var st = conn.createStatement()) {
            st.execute("LOAD ducklake");
            st.execute("ATTACH 'ducklake:" + root.resolve("ducklake.catalog.db") + "' AS lake");
            st.execute("INSERT INTO lake.block VALUES (2,2,4), (3,6,5)");
        }
        try (var catalog = DuckLakeCatalog.open(root, root.resolve("work"))) {
            assertThat(catalog.firstNonByronEpoch(catalog.latestSnapshotId())).hasValue(4);
            assertThat(catalog.firstNonByronEpoch(byronSnapshot)).isEmpty();
        }
    }

    private static ExportOptions withTargets(ExportOptions o, TargetTable target) {
        return new ExportOptions(o.dataDir(), o.workDir(), o.outputDir(), o.network(), o.protocolMagic(),
                o.genesisHash(), o.partSizeBytes(), o.targetEpoch(), o.minConfirmations(), o.allowIncomplete(),
                o.unsigned(), o.yaciStoreVersion(), o.modules(), o.pruningSettings(), o.schemaFingerprint(),
                o.flywayFingerprint(), o.completedPartitions(), o.tableSchemaFingerprints(), o.memoryLimit(),
                Map.of(target.name(), target));
    }

    private static TargetTable target(String name, String declaration) {
        Map<String, String> columns = new LinkedHashMap<>();
        Map<String, Boolean> nullable = new LinkedHashMap<>();
        for (String column : declaration.split(",")) {
            String[] parts = column.trim().split(" ", 2);
            columns.put(parts[0], parts[1]);
            nullable.put(parts[0], true);
        }
        return new TargetTable("s", name, columns, nullable, Map.of(), List.of(), false);
    }

    private ExportOptions options(Map<String, Map<String, Long>> completed) {
        return new ExportOptions(root, root.resolve("work"), root.resolve("out"), "preprod", 1,
                null, 1024 * 1024, 1, 0, false, true, "test", List.of(), Map.of(),
                "schema", "flyway", completed);
    }
}
