package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.convert.ConverterRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.ImportMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every built-in SQL transform, run over synthesized Parquet laid out the way DuckLake writes it, in
 * hive-style {@code epoch=N} and {@code date=D} directories, and planned against its target.
 *
 * <p>This covers every SQL spec, not only those whose tables happen to have rows on preprod: mir
 * has none there, which is how its type mismatch reached mainnet. Each file's partition column
 * deliberately disagrees with its directory, so a scan that let the directory value through would
 * change both the type and the value.
 *
 * <p>The source columns are the DuckLake types the preprod export recorded, and the targets are the
 * migrated PostgreSQL definitions. A new SQL spec fails here until both are added.
 */
class BuiltInSqlTransformsTest {

    private static final long CUT_SLOT = 1000;
    private static final int COMPLETED_EPOCH = 208;

    /** Recorded DuckLake column types, by source relation. */
    private static final Map<String, Map<String, String>> SOURCES = Map.of(
            "block", columns("hash varchar, number int64, slot int64, epoch int32, era int16,"
                    + " block_time timestamptz, date date"),
            "mir", columns("tx_hash varchar, cert_index int32, pot varchar, credential varchar,"
                    + " address varchar, amount decimal(38,0), epoch int32, slot int64, block_hash varchar,"
                    + " block_time timestamptz"),
            "pool_registration", columns("tx_hash varchar, cert_index int32, tx_index int32,"
                    + " pool_id varchar, vrf_key_hash varchar, pledge decimal(20,0), cost decimal(20,0),"
                    + " margin float64, margin_numerator decimal(20,0), margin_denominator decimal(20,0),"
                    + " reward_account varchar, pool_owners varchar, relays varchar, metadata_url varchar,"
                    + " metadata_hash varchar, epoch int32, slot int64, block_hash varchar,"
                    + " block_time timestamptz, date date"),
            "pool_retirement", columns("tx_hash varchar, cert_index int32, tx_index int32,"
                    + " pool_id varchar, retirement_epoch int32, epoch int32, slot int64, block_hash varchar,"
                    + " block_time timestamptz, date date"),
            "address_utxo", columns("tx_hash varchar, output_index int16, asset_unit varchar,"
                    + " policy_id varchar, asset_name varchar, quantity decimal(38,0), owner_addr varchar,"
                    + " owner_stake_addr varchar, owner_payment_credential varchar,"
                    + " owner_stake_credential varchar, inline_datum varchar, data_hash varchar,"
                    + " script_ref varchar, reference_script_hash varchar, is_collateral_return boolean,"
                    + " epoch int32, slot int64, block_hash varchar, block_time timestamptz, tx_index int32,"
                    + " asset_index int64, date date, owner_addr_full varchar"));

    /** Target definitions from the store migrations; every column is nullable unless listed. */
    private static final Map<String, TargetTable> TARGETS = Map.of(
            "mir", target("mir", "id uuid, tx_hash varchar(64), cert_index integer, pot varchar(30),"
                    + " credential varchar(56), address varchar(255), amount numeric(38,0), epoch integer,"
                    + " slot bigint, block_hash varchar(64), block bigint, block_time bigint,"
                    + " update_datetime timestamp", "id", "tx_hash", "cert_index"),
            "pool_registration", target("pool_registration", "tx_hash varchar(64), cert_index integer,"
                    + " tx_index integer, pool_id varchar(56), vrf_key varchar(64), pledge numeric(20,0),"
                    + " cost numeric(20,0), margin double precision, margin_numerator numeric(20,0),"
                    + " margin_denominator numeric(20,0), reward_account varchar(255), pool_owners jsonb,"
                    + " relays jsonb, metadata_url text, metadata_hash varchar(64), epoch integer,"
                    + " slot bigint, block_hash varchar(64), block bigint, block_time bigint,"
                    + " update_datetime timestamp", "tx_hash", "cert_index", "tx_index"),
            "pool_retirement", target("pool_retirement", "tx_hash varchar(64), cert_index integer,"
                    + " tx_index integer, pool_id varchar(56), retirement_epoch integer, epoch integer,"
                    + " slot bigint, block_hash varchar(64), block bigint, block_time bigint,"
                    + " update_datetime timestamp", "tx_hash", "cert_index", "tx_index"),
            "address_utxo", target("address_utxo", "tx_hash varchar(64), output_index smallint,"
                    + " slot bigint, block_hash varchar(64), epoch integer, lovelace_amount bigint,"
                    + " amounts jsonb, data_hash varchar(64), inline_datum text, owner_addr varchar(500),"
                    + " owner_addr_full text, owner_stake_addr varchar(255),"
                    + " owner_payment_credential varchar(56), owner_stake_credential varchar(56),"
                    + " script_ref text, reference_script_hash varchar(56), is_collateral_return boolean,"
                    + " block bigint, block_time bigint, update_datetime timestamp, tx_index integer",
                    "tx_hash", "output_index"));

    @TempDir
    Path dir;

    @Test
    void everyBuiltInSqlTransformLoadsHivePartitionedFilesIntoItsTarget() throws Exception {
        SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();
        List<SnapshotTableSpec> sqlSpecs = registry.all().stream()
                .filter(s -> s.restore() == com.bloxbean.cardano.yaci.store.snapshot.spec.RestoreMode.IMPORT
                        && s.importSpec().mode() == ImportMode.SQL)
                .toList();
        assertThat(sqlSpecs).extracting(SnapshotTableSpec::id)
                .contains("mir", "pool-registration", "pool-retirement", "address-utxo");

        ColumnPlanner planner = new ColumnPlanner(new ConverterRegistry());
        TableLoader loader = new TableLoader(new ConverterRegistry(), dir);
        try (Connection duck = DuckDb.open("512MB", dir.resolve("work"), 1)) {
            for (SnapshotTableSpec spec : sqlSpecs) {
                assertThat(SOURCES).as("recorded source columns for %s", spec.relation()).containsKey(spec.relation());
                assertThat(TARGETS).as("target definition for %s", spec.targetTable()).containsKey(spec.targetTable());
                TargetTable target = TARGETS.get(spec.targetTable());

                List<SnapshotManifest.FileEntry> files = List.of(write(duck, spec));
                Map<String, List<SnapshotManifest.FileEntry>> dependencyFiles = new LinkedHashMap<>();
                Map<String, String> emptyDependencies = new LinkedHashMap<>();
                for (String dep : spec.importSpec().dependencies()) {
                    SnapshotTableSpec depSpec = registry.byId(dep).orElseThrow();
                    assertThat(SOURCES).as("recorded source columns for %s", depSpec.relation())
                            .containsKey(depSpec.relation());
                    dependencyFiles.put(dep, List.of(write(duck, depSpec)));
                    emptyDependencies.put(dep, ImportPlanCheck.emptyRelation(SOURCES.get(depSpec.relation())));
                }

                // The loader's own select over the files: it plans and runs, and keeps the file's epoch.
                String select = loader.sourceSelect(spec, ImportBatch.of("s", spec, files, files.get(0).partition()),
                        CUT_SLOT, COMPLETED_EPOCH, dependencyFiles);
                Map<String, String> overFiles = ImportPlanCheck.describe(duck, select);
                ColumnPlan plan = planner.plan(spec, overFiles, target);
                try (ResultSet rs = duck.createStatement().executeQuery(
                        "SELECT " + plan.selectList() + " FROM " + select + " AS src")) {
                    assertThat(rs.next()).as("%s produces a row", spec.id()).isTrue();
                    assertThat(rs.getInt(plan.targetColumns().indexOf("epoch") + 1))
                            .as("%s epoch comes from the file, not its directory", spec.id()).isEqualTo(207);
                }

                // The preflight check, over empty relations of the recorded types, sees the same output.
                String emptySource = ImportPlanCheck.emptyRelation(SOURCES.get(spec.relation()));
                assertThat(ImportPlanCheck.check(duck, planner, spec, SOURCES.get(spec.relation()), emptySource,
                        emptyDependencies, target, CUT_SLOT, COMPLETED_EPOCH)).as(spec.id()).isEmpty();
                String overEmpty = SqlResources.bindTransform(SqlResources.read(spec.importSpec().selectResource()),
                        emptySource, emptyDependencies, CUT_SLOT, COMPLETED_EPOCH);
                assertThat(ImportPlanCheck.describe(duck, overEmpty)).as(spec.id()).isEqualTo(overFiles);
            }
        }
    }

    /** One row of plausible values in the directory DuckLake would use, with a disagreeing partition column. */
    private SnapshotManifest.FileEntry write(Connection duck, SnapshotTableSpec spec) throws Exception {
        String partition = switch (spec.source().partition().strategy()) {
            case EPOCH -> "epoch=" + COMPLETED_EPOCH;
            case DAILY -> "date=2020-08-08";
            case NONE -> null;
        };
        String relative = "main/" + spec.relation() + (partition == null ? "" : "/" + partition) + "/f.parquet";
        Path file = dir.resolve(relative);
        if (Files.exists(file)) {
            return entry(relative);
        }
        List<String> values = new ArrayList<>();
        SOURCES.get(spec.relation()).forEach((column, type) -> values.add(value(column, type) + " AS " + column));
        Files.createDirectories(file.getParent());
        DuckDb.exec(duck, "COPY (SELECT " + String.join(", ", values) + ") TO '" + file + "' (FORMAT parquet)");
        return entry(relative);
    }

    private static String value(String column, String type) {
        if (column.equals("epoch")) {
            return "CAST(207 AS " + ImportPlanCheck.duckDbType(type) + ")";
        }
        if (type.startsWith("decimal") || type.startsWith("int") || type.startsWith("float")) {
            return "CAST(1 AS " + ImportPlanCheck.duckDbType(type) + ")";
        }
        return switch (type) {
            // Valid JSON, so the varchar-to-jsonb mappings load too; also the block join key.
            case "varchar" -> "'[\"x\"]'";
            case "boolean" -> "false";
            case "date" -> "DATE '2020-08-09'";
            case "timestamptz" -> "TIMESTAMPTZ '2020-08-08 12:00:00+00'";
            default -> throw new IllegalArgumentException("No sample value for " + type);
        };
    }

    private static SnapshotManifest.FileEntry entry(String path) {
        return new SnapshotManifest.FileEntry(path, 1, "0".repeat(64), 1, path.substring(0, path.lastIndexOf('/')));
    }

    private static Map<String, String> columns(String declaration) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String column : declaration.split(",(?![^(]*\\))")) {
            String[] parts = column.trim().split(" ", 2);
            out.put(parts[0], parts[1]);
        }
        return out;
    }

    private static TargetTable target(String name, String declaration, String... notNull) {
        Map<String, String> columns = columns(declaration);
        Map<String, Boolean> nullable = columns.keySet().stream().collect(Collectors.toMap(c -> c,
                c -> !List.of(notNull).contains(c), (a, b) -> a, LinkedHashMap::new));
        return new TargetTable("s", name, columns, nullable, Map.of(), List.of(notNull), false);
    }
}
