package com.bloxbean.cardano.yaci.store.snapshot.spec;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotSpecLoaderTest {

    private final SnapshotSpecLoader loader = new SnapshotSpecLoader();

    private SnapshotTableSpec load(String yaml) {
        return loader.load(yaml.getBytes(StandardCharsets.UTF_8), "test");
    }

    private static final String MINIMAL = """
            snapshot-table:
              id: pool-registration
              spec-version: 1
              module: staking
              kind: CHAIN_DATA
              restore: IMPORT
              source:
                exporter-id: pool_registration
                ducklake-relation: pool_registration
                partition:
                  strategy: DAILY
                  column: block_time
              consistency:
                cutoff:
                  type: SLOT_LTE
                  column: slot
              import:
                target-table: pool_registration
                mode: MAPPED
                columns:
                  vrf_key:
                    source: vrf_key_hash
                  block_time:
                    source: block_time
                    converter: timestamp-to-epoch-seconds
                ignore-source-columns: [date]
                use-target-defaults: [update_datetime]
                batch-boundary: FILE_SET
              validation:
                key: [tx_hash, cert_index]
                bounds: [slot, epoch]
            """;

    @Test
    void bindsAllSections() {
        SnapshotTableSpec spec = load(MINIMAL);

        assertThat(spec.id()).isEqualTo("pool-registration");
        assertThat(spec.specVersion()).isEqualTo(1);
        assertThat(spec.kind()).isEqualTo(TableKind.CHAIN_DATA);
        assertThat(spec.restore()).isEqualTo(RestoreMode.IMPORT);
        assertThat(spec.source().ducklakeRelation()).isEqualTo("pool_registration");
        assertThat(spec.source().partition().strategy()).isEqualTo(PartitionStrategy.DAILY);
        assertThat(spec.consistency().cutoff().type()).isEqualTo(CutoffType.SLOT_LTE);
        assertThat(spec.importSpec().mode()).isEqualTo(ImportMode.MAPPED);
        assertThat(spec.importSpec().columns()).containsKeys("vrf_key", "block_time");
        assertThat(spec.importSpec().columns().get("vrf_key").source()).isEqualTo("vrf_key_hash");
        assertThat(spec.importSpec().columns().get("block_time").converter())
                .isEqualTo("timestamp-to-epoch-seconds");
        assertThat(spec.importSpec().ignoreSourceColumns()).containsExactly("date");
        assertThat(spec.validation().key()).containsExactly("tx_hash", "cert_index");
        assertThat(spec.digest()).hasSize(64);
    }

    @Test
    void digestIsStableAcrossLoadsAndChangesWithContent() {
        String a = load(MINIMAL).digest();
        String b = load(MINIMAL).digest();
        assertThat(a).isEqualTo(b);

        String modified = MINIMAL.replace("bounds: [slot, epoch]", "bounds: [slot]");
        assertThat(load(modified).digest()).isNotEqualTo(a);
    }

    @Test
    void rejectsUnknownKeys() {
        assertThatThrownBy(() -> load(MINIMAL.replace("  kind: CHAIN_DATA", "  kind: CHAIN_DATA\n  colour: red")))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("unknown key(s)")
                .hasMessageContaining("colour");
    }

    @Test
    void rejectsUnknownEnumValue() {
        assertThatThrownBy(() -> load(MINIMAL.replace("type: SLOT_LTE", "type: SLOT_ROUGHLY")))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("invalid consistency.cutoff.type");
    }

    @Test
    void requiresReasonForEveryNonImportedTable() {
        String yaml = """
                snapshot-table:
                  id: epoch-nonce
                  spec-version: 1
                  module: epoch-nonce
                  kind: DERIVED_DATA
                  restore: NOT_RESTORED
                  import:
                    target-table: epoch_nonce
                """;
        assertThatThrownBy(() -> load(yaml))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("requires an explicit 'reason'");
    }

    @Test
    void directModeMustNotDeclareColumnMappings() {
        String yaml = MINIMAL.replace("mode: MAPPED", "mode: DIRECT");
        assertThatThrownBy(() -> load(yaml))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("DIRECT mode must not declare import.columns");
    }

    @Test
    void sqlModeRequiresABuiltInResource() {
        String yaml = MINIMAL
                .replace("mode: MAPPED", "mode: SQL")
                .replace("""
                        columns:
                          vrf_key:
                            source: vrf_key_hash
                          block_time:
                            source: block_time
                            converter: timestamp-to-epoch-seconds
                    """, "")
                .replace("ignore-source-columns: [date]",
                        "select-resource: file:///tmp/evil.sql\n    ignore-source-columns: [date]");
        assertThatThrownBy(() -> load(yaml))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("must be a built-in classpath:/snapshot/sql/ resource");
    }

    @Test
    void handlerModeMustNotDeclareASource() {
        String yaml = """
                snapshot-table:
                  id: cursor
                  spec-version: 1
                  module: core
                  kind: CONTROL_STATE
                  restore: HANDLER
                  reason: rebuilt from the imported block tail
                  source:
                    exporter-id: cursor
                    ducklake-relation: cursor_
                    partition:
                      strategy: NONE
                  import:
                    target-table: cursor_
                    handler: cursor-tail
                """;
        assertThatThrownBy(() -> load(yaml))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("must not declare a 'source' section");
    }

    @Test
    void importedTableRequiresAValidationKey() {
        String yaml = MINIMAL.replace("""
                  validation:
                    key: [tx_hash, cert_index]
                    bounds: [slot, epoch]
                """, "");
        assertThatThrownBy(() -> load(yaml))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("validation.key is required");
    }

    @Test
    void aColumnCannotBeBothMappedAndDefaulted() {
        String yaml = MINIMAL.replace("use-target-defaults: [update_datetime]",
                "use-target-defaults: [update_datetime, vrf_key]");
        assertThatThrownBy(() -> load(yaml))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("both explicitly mapped and listed in use-target-defaults");
    }

    @Test
    void lossyColumnMustAlsoBeLeftToItsDefault() {
        String yaml = MINIMAL + """
                  lossy:
                    metadata_hash: not exported
                """;
        assertThatThrownBy(() -> load(yaml))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("must also appear in import.use-target-defaults");
    }

    @Test
    void tableWithoutSourceCannotGateTheConsistencyPoint() {
        String yaml = """
                snapshot-table:
                  id: era
                  spec-version: 1
                  module: core
                  kind: CONTROL_STATE
                  restore: HANDLER
                  reason: rebuilt from imported blocks
                  consistency:
                    completed-epoch:
                      type: MAX_EPOCH
                      column: epoch
                  import:
                    target-table: era
                    handler: era-transitions
                """;
        assertThatThrownBy(() -> load(yaml))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("cannot gate the consistency point");
    }

    @Test
    void selfSourcingConverterMustNotAlsoNameASourceColumn() {
        String yaml = MINIMAL.replace("""
                    vrf_key:
                        source: vrf_key_hash
                """, """
                    vrf_key:
                        source: vrf_key_hash
                        converter: "uuid-v5:tx_hash"
                """);
        assertThatThrownBy(() -> load(yaml))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("self-sourcing converter");
    }

    @Test
    void rejectsIdentifiersThatCouldNotBeQuotedSafely() {
        assertThatThrownBy(() -> load(MINIMAL.replace("ducklake-relation: pool_registration",
                "ducklake-relation: \"pool; drop table x\"")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid SQL identifier");
    }
}
