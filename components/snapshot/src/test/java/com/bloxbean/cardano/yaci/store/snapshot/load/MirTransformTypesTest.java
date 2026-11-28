package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.convert.UuidV5Converter;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The transform must produce the target types (mainnet failed with id VARCHAR -> uuid, and with
 * epoch BIGINT -> integer while scans let the {@code epoch=N} directory replace the file's column).
 *
 * <p>One certificate pays many credentials, so the id must cover the credential too (mainnet failed
 * with a duplicate mir_pkey: 208,618 rows from 1,062 certificates).
 */
class MirTransformTypesTest {

    @TempDir
    Path dir;

    /** Target columns from stores/mir V0_1000_1__init.sql. */
    private static final Map<String, String> TARGET = Map.ofEntries(
            Map.entry("id", "uuid"), Map.entry("tx_hash", "varchar(64)"), Map.entry("cert_index", "integer"),
            Map.entry("pot", "varchar(30)"), Map.entry("credential", "varchar(56)"),
            Map.entry("address", "varchar(255)"), Map.entry("amount", "numeric(38)"),
            Map.entry("epoch", "integer"), Map.entry("slot", "bigint"), Map.entry("block_hash", "varchar(64)"),
            Map.entry("block", "bigint"), Map.entry("block_time", "bigint"));

    @Test
    void transformOutputMatchesTheTargetTypes() throws Exception {
        try (Connection duck = DuckDb.open("256MB", dir.resolve("work"), 1)) {
            Path mir = write(duck, "main/mir/epoch=208/f.parquet", """
                    SELECT 'aa'::VARCHAR tx_hash, 0::INTEGER cert_index, 'RESERVES'::VARCHAR pot,
                           'cred'::VARCHAR credential, 'stake1x'::VARCHAR address, 5::DECIMAL(38,0) amount,
                           208::INTEGER epoch, 4924900::BIGINT slot, 'bh'::VARCHAR block_hash,
                           TIMESTAMPTZ '2020-08-08 21:44:51+00' block_time
                    UNION ALL
                    SELECT 'aa', 0, 'RESERVES', 'cred2', 'stake1y', 7, 208, 4924900, 'bh',
                           TIMESTAMPTZ '2020-08-08 21:44:51+00'""");
            Path block = write(duck, "main/block/date=2020-08-08/b.parquet",
                    "SELECT 'bh'::VARCHAR hash, 4500000::BIGINT number");

            var spec = SnapshotSpecRegistry.builtIn().byId("mir").orElseThrow();
            String sql = SqlResources.bindTransform(SqlResources.read(spec.importSpec().selectResource()),
                    DuckDb.readParquet("['" + mir + "']"),
                    Map.of("block", DuckDb.readParquet("['" + block + "']")), 999999999L, 0);

            Map<String, String> produced = new LinkedHashMap<>();
            try (ResultSet rs = duck.createStatement().executeQuery("DESCRIBE (" + sql + ")")) {
                while (rs.next()) {
                    produced.put(rs.getString("column_name"), rs.getString("column_type"));
                }
            }
            assertThat(produced.keySet()).containsExactlyInAnyOrderElementsOf(
                    TARGET.keySet().stream().filter(c -> !c.equals("update_datetime")).toList());
            produced.forEach((column, type) -> assertThat(TypeCompatibility.reject(type, TARGET.get(column)))
                    .as("%s: %s -> %s", column, type, TARGET.get(column)).isNull());

            Map<String, Object> ids = new LinkedHashMap<>();
            try (ResultSet rs = duck.createStatement().executeQuery(
                    "SELECT credential, id, epoch, block FROM (" + sql + ") ORDER BY credential")) {
                while (rs.next()) {
                    ids.put(rs.getString(1), rs.getObject(2));
                    assertThat(rs.getInt(3)).isEqualTo(208);
                    assertThat(rs.getLong(4)).isEqualTo(4500000L);
                }
            }
            assertThat(ids).containsOnlyKeys("cred", "cred2");
            assertThat(ids.get("cred")).isEqualTo(UuidV5Converter.compute("aa\u001f0\u001fcred"));
            assertThat(ids.get("cred2")).isEqualTo(UuidV5Converter.compute("aa\u001f0\u001fcred2"));
        }
    }

    private Path write(Connection duck, String relativePath, String select) throws Exception {
        Path file = dir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        DuckDb.exec(duck, "COPY (" + select + ") TO '" + file + "' (FORMAT parquet)");
        return file;
    }
}
