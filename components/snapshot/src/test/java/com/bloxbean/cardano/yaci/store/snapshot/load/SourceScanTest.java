package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** An exporter gains a column in a later release; DuckLake keeps the files written before it. */
class SourceScanTest {

    @TempDir
    Path root;

    @Test
    void filesWrittenBeforeAColumnWasAddedReadItAsNull() throws Exception {
        try (var conn = DriverManager.getConnection("jdbc:duckdb:"); var st = conn.createStatement()) {
            st.execute("LOAD ducklake");
            st.execute("ATTACH 'ducklake:" + root.resolve("ducklake.catalog.db") + "' AS lake (DATA_PATH '" + root + "')");
            st.execute("CALL lake.set_option('data_inlining_row_limit', 0)");
            st.execute("CREATE TABLE lake.address_utxo(slot BIGINT, owner_addr VARCHAR, date DATE)");
            st.execute("ALTER TABLE lake.address_utxo SET PARTITIONED BY (date)");
            st.execute("INSERT INTO lake.address_utxo VALUES (1, 'a', '2026-01-01'), (2, 'b', '2026-01-02')");
            st.execute("ALTER TABLE lake.address_utxo ADD COLUMN owner_addr_full VARCHAR");
            st.execute("INSERT INTO lake.address_utxo BY NAME SELECT 3 AS slot, 'c' AS owner_addr,"
                    + " 'full' AS owner_addr_full, DATE '2026-01-03' AS date");
        }

        try (var catalog = DuckLakeCatalog.open(root, root.resolve("work"))) {
            long snapshot = catalog.latestSnapshotId();
            assertThat(catalog.filesPredatingColumns("address_utxo", snapshot)).containsExactly(Map.entry("owner_addr_full", 2L));

            Map<String, String> declared = catalog.columns("address_utxo", snapshot);
            List<DuckLakeFile> files = catalog.files("address_utxo", snapshot);
            assertThat(files).hasSize(3);
            var duck = catalog.connection();

            // Older and newer files together: the column is matched by name
            String all = SourceScan.of(duck, catalog.parquetList(files), declared);
            assertThat(strings(duck, "SELECT coalesce(owner_addr_full, '-') FROM " + all + " ORDER BY slot"))
                    .containsExactly("-", "-", "full");

            // Older files only: the column is added as a typed NULL
            String older = SourceScan.of(duck, catalog.parquetList(files.subList(0, 2)), declared);
            assertThat(strings(duck, "SELECT typeof(owner_addr_full) || ':' || coalesce(owner_addr_full, '-') FROM "
                    + older + " ORDER BY slot")).containsExactly("VARCHAR:-", "VARCHAR:-");

            // Without declared columns the files are read as they are
            assertThat(SourceScan.of(duck, catalog.parquetList(files.subList(0, 2)), null))
                    .doesNotContain("owner_addr_full");
        }
    }

    private static List<String> strings(java.sql.Connection duck, String sql) throws Exception {
        try (var st = duck.createStatement(); var rs = st.executeQuery(sql)) {
            var out = new java.util.ArrayList<String>();
            while (rs.next()) {
                out.add(rs.getString(1));
            }
            return out;
        }
    }
}
