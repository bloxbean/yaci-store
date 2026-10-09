package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.convert.ConverterRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class UtxoBatchLoaderIT {
    @TempDir Path root;
    private static final int OUTPUTS = 50_000;

    @Test
    void lowMemoryMatchesUnsplitResultAndRetryRollsBackEarlierSlices() throws Exception {
        String schema = "yaci_utxo_memory_it_" + UUID.randomUUID().toString().replace("-", "");
        String url = System.getenv("SNAPSHOT_IT_JDBC_URL");
        String user = System.getenv().getOrDefault("SNAPSHOT_IT_USER", "postgres");
        String password = System.getenv().getOrDefault("SNAPSHOT_IT_PASSWORD", "");
        try (Connection pg = DriverManager.getConnection(url, user, password); var st = pg.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
            try {
                st.execute("CREATE TABLE " + schema + ".address_utxo (tx_hash text, output_index smallint,"
                        + " amounts jsonb, inline_datum text, lovelace_amount bigint, block bigint, slot bigint, tx_index int,"
                        + " PRIMARY KEY(tx_hash, output_index))");
                new ImportJournal(pg, schema).createIfAbsent();
                var files = fixture();
                var spec = SnapshotSpecRegistry.builtIn().byId("address-utxo").orElseThrow();
                var batch = ImportBatch.of("memory-test", spec, files, "date=2026-01-01");
                var block = file("block.parquet", 1);
                var deps = Map.of("block", List.of(block));
                var columns = List.of("tx_hash", "output_index", "amounts", "inline_datum", "lovelace_amount", "block", "slot");
                var plan = new ColumnPlan(columns, columns, List.of());
                var manifest = mock(SnapshotManifest.class, RETURNS_DEEP_STUBS);
                when(manifest.snapshotId()).thenReturn("memory-test");
                when(manifest.point().slot()).thenReturn((long) OUTPUTS - 1);
                var loader = new TableLoader(new ConverterRegistry(), root);
                var options = options(url, user, password, schema, "4GB");
                String baseline;
                try (var session = DuckPgSession.open(options, root.resolve("large-spill"), 2)) {
                    String select = loader.sourceSelect(spec, batch, OUTPUTS - 1, 0, deps);
                    assertThat(loader.loadBatch(session, batch, plan, select, "memory-test", schema)).isEqualTo(OUTPUTS);
                    baseline = digest(pg, schema);
                }
                st.execute("TRUNCATE " + schema + ".address_utxo, " + schema + "." + ImportJournal.BATCH_TABLE);
                var attempts = new ArrayList<Integer>();
                var retryingLoader = new TableLoader(new ConverterRegistry(), root) {
                    @Override
                    long loadBatch(DuckPgSession session, ImportBatch b, ColumnPlan p, Iterable<String> selects,
                                   String snapshotId, String targetSchema) throws SQLException {
                        var queries = new ArrayList<String>();
                        selects.forEach(queries::add);
                        attempts.add(queries.size());
                        if (attempts.size() == 1) {
                            // Execute real PG writes in the first slice, then force a DuckDB failure.
                            // The normal transaction code must roll those writes back before retry.
                            queries.set(1, "(SELECT error('Out of Memory Error: injected after first slice') AS tx_hash,"
                                    + " 0 AS output_index, '[]' AS amounts, NULL AS inline_datum,"
                                    + " 0 AS lovelace_amount, 0 AS block, 0 AS slot)");
                        }
                        return super.loadBatch(session, b, p, queries, snapshotId, targetSchema);
                    }
                };
                try (var session = DuckPgSession.open(options(url, user, password, schema, "128MB"),
                        root.resolve("small-spill"), 1)) {
                    assertThat(UtxoBatchLoader.load(retryingLoader, session, batch, plan, manifest, deps, schema))
                            .isEqualTo(OUTPUTS);
                }
                assertThat(attempts.size()).isGreaterThanOrEqualTo(2);
                assertThat(attempts.get(1)).isEqualTo(attempts.getFirst() * 2);
                assertThat(digest(pg, schema)).isEqualTo(baseline);
                assertThat(new ImportJournal(pg, schema).completedBatchIds("memory-test")).containsExactly(batch.batchId());
                assertThat(new ImportJournal(pg, schema).rowsPerTable("memory-test")).containsEntry("address_utxo", (long) OUTPUTS);
            } finally {
                st.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private ImportOptions options(String url, String user, String password, String schema, String memory) {
        return new ImportOptions(root.resolve("manifest.json"), root, root, url, user, password, schema,
                "preprod", 1, 1, 1, 1, memory, 0, true, false, List.of(), true, false);
    }

    private String digest(Connection pg, String schema) throws SQLException {
        try (var st = pg.createStatement(); var rs = st.executeQuery("SELECT md5(string_agg(md5(u::text), ''"
                + " ORDER BY tx_hash,output_index)) FROM " + schema + ".address_utxo u")) {
            rs.next(); return rs.getString(1);
        }
    }

    private SnapshotManifest.FileEntry file(String name, long rows) throws Exception {
        return new SnapshotManifest.FileEntry(name, Files.size(root.resolve(name)), name, rows, "date=2026-01-01");
    }

    private List<SnapshotManifest.FileEntry> fixture() throws Exception {
        var files = new ArrayList<SnapshotManifest.FileEntry>();
        try (var duck = DriverManager.getConnection("jdbc:duckdb:"); var st = duck.createStatement()) {
            st.execute("COPY (SELECT 'blockhash' AS hash, 1::BIGINT AS number) TO '"
                    + root.resolve("block.parquet") + "' (FORMAT PARQUET)");
            // Split each output's four assets across files: slicing by file would lose amounts.
            for (int part = 0; part < 2; part++) {
                String name = "assets-" + part + ".parquet";
                st.execute("COPY (SELECT printf('%064x',n) AS tx_hash, 0::SMALLINT AS output_index,"
                        + " CASE WHEN a=0 THEN 'lovelace' ELSE repeat('a',56)||printf('%02x',a) END AS asset_unit,"
                        + " CASE WHEN a=0 THEN NULL ELSE repeat('a',56) END AS policy_id,"
                        + " CASE WHEN a=0 THEN 'lovelace' ELSE printf('%02x',a) END AS asset_name,"
                        + " (100+n+a)::DECIMAL(38,0) AS quantity,"
                        + " 'owner' AS owner_addr, NULL::VARCHAR AS owner_stake_addr,"
                        + " NULL::VARCHAR AS owner_payment_credential, NULL::VARCHAR AS owner_stake_credential,"
                        + " repeat(md5(n::VARCHAR),16) AS inline_datum, NULL::VARCHAR AS data_hash,"
                        + " NULL::VARCHAR AS script_ref, NULL::VARCHAR AS reference_script_hash,"
                        + " false AS is_collateral_return, 0 AS epoch, n::BIGINT AS slot,"
                        + " 'blockhash' AS block_hash, to_timestamp(n) AS block_time, (n % 7)::INTEGER AS tx_index"
                        + " FROM range(" + (OUTPUTS + 10) + ") t(n), range(" + (part * 2) + "," + (part * 2 + 2)
                        + ") assets(a)) TO '" + root.resolve(name) + "' (FORMAT PARQUET)");
                files.add(file(name, (OUTPUTS + 10L) * 2));
            }
        }
        return files;
    }
}
