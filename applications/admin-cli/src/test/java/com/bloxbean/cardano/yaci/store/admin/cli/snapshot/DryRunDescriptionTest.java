package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DryRunDescriptionTest {

    @Test
    void listsLoadedSkippedAndHandlerTablesWithManifestCounts() {
        var registry = SnapshotSpecRegistry.builtIn().selectModules(Set.of("core", "blocks"));
        var manifest = new SnapshotManifest(1, "id", "now", "test", "dev", "1", "1.4.0", "1.0", null, null,
                List.of(), Map.of(), null, null, List.of(
                table("block", "IMPORT", "block", 30, 2_000, 1_000),
                table("address-utxo", "IMPORT", "address_utxo", 1_234_567, 3L << 30),
                table("era", "HANDLER", "era", 0)), List.of(), List.of());

        List<String> lines = SnapshotCommands.describeImport(manifest, registry);

        assertThat(lines.get(0)).startsWith("Load: 1 table(s), 30 rows, 2.9 KiB")
                .containsPattern("\\n +block +30 +2\\.9 KiB");
        assertThat(lines.get(1)).startsWith("Skip (store not enabled in the target): 1 table(s), 1,234,567 rows, 3.0 GiB")
                .containsPattern("address_utxo +1,234,567 +3\\.0 GiB");
        assertThat(lines.get(2)).isEqualTo("Not loaded from Parquet: era (handler)");
    }

    @Test
    void urlLosesItsQueryStringAndUserInfo() {
        assertThat(SnapshotCommands.redactedUrl("jdbc:postgresql://db:5432/yaci?currentSchema=s&password=x"))
                .isEqualTo("jdbc:postgresql://db:5432/yaci");
        assertThat(SnapshotCommands.redactedUrl("jdbc:postgresql://u:p@db/yaci")).isEqualTo("jdbc:postgresql://db/yaci");
    }

    private static SnapshotManifest.TableManifest table(String specId, String restore, String target, long rows,
                                                        long... fileSizes) {
        List<SnapshotManifest.FileEntry> files = java.util.Arrays.stream(fileSizes)
                .mapToObj(size -> new SnapshotManifest.FileEntry(target + "/" + size + ".parquet", size, "0", 1, null))
                .toList();
        return new SnapshotManifest.TableManifest(specId, 1, "d", "m", "HISTORY", restore, null, null, null, target,
                null, null, 1, List.of(), rows, List.of(), null, Map.of(), Map.of(), files);
    }
}
