package com.bloxbean.cardano.yaci.store.snapshot.spec;

import com.bloxbean.cardano.yaci.store.snapshot.load.SqlResources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotSpecRegistryTest {

    @Test
    void everyBuiltInSpecificationLoads() {
        SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();
        assertThat(registry.all()).isNotEmpty();
        assertThat(registry.importedTables()).isNotEmpty();
        assertThat(registry.handlerTables())
                .extracting(SnapshotTableSpec::targetTable)
                .contains("cursor_", "era", "adapot_jobs");
    }

    @Test
    void everyTargetTableIsClaimedByExactlyOneSpecification() {
        SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();
        assertThat(registry.all())
                .extracting(SnapshotTableSpec::targetTable)
                .doesNotHaveDuplicates();
    }

    @Test
    void specDigestsAreStableAcrossRegistryInstances() {
        Map<String, String> first = SnapshotSpecRegistry.builtIn().digests();
        Map<String, String> second = SnapshotSpecRegistry.builtIn().digests();
        assertThat(first).isEqualTo(second);
        assertThat(first.values()).allSatisfy(d -> assertThat(d).hasSize(64));
    }

    @Test
    void importedTablesAreOrderedAfterTheirDependencies() {
        List<SnapshotTableSpec> ordered = SnapshotSpecRegistry.builtIn().importedTables();
        for (int i = 0; i < ordered.size(); i++) {
            for (String dep : ordered.get(i).importSpec().dependencies()) {
                int depIndex = -1;
                for (int j = 0; j < ordered.size(); j++) {
                    if (ordered.get(j).id().equals(dep)) {
                        depIndex = j;
                        break;
                    }
                }
                assertThat(depIndex)
                        .as("dependency '%s' of '%s' must load first", dep, ordered.get(i).id())
                        .isBetween(0, i - 1);
            }
        }
    }

    @Test
    void everySqlTransformResourceExistsAndIsReadOnly() {
        for (SnapshotTableSpec spec : SnapshotSpecRegistry.builtIn().importedTables()) {
            if (spec.importSpec().mode() == ImportMode.SQL) {
                assertThat(SqlResources.read(spec.importSpec().selectResource()))
                        .as("transform for %s", spec.id())
                        .isNotBlank();
            }
        }
    }

    @Test
    void everyDeclaredDependencyResolves() {
        SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();
        for (SnapshotTableSpec spec : registry.all()) {
            for (String dep : spec.importSpec().dependencies()) {
                assertThat(registry.byId(dep))
                        .as("dependency '%s' of '%s'", dep, spec.id())
                        .isPresent();
            }
        }
    }

    @Test
    void customSpecificationsRequireExplicitAcknowledgement(@TempDir Path dir) throws IOException {
        Path custom = writeCustom(dir, "my-table", "my_table");
        assertThatThrownBy(() -> SnapshotSpecRegistry.load(List.of(custom), false))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("--allow-custom-specs");

        SnapshotSpecRegistry registry = SnapshotSpecRegistry.load(List.of(custom), true);
        assertThat(registry.byId("my-table")).isPresent();
        assertThat(registry.isBuiltIn(registry.byId("my-table").orElseThrow())).isFalse();
    }

    @Test
    void aCustomSpecificationCannotOverrideABuiltInOne(@TempDir Path dir) throws IOException {
        SnapshotTableSpec builtIn = SnapshotSpecRegistry.builtIn().byId("block").orElseThrow();
        Path custom = writeCustom(dir, builtIn.id(), "block");
        assertThatThrownBy(() -> SnapshotSpecRegistry.load(List.of(custom), true))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("cannot override the built-in definition");
    }

    @Test
    void twoSpecificationsCannotClaimTheSameTargetTable(@TempDir Path dir) throws IOException {
        Path custom = writeCustom(dir, "my-block-copy", "block");
        assertThatThrownBy(() -> SnapshotSpecRegistry.load(List.of(custom), true))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("is claimed by both");
    }

    @Test
    void circularDependenciesAreRejected() {
        SnapshotTableSpec a = spec("a", "b");
        SnapshotTableSpec b = spec("b", "a");
        assertThatThrownBy(() -> SnapshotSpecRegistry.orderByDependencies(List.of(a, b)))
                .isInstanceOf(SpecException.class)
                .hasMessageContaining("Circular snapshot dependency");
    }

    private static SnapshotTableSpec spec(String id, String dependsOn) {
        return new SnapshotTableSpec(id, 1, "m", TableKind.CHAIN_DATA, RestoreMode.IMPORT, null,
                null, null,
                new SnapshotTableSpec.Import(id, ImportMode.DIRECT, Map.of(), List.of(), List.of(),
                        null, 1, List.of(dependsOn), BatchBoundary.FILE_SET, 0, null, null),
                new SnapshotTableSpec.Validation(List.of("k"), List.of(), List.of()),
                Map.of(), "digest", "test");
    }

    private static Path writeCustom(Path dir, String id, String targetTable) throws IOException {
        Path file = dir.resolve(id + ".yml");
        Files.writeString(file, """
                snapshot-table:
                  id: %s
                  spec-version: 1
                  module: custom
                  kind: CHAIN_DATA
                  restore: IMPORT
                  source:
                    exporter-id: %s
                    ducklake-relation: %s
                    partition:
                      strategy: DAILY
                      column: block_time
                  consistency:
                    cutoff:
                      type: SLOT_LTE
                      column: slot
                  import:
                    target-table: %s
                    mode: DIRECT
                  validation:
                    key: [id]
                """.formatted(id, targetTable, targetTable, targetTable));
        return file;
    }
}
