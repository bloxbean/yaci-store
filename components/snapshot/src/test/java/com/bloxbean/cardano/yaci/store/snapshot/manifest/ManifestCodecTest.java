package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

class ManifestCodecTest {

    private final ManifestCodec codec = new ManifestCodec();

    private SnapshotManifest manifest(Map<String, String> bounds) {
        SnapshotManifest.FileEntry file =
                new SnapshotManifest.FileEntry("main/block/date=2026-01-01/a.parquet", 100, "abc", 10,
                        "main/block/date=2026-01-01");
        SnapshotManifest.TableManifest table = new SnapshotManifest.TableManifest(
                "block", 1, "specdigest", "blocks", "CHAIN_DATA", "IMPORT", null, "block", "block",
                "block", "MAPPED", "SLOT_LTE(slot)", 1, List.of(), 10, List.of("hash"), "colfp",
                Map.of("hash", "varchar"), bounds, List.of(file));
        SnapshotManifest.PartManifest part =
                new SnapshotManifest.PartManifest(1, "snap.part-00001.zip", 200, "def", 1, List.of("block"));
        return new SnapshotManifest(1, "sid", "2026-01-01T00:00:00Z", "test", "3.0.0", "1",
                "duckdb", "1.0",
                new ConsistencyPoint("preprod", 1, 308, 131414402L, 5072176L, "hash", "prev", 7, 1700L, 42),
                "genesis", List.of("blocks"), Map.of("store.utxo.pruning-enabled", "false"),
                "schemafp", "flywayfp", List.of(table), List.of(part), List.of());
    }

    @Test
    void serialisationIsDeterministicRegardlessOfMapOrder() {
        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("slot.min", "1");
        ordered.put("slot.max", "9");

        Map<String, String> reversed = new LinkedHashMap<>();
        reversed.put("slot.max", "9");
        reversed.put("slot.min", "1");

        assertThat(codec.toJson(manifest(ordered))).isEqualTo(codec.toJson(manifest(reversed)));
        assertThat(codec.digest(manifest(ordered))).isEqualTo(codec.digest(manifest(reversed)));
    }

    @Test
    void roundTripsThroughJson(@TempDir Path tmp) {
        SnapshotManifest original = manifest(new TreeMap<>(Map.of("slot.min", "1")));
        Path file = tmp.resolve("m.json");
        codec.writeAtomically(original, file);

        SnapshotManifest read = codec.read(file);
        assertThat(read.snapshotId()).isEqualTo("sid");
        assertThat(read.point().epoch()).isEqualTo(308);
        assertThat(read.point().ducklakeSnapshotId()).isEqualTo(42);
        assertThat(read.tables()).hasSize(1);
        assertThat(read.tables().get(0).files()).hasSize(1);
        assertThat(read.totalRows()).isEqualTo(10);
        assertThat(codec.digest(read)).isEqualTo(codec.digest(original));
    }

    @Test
    void writeIsAtomicAndLeavesNoTemporaryFile(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("m.json");
        codec.writeAtomically(manifest(Map.of()), file);
        try (var list = Files.list(tmp)) {
            assertThat(list.map(p -> p.getFileName().toString())).containsExactly("m.json");
        }
    }

    @Test
    void containsNoCredentialOrLocalPath() {
        String json = codec.toJson(manifest(Map.of()));
        assertThat(json)
                .doesNotContain("password")
                .doesNotContain("jdbc:")
                .doesNotContain("/Users/")
                .doesNotContain("localhost");
    }
}
