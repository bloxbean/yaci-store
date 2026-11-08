package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.Retention;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The pruned tier over a real DuckLake catalog: blocks 0 to 120 at 20 slots each, the point at block
 * 99 and a 10-block window, so inputs spent from block 89 on are kept.
 *
 * <ul>
 *   <li>outputs 0-19 are spent at blocks 60-79, before the window: dropped from the pruned tier, with their inputs;</li>
 *   <li>outputs 20-29 are spent at blocks 90-99, inside the window: kept, with their inputs;</li>
 *   <li>outputs 30-34 are spent at block 110, after the point: kept, their inputs cut off;</li>
 *   <li>outputs 35-49 are unspent: kept.</li>
 * </ul>
 */
@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class TieredExportIT {

    @TempDir
    Path root;

    private static final int POINT_BLOCK = 99;
    private static final int SAFE_BLOCKS = 10;

    @Test
    void currentTierIsWhatAPruningNodeHoldsAtThePoint() throws Exception {
        splits(true);
    }

    @Test
    void findsTheOutputsWithoutAssetIndexToo() throws Exception {
        splits(false);
    }

    private void splits(boolean assetIndex) throws Exception {
        try (var conn = DriverManager.getConnection("jdbc:duckdb:"); var st = conn.createStatement()) {
            st.execute("LOAD ducklake");
            st.execute("ATTACH 'ducklake:" + root.resolve("ducklake.catalog.db") + "' AS lake (DATA_PATH '" + root + "')");
            st.execute("CALL lake.set_option('data_inlining_row_limit', 0)");
            st.execute("CREATE TABLE lake.block(number BIGINT, slot BIGINT, date DATE)");
            st.execute("ALTER TABLE lake.block SET PARTITIONED BY (date)");
            st.execute("INSERT INTO lake.block SELECT n, n * 20, DATE '2026-01-01' + CAST(n / 40 AS INTEGER) FROM range(0, 121) t(n)");

            st.execute("CREATE TABLE lake.address_utxo(tx_hash VARCHAR, output_index SMALLINT, asset_unit VARCHAR,"
                    + " quantity DECIMAL(38,0), epoch INTEGER, slot BIGINT, block_time TIMESTAMPTZ, date DATE"
                    + (assetIndex ? ", asset_index BIGINT" : "") + ")");
            st.execute("ALTER TABLE lake.address_utxo SET PARTITIONED BY (date)");
            st.execute("INSERT INTO lake.address_utxo SELECT printf('%064x', n), 0, asset, 1, CAST(n / 10 AS INTEGER),"
                    + " n * 20, to_timestamp(1700000000 + n * 20), DATE '2026-01-01' + CAST(n / 40 AS INTEGER)"
                    + (assetIndex ? ", CASE asset WHEN 'lovelace' THEN 1 ELSE 2 END" : "")
                    + " FROM range(0, 50) t(n), (SELECT unnest(['lovelace', 'tok']) AS asset)");

            st.execute("CREATE TABLE lake.tx_input(tx_hash VARCHAR, output_index SMALLINT, spent_tx_hash VARCHAR,"
                    + " spent_at_slot BIGINT, spent_at_block BIGINT, spent_epoch INTEGER, spent_block_time TIMESTAMPTZ, date DATE)");
            st.execute("ALTER TABLE lake.tx_input SET PARTITIONED BY (date)");
            st.execute("INSERT INTO lake.tx_input SELECT printf('%064x', n), 0, printf('%064x', n + 1000), b * 20, b,"
                    + " CAST(b / 10 AS INTEGER), to_timestamp(1700000000 + b * 20), DATE '2026-01-01' + CAST(b / 40 AS INTEGER)"
                    + " FROM (SELECT n, CASE WHEN n < 20 THEN n + 60 WHEN n < 30 THEN n + 70 ELSE 110 END AS b"
                    + "       FROM range(0, 35) t(n))");

            st.execute("CREATE TABLE lake.epoch_stake(epoch INTEGER, address VARCHAR, amount DECIMAL(38,0), pool_id VARCHAR,"
                    + " delegation_epoch INTEGER, active_epoch INTEGER)");
            st.execute("ALTER TABLE lake.epoch_stake SET PARTITIONED BY (epoch)");
            for (int e = 0; e <= 9; e++) {
                st.execute("INSERT INTO lake.epoch_stake SELECT " + e + ", 'stake' || n, 1, 'pool', " + e + ", " + e
                        + " FROM range(0, 3) t(n)");
            }
        }

        var registry = SnapshotSpecRegistry.builtIn();
        var point = new ConsistencyPoint("preprod", 1, 9, POINT_BLOCK * 20L, POINT_BLOCK, "aa".repeat(32), "bb".repeat(32),
                7, 1700000000L + POINT_BLOCK * 20L, 1);
        try (var catalog = DuckLakeCatalog.open(root, root.resolve("work"))) {
            long snapshot = catalog.latestSnapshotId();
            var planner = new ExportPlanner(catalog);
            List<TablePlan> plans = List.of(
                    planner.plan(registry.byId("address-utxo").orElseThrow(), snapshot, 9, point.slot()),
                    planner.plan(registry.byId("tx-input").orElseThrow(), snapshot, 9, point.slot()),
                    planner.plan(registry.byId("epoch-stake").orElseThrow(), snapshot, 9, point.slot()));
            assertThat(plans.get(0).rowCount()).isEqualTo(50);

            var result = new TieredExport(catalog, root.resolve("work"), "512MB", null)
                    .apply(plans, point, snapshot, SAFE_BLOCKS, 4);

            // address_utxo: the pruned copy first, then every original file, full-only and unchanged.
            TablePlan utxo = result.plans().get(0);
            TieredExport.Tiers utxoTiers = result.tiers().get("address-utxo");
            assertThat(utxo.rowCount()).isEqualTo(50);
            assertThat(utxoTiers.split().key()).isEqualTo(Retention.UTXO_SPENT_SAFE_BLOCKS);
            assertThat(utxoTiers.split().value()).isEqualTo("10");
            assertThat(utxoTiers.split().rowCount()).isEqualTo(30);
            List<DuckLakeFile> utxoCopy = tier(utxo, utxoTiers, SnapshotManifest.FileEntry.TIER_PRUNED);
            assertThat(keys(result.rewritten(), utxoCopy, "tx_hash")).containsExactlyElementsOf(hashes(20, 50));
            assertThat(rows(utxoCopy)).isEqualTo(60);
            assertThat(utxoCopy).allMatch(f -> f.relativePath().startsWith("main/address_utxo/date=")
                    && result.rewritten().containsKey(f.relativePath()));
            assertThat(tier(utxo, utxoTiers, SnapshotManifest.FileEntry.TIER_FULL)).isEqualTo(plans.get(0).files());
            assertThat(utxo.files().subList(0, utxoCopy.size())).isEqualTo(utxoCopy);
            assertThat(utxoTiers.split().bounds()).containsEntry("slot.min", Long.toString(20 * 20L));

            // tx_input: the inputs spent within the window, copied; the originals full-only.
            TablePlan inputs = result.plans().get(1);
            TieredExport.Tiers inputTiers = result.tiers().get("tx-input");
            assertThat(inputs.rowCount()).isEqualTo(30);
            assertThat(inputTiers.split().rowCount()).isEqualTo(10);
            List<DuckLakeFile> inputCopy = tier(inputs, inputTiers, SnapshotManifest.FileEntry.TIER_PRUNED);
            assertThat(keys(result.rewritten(), inputCopy, "tx_hash")).containsExactlyElementsOf(hashes(20, 30));
            assertThat(tier(inputs, inputTiers, SnapshotManifest.FileEntry.TIER_FULL)).isEqualTo(plans.get(1).files());
            assertThat(inputTiers.split().bounds()).containsEntry("spent_at_slot.min", Long.toString(90 * 20L))
                    .containsEntry("spent_at_slot.max", Long.toString(99 * 20L));

            // epoch_stake: epochs before the newest 4 full-only, nothing rewritten.
            TablePlan stake = result.plans().get(2);
            TieredExport.Tiers stakeTiers = result.tiers().get("epoch-stake");
            assertThat(stake.rowCount()).isEqualTo(30);
            assertThat(stakeTiers.split()).isNull();
            assertThat(tier(stake, stakeTiers, null)).allMatch(f -> f.relativePath().matches(".*epoch=[6-9]/.*"));
            assertThat(tier(stake, stakeTiers, SnapshotManifest.FileEntry.TIER_FULL)).hasSize(6)
                    .allMatch(f -> f.relativePath().matches(".*epoch=[0-5]/.*"));
            assertThat(stake.files()).noneMatch(f -> result.rewritten().containsKey(f.relativePath()));

            assertThat(Files.exists(root.resolve("work/tiers/.spill"))).isFalse();
        }
    }

    private static List<DuckLakeFile> tier(TablePlan plan, TieredExport.Tiers tiers, String tier) {
        return plan.files().stream().filter(f -> java.util.Objects.equals(tiers.of(f.relativePath()), tier)).toList();
    }

    private static long rows(List<DuckLakeFile> files) {
        return files.stream().mapToLong(DuckLakeFile::rowCount).sum();
    }

    private static List<String> hashes(int from, int to) {
        return java.util.stream.IntStream.range(from, to).mapToObj(n -> String.format("%064x", n)).toList();
    }

    private List<String> keys(Map<String, Path> rewritten, List<DuckLakeFile> list, String column) throws Exception {
        String files = list.stream().map(f -> Identifiers.literal(rewritten.getOrDefault(f.relativePath(),
                        root.resolve(f.relativePath())).toString()))
                .collect(Collectors.joining(", ", "[", "]"));
        try (var conn = DriverManager.getConnection("jdbc:duckdb:")) {
            return DuckDb.query(conn, "SELECT DISTINCT " + column + " FROM " + DuckDb.readParquet(files) + " ORDER BY 1", rs -> {
                List<String> out = new java.util.ArrayList<>();
                try {
                    while (rs.next()) {
                        out.add(rs.getString(1));
                    }
                } catch (java.sql.SQLException e) {
                    throw new IllegalStateException(e);
                }
                return out;
            });
        }
    }
}
