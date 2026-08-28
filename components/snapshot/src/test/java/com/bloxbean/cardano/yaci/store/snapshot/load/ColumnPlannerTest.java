package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.convert.ConverterRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecLoader;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The strict column-accounting contract: nothing may be silently dropped, silently added, or
 * silently NULLed.
 */
class ColumnPlannerTest {

    private final ColumnPlanner planner = new ColumnPlanner(new ConverterRegistry());
    private final SnapshotSpecLoader loader = new SnapshotSpecLoader();

    private SnapshotTableSpec spec(String columnsBlock, String extras) {
        String yaml = """
                snapshot-table:
                  id: widget
                  spec-version: 1
                  module: test
                  kind: CHAIN_DATA
                  restore: IMPORT
                  source:
                    exporter-id: widget
                    ducklake-relation: widget
                    partition:
                      strategy: DAILY
                      column: block_time
                  consistency:
                    cutoff:
                      type: SLOT_LTE
                      column: slot
                  import:
                    target-table: widget
                    mode: MAPPED
                %s%s
                  validation:
                    key: [tx_hash]
                """.formatted(columnsBlock, extras);
        return loader.load(yaml.getBytes(StandardCharsets.UTF_8), "test");
    }

    private static Map<String, String> source(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put(pairs[i], pairs[i + 1]);
        }
        return m;
    }

    private static TargetTable target(Map<String, String> columns) {
        Map<String, Boolean> nullable = new LinkedHashMap<>();
        columns.keySet().forEach(c -> nullable.put(c, true));
        return new TargetTable("s", "widget", columns, nullable, Map.of(), List.of("tx_hash"), false);
    }

    @Test
    void mapsIdenticallyNamedColumnsAutomatically() {
        ColumnPlan plan = planner.plan(
                spec("", ""),
                source("tx_hash", "varchar", "slot", "int64"),
                target(source("tx_hash", "character varying(64)", "slot", "bigint")));

        assertThat(plan.targetColumns()).containsExactlyInAnyOrder("tx_hash", "slot");
        assertThat(plan.selectList()).contains("\"tx_hash\"").contains("\"slot\"");
    }

    @Test
    void appliesADeclaredRename() {
        ColumnPlan plan = planner.plan(
                spec("    columns:\n      vrf_key:\n        source: vrf_key_hash\n", ""),
                source("tx_hash", "varchar", "vrf_key_hash", "varchar"),
                target(source("tx_hash", "character varying(64)", "vrf_key", "character varying(64)")));

        int i = plan.targetColumns().indexOf("vrf_key");
        assertThat(plan.expressions().get(i)).isEqualTo("\"vrf_key_hash\"");
    }

    @Test
    void appliesTheTimestampConverter() {
        ColumnPlan plan = planner.plan(
                spec("    columns:\n      block_time:\n        source: block_time\n"
                        + "        converter: timestamp-to-epoch-seconds\n", ""),
                source("tx_hash", "varchar", "block_time", "timestamptz"),
                target(source("tx_hash", "character varying(64)", "block_time", "bigint")));

        int i = plan.targetColumns().indexOf("block_time");
        assertThat(plan.expressions().get(i)).isEqualTo("CAST(epoch(\"block_time\") AS BIGINT)");
    }

    @Test
    void rejectsATimestampMappedToBigintWithoutAConverter() {
        assertThatThrownBy(() -> planner.plan(
                spec("", ""),
                source("tx_hash", "varchar", "block_time", "timestamptz"),
                target(source("tx_hash", "character varying(64)", "block_time", "bigint"))))
                .isInstanceOf(ColumnPlanner.MappingException.class)
                .hasMessageContaining("block_time")
                .hasMessageContaining("Map it explicitly with a converter");
    }

    @Test
    void rejectsAVarcharMappedToJsonbWithoutAConverter() {
        assertThatThrownBy(() -> planner.plan(
                spec("", ""),
                source("tx_hash", "varchar", "inputs", "varchar"),
                target(source("tx_hash", "character varying(64)", "inputs", "jsonb"))))
                .isInstanceOf(ColumnPlanner.MappingException.class)
                .hasMessageContaining("inputs");
    }

    @Test
    void acceptsAVarcharToJsonbMappingWithTheConverter() {
        ColumnPlan plan = planner.plan(
                spec("    columns:\n      inputs:\n        source: inputs\n"
                        + "        converter: varchar-to-jsonb\n", ""),
                source("tx_hash", "varchar", "inputs", "varchar"),
                target(source("tx_hash", "character varying(64)", "inputs", "jsonb")));
        assertThat(plan.targetColumns()).contains("inputs");
    }

    @Test
    void anUnaccountedSourceColumnIsAnError() {
        assertThatThrownBy(() -> planner.plan(
                spec("", ""),
                source("tx_hash", "varchar", "date", "date"),
                target(source("tx_hash", "character varying(64)"))))
                .isInstanceOf(ColumnPlanner.MappingException.class)
                .hasMessageContaining("neither mapped nor listed in ignore-source-columns")
                .hasMessageContaining("date");
    }

    @Test
    void anUnwrittenTargetColumnIsAnError() {
        assertThatThrownBy(() -> planner.plan(
                spec("", ""),
                source("tx_hash", "varchar"),
                target(source("tx_hash", "character varying(64)", "update_datetime", "timestamp"))))
                .isInstanceOf(ColumnPlanner.MappingException.class)
                .hasMessageContaining("would be left unwritten")
                .hasMessageContaining("update_datetime");
    }

    @Test
    void ignoredSourceColumnsAndTargetDefaultsCloseTheAccounting() {
        ColumnPlan plan = planner.plan(
                spec("", "    ignore-source-columns: [date]\n"
                        + "    use-target-defaults: [update_datetime]\n"),
                source("tx_hash", "varchar", "date", "date"),
                target(source("tx_hash", "character varying(64)", "update_datetime", "timestamp")));

        assertThat(plan.targetColumns()).containsExactly("tx_hash");
        assertThat(plan.defaulted()).containsExactly("update_datetime");
    }

    @Test
    void schemaDriftFromANewSourceColumnFails() {
        // A migration adds a column to the export; the spec has not been updated yet.
        assertThatThrownBy(() -> planner.plan(
                spec("", "    ignore-source-columns: [date]\n"),
                source("tx_hash", "varchar", "date", "date", "brand_new_column", "varchar"),
                target(source("tx_hash", "character varying(64)"))))
                .isInstanceOf(ColumnPlanner.MappingException.class)
                .hasMessageContaining("brand_new_column");
    }

    @Test
    void ignoreListMustReferToARealSourceColumn() {
        assertThatThrownBy(() -> planner.plan(
                spec("", "    ignore-source-columns: [not_there]\n"),
                source("tx_hash", "varchar"),
                target(source("tx_hash", "character varying(64)"))))
                .isInstanceOf(ColumnPlanner.MappingException.class)
                .hasMessageContaining("which the source does not have");
    }

    @Test
    void narrowingAnIntegerWithoutAConverterIsRejected() {
        assertThatThrownBy(() -> planner.plan(
                spec("", ""),
                source("tx_hash", "varchar", "output_index", "int64"),
                target(source("tx_hash", "character varying(64)", "output_index", "smallint"))))
                .isInstanceOf(ColumnPlanner.MappingException.class)
                .hasMessageContaining("can overflow");
    }

    @Test
    void wideningAnIntegerIsAccepted() {
        ColumnPlan plan = planner.plan(
                spec("", ""),
                source("tx_hash", "varchar", "output_index", "int16"),
                target(source("tx_hash", "character varying(64)", "output_index", "smallint")));
        assertThat(plan.targetColumns()).contains("output_index");
    }

    @Test
    void selfSourcingUuidConverterDoesNotConsumeItsKeyColumns() {
        ColumnPlan plan = planner.plan(
                spec("    columns:\n      id:\n        converter: \"uuid-v5:tx_hash,idx\"\n", ""),
                source("tx_hash", "varchar", "idx", "int32"),
                target(source("id", "uuid", "tx_hash", "character varying(64)", "idx", "integer")));

        assertThat(plan.targetColumns()).containsExactlyInAnyOrder("id", "tx_hash", "idx");
        int i = plan.targetColumns().indexOf("id");
        assertThat(plan.expressions().get(i)).contains("sha1(").contains("'5'");
    }
}
