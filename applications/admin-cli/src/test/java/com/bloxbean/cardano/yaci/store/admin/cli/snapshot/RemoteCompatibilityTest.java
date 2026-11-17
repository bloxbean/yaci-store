package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class RemoteCompatibilityTest {

    private static SnapshotManifest withoutAssetsExt() {
        var tables = SnapshotSpecRegistry.builtIn().all().stream()
                .filter(s -> !s.id().equals("cip68-metadata"))
                .map(s -> new SnapshotManifest.TableManifest(s.id(), s.specVersion(), s.digest(), s.module(),
                        s.kind().name(), s.restore().name(), s.reason(), s.relation(), null, s.targetTable(), null,
                        null, 1, List.of(), 0, List.of(), null, Map.of(), Map.of(), List.of(), s.contractDigest()))
                .toList();
        return new SnapshotManifest(1, "snapshot", "now", "test", "test", "1", "duckdb", "1",
                new ConsistencyPoint("preprod", 1, 2, 20, 2, "hash", "prev", 7, 100, 1),
                null, List.of(), Map.of(), "schema", "flyway", tables, List.of(), List.of());
    }

    @Test
    void acceptsASnapshotWithoutTheTablesOfStoresTurnedOffHere() {
        var support = new SnapshotCliSupport(new MockEnvironment());
        assertThat(SnapshotRemoteCommands.compatibility(support.configuredRegistry()).apply(withoutAssetsExt()))
                .isEmpty();
    }

    @Test
    void stillRefusesASnapshotMissingTheTablesOfStoresTurnedOnHere() {
        var support = new SnapshotCliSupport(new MockEnvironment().withProperty("store.assets.ext.enabled", "true"));
        assertThat(SnapshotRemoteCommands.compatibility(support.configuredRegistry()).apply(withoutAssetsExt()))
                .anyMatch(p -> p.contains("Missing manifest specification 'cip68-metadata'"));
    }
}
