package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.convert.ConverterRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "SNAPSHOT_IT_JDBC_URL", matches = ".+")
class OwnedPartitionLoadIT {
    @TempDir Path root;

    @Test
    void aBatchReplacesWhateverItsOwnPartitionHeld() throws Exception {
        String schema = "yaci_owned_it_" + UUID.randomUUID().toString().replace("-", "");
        String url = System.getenv("SNAPSHOT_IT_JDBC_URL");
        String user = System.getenv().getOrDefault("SNAPSHOT_IT_USER", "postgres");
        String password = System.getenv().getOrDefault("SNAPSHOT_IT_PASSWORD", "");
        try (Connection pg = DriverManager.getConnection(url, user, password); var st = pg.createStatement()) {
            st.execute("CREATE SCHEMA " + schema);
            try {
                st.execute("CREATE TABLE " + schema + ".epoch_stake (epoch int, address text, amount bigint)"
                        + " PARTITION BY RANGE (epoch)");
                st.execute("CREATE TABLE " + schema + ".epoch_stake_p455 PARTITION OF " + schema
                        + ".epoch_stake FOR VALUES FROM (455) TO (456)");
                st.execute("CREATE TABLE " + schema + ".epoch_stake_p456 PARTITION OF " + schema
                        + ".epoch_stake FOR VALUES FROM (456) TO (457)");
                // Rows a crashed attempt could not have left (it rolls back), and the next epoch's rows.
                st.execute("INSERT INTO " + schema + ".epoch_stake VALUES (455, 'stale', 1), (456, 'other', 2)");
                new ImportJournal(pg, schema).createIfAbsent();

                var spec = SnapshotSpecRegistry.builtIn().byId("epoch-stake").orElseThrow();
                var columns = List.of("epoch", "address", "amount");
                var plan = new ColumnPlan(columns, columns, List.of());
                String select = "(SELECT 455 AS epoch, 'addr' || i AS address, i::BIGINT AS amount FROM range(3) t(i))";
                var loader = new TableLoader(new ConverterRegistry(), root);
                try (var session = DuckPgSession.open(options(url, user, password, schema), root.resolve("spill"), 1)) {
                    for (String snapshotId : List.of("first", "again")) {
                        var batch = ImportBatch.of(snapshotId, spec, List.of(), "main/epoch_stake/epoch=455");
                        assertThat(loader.loadBatch(session, batch, plan, select, snapshotId, schema)).isEqualTo(3);
                    }
                }

                try (var rs = st.executeQuery("SELECT epoch, string_agg(address, ',' ORDER BY address) FROM "
                        + schema + ".epoch_stake GROUP BY epoch ORDER BY epoch")) {
                    rs.next();
                    assertThat(rs.getString(2)).isEqualTo("addr0,addr1,addr2");
                    rs.next();
                    assertThat(rs.getString(2)).isEqualTo("other");
                }
            } finally {
                st.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private ImportOptions options(String url, String user, String password, String schema) {
        return new ImportOptions(root.resolve("manifest.json"), root, root, url, user, password, schema,
                "preprod", 1, 1, 1, 1, "512MB", 0, true, false, List.of(), true, false);
    }
}
