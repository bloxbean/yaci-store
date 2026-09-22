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
        return registry.all().stream().map(s -> new SnapshotManifest.TableManifest(s.id(),
                s.specVersion(), s.digest(), s.module(), s.kind().name(), s.restore().name(),
                s.reason(), s.relation(), null, s.targetTable(), null, null, 1,
                List.of(), 0, List.of(), null, Map.of(), Map.of(), List.of())).toList();
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
    void rejectsDuplicateSpecificationsAndUnknownFormats() {
        var duplicate = new ArrayList<>(tables());
        duplicate.add(duplicate.get(0));
        assertThat(validator.validate(manifest(1, duplicate)))
                .anyMatch(p -> p.contains("Duplicate manifest specification"));
        assertThat(validator.validate(manifest(2, tables())))
                .anyMatch(p -> p.contains("Unsupported snapshot"));
    }
}
