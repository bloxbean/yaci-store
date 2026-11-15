package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.convert.ConverterRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImportPlanCheckTest {

    private final SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();
    private final ColumnPlanner planner = new ColumnPlanner(new ConverterRegistry());

    private static final Map<String, String> MIR = ordered("tx_hash", "varchar", "cert_index", "int32",
            "pot", "varchar", "credential", "varchar", "address", "varchar", "amount", "decimal(38,0)",
            "epoch", "int32", "slot", "int64", "block_hash", "varchar", "block_time", "timestamptz");
    private static final Map<String, String> BLOCK = ordered("hash", "varchar", "number", "int64");

    @Test
    void anEmptyRelationHasTheRecordedTypes() throws Exception {
        try (Connection duck = DuckDb.open("256MB", null, 1)) {
            assertThat(ImportPlanCheck.describe(duck, "SELECT * FROM " + ImportPlanCheck.emptyRelation(ordered(
                    "a", "int8", "b", "int16", "c", "int32", "d", "int64", "e", "float64", "f", "decimal(20,0)",
                    "g", "timestamptz", "h", "varchar", "i", "boolean", "j", "date"))))
                    .containsExactlyEntriesOf(ordered("a", "TINYINT", "b", "SMALLINT", "c", "INTEGER",
                            "d", "BIGINT", "e", "DOUBLE", "f", "DECIMAL(20,0)", "g", "TIMESTAMP WITH TIME ZONE",
                            "h", "VARCHAR", "i", "BOOLEAN", "j", "DATE"));
        }
    }

    /** The mainnet failure, found without a single file: the transform's id does not fit the target. */
    @Test
    void anSqlTransformWhoseOutputDoesNotFitTheTargetIsReported() throws Exception {
        var mir = registry.byId("mir").orElseThrow();
        try (Connection duck = DuckDb.open("256MB", null, 1)) {
            List<String> ok = check(duck, mir, target("id uuid"));
            assertThat(ok).isEmpty();

            List<String> problems = check(duck, mir, target("id varchar(36)"));
            assertThat(problems).singleElement().asString()
                    .contains("'mir'").contains("column 'id'").contains("UUID").contains("varchar(36)");

            assertThat(check(duck, mir, target("id uuid", "epoch smallint"))).singleElement().asString()
                    .contains("narrowing 'INTEGER' to 'smallint'");
        }
    }

    @Test
    void aTransformThatCannotBindIsReportedNotThrown() throws Exception {
        var mir = registry.byId("mir").orElseThrow();
        Map<String, String> missing = new LinkedHashMap<>(MIR);
        missing.remove("credential");
        try (Connection duck = DuckDb.open("256MB", null, 1)) {
            assertThat(ImportPlanCheck.check(duck, planner, mir, missing, ImportPlanCheck.emptyRelation(missing),
                    Map.of("block", ImportPlanCheck.emptyRelation(BLOCK)), target("id uuid"), 1, 1))
                    .singleElement().asString().contains("mir_v3.sql").contains("cannot be prepared")
                    .contains("credential");
        }
    }

    @Test
    void mappedTablesArePlannedFromTheRecordedTypes() throws Exception {
        var adapot = registry.byId("adapot").orElseThrow();
        TargetTable target = new TargetTable("s", "adapot", ordered("epoch", "integer", "slot", "bigint",
                "update_datetime", "timestamp"), Map.of("epoch", false, "slot", true, "update_datetime", true),
                Map.of(), List.of("epoch"), false);
        try (Connection duck = DuckDb.open("256MB", null, 1)) {
            assertThat(ImportPlanCheck.check(duck, planner, adapot, ordered("epoch", "int32", "slot", "int64"),
                    null, Map.of(), target, 1, 1)).isEmpty();
            assertThat(ImportPlanCheck.check(duck, planner, adapot, ordered("epoch", "int64", "slot", "int64"),
                    null, Map.of(), target, 1, 1)).singleElement().asString().contains("narrowing");
        }
    }

    private List<String> check(Connection duck, com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec spec,
                               TargetTable target) {
        return ImportPlanCheck.check(duck, planner, spec, MIR, ImportPlanCheck.emptyRelation(MIR),
                Map.of("block", ImportPlanCheck.emptyRelation(BLOCK)), target, 1, 1);
    }

    /** The mir target, with some column types replaced. */
    private static TargetTable target(String... overrides) {
        Map<String, String> columns = ordered("id", "uuid", "tx_hash", "varchar(64)", "cert_index", "integer",
                "pot", "varchar(30)", "credential", "varchar(56)", "address", "varchar(255)",
                "amount", "numeric(38,0)", "epoch", "integer", "slot", "bigint", "block_hash", "varchar(64)",
                "block", "bigint", "block_time", "bigint", "update_datetime", "timestamp");
        for (String o : overrides) {
            String[] p = o.split(" ", 2);
            columns.put(p[0], p[1]);
        }
        Map<String, Boolean> nullable = new LinkedHashMap<>();
        columns.keySet().forEach(c -> nullable.put(c, !List.of("id", "tx_hash", "cert_index").contains(c)));
        return new TargetTable("s", "mir", columns, nullable, Map.of(), List.of("id"), false);
    }

    private static Map<String, String> ordered(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }
}
