package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class ManifestValidatorTest {
    private final SnapshotSpecRegistry registry = SnapshotSpecRegistry.builtIn();
    private final ManifestValidator validator = new ManifestValidator(registry);

    private List<SnapshotManifest.TableManifest> tables() {
        return registry.all().stream().map(s -> table(s.id(), s.specVersion(), s.digest(), s.contractDigest()))
                .toList();
    }

    private SnapshotManifest.TableManifest table(String id, int version, String digest, String contractDigest) {
        var s = registry.byId(id).orElseThrow();
        return new SnapshotManifest.TableManifest(s.id(), version, digest, s.module(), s.kind().name(),
                s.restore().name(), s.reason(), s.relation(), null, s.targetTable(), null, null, 1,
                List.of(), 0, List.of(), null, Map.of(), Map.of(), List.of(), contractDigest);
    }

    private List<SnapshotManifest.TableManifest> replacing(SnapshotManifest.TableManifest replacement) {
        return tables().stream().map(t -> t.specId().equals(replacement.specId()) ? replacement : t).toList();
    }

    private SnapshotManifest manifest(int format, List<SnapshotManifest.TableManifest> tables) {
        return new SnapshotManifest(format, "snapshot", "now", "test", "test", "1", "duckdb", "1",
                new ConsistencyPoint("preprod", 1, 2, 20, 2, "hash", "prev", 7, 100, 1),
                null, List.of(), Map.of(), "schema", "flyway", tables, List.of(), List.of());
    }

    @Test
    void requiresEverySpecificationEvenWhenAllTablesAreEmpty() {
        assertThat(validator.validate(manifest(1, tables()))).isEmpty();
        assertThat(validator.validate(manifest(1, tables().stream()
                .filter(t -> !t.specId().equals("delegation")).toList())))
                .anyMatch(p -> p.contains("Missing manifest specification 'delegation'"));
    }

    @Test
    void aNewManifestPinsOnlyTheExportContract() {
        var mir = registry.byId("mir").orElseThrow();
        var fixedTransform = manifest(1, replacing(table("mir", mir.specVersion(), "0".repeat(64), mir.contractDigest())));
        assertThat(validator.validate(fixedTransform)).isEmpty();
        assertThat(validator.importChanges(fixedTransform)).singleElement().asString()
                .contains("'mir'").contains("export contract matches");

        var otherContract = manifest(1, replacing(table("mir", mir.specVersion(), mir.digest(), "0".repeat(64))));
        assertThat(validator.validate(otherContract)).singleElement().asString()
                .contains("'mir'").contains("export contract different");

        var otherVersion = manifest(1, replacing(table("mir", mir.specVersion() + 1, mir.digest(), mir.contractDigest())));
        assertThat(validator.validate(otherVersion)).singleElement().asString().contains("version does not match");
    }

    @Test
    void aManifestWithoutAContractDigestStillNeedsTheWholeSpecification() {
        var mir = registry.byId("mir").orElseThrow();
        assertThat(validator.validate(manifest(1, replacing(table("mir", mir.specVersion(), mir.digest(), null)))))
                .isEmpty();
        var older = manifest(1, replacing(table("mir", mir.specVersion(), "0".repeat(64), null)));
        assertThat(validator.validate(older)).singleElement().asString().contains("has digest different");
        assertThat(validator.importChanges(older)).isEmpty();
    }

    @Test
    void rejectsDuplicateSpecificationsAndUnknownFormats() {
        var duplicate = new ArrayList<>(tables());
        duplicate.add(duplicate.get(0));
        assertThat(validator.validate(manifest(1, duplicate)))
                .anyMatch(p -> p.contains("Duplicate manifest specification"));
        assertThat(validator.validate(manifest(2, tables())))
                .anyMatch(p -> p.contains("Unsupported snapshot"));
    }

    @Test
    void anEmptyTableOfARemovedSpecificationIsIgnoredButOneWithDataIsNot() {
        var removed = new SnapshotManifest.TableManifest("cip113-registry-node", 1, "d", "assets-ext", "EXTENSION_DATA",
                "EMPTY_EXPECTED", "gone", null, null, "cip113_registry_node", null, null, 0, List.of(), 0, List.of(),
                null, Map.of(), Map.of(), List.of(), "c");
        var withData = new SnapshotManifest.TableManifest("gone-table", 1, "d", "x", "CHAIN_DATA", "IMPORT", null,
                "gone", "gone", "gone", "MAPPED", null, 1, List.of(), 5, List.of(), null, Map.of(), Map.of(),
                List.of(new SnapshotManifest.FileEntry("main/gone/f.parquet", 1, "h", 5, "main/gone")), "c");
        var tables = new ArrayList<>(tables());
        tables.add(removed);
        assertThat(validator.validate(manifest(1, tables))).isEmpty();
        tables.add(withData);
        assertThat(validator.validate(manifest(1, tables))).singleElement().asString()
                .contains("'gone-table' not installed locally");
    }
}
