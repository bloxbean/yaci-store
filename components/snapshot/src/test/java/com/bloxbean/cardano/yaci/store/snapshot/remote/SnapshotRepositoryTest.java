package com.bloxbean.cardano.yaci.store.snapshot.remote;

import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveExtractor;
import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveWriter;
import com.bloxbean.cardano.yaci.store.snapshot.archive.Checksums;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestCodec;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SnapshotRepositoryTest {

    @TempDir
    Path tmp;

    /** A real exported snapshot directory: parts, manifest and SHA256SUMS. */
    static Path snapshot(Path dir, String id, int epoch, String flavor) throws IOException {
        Path data = dir.resolve("data");
        List<SnapshotManifest.FileEntry> files = new ArrayList<>();
        List<String> owners = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Path p = data.resolve("main/block/date=2026-01-0" + (i + 1) + "/f.parquet");
            Files.createDirectories(p.getParent());
            byte[] payload = new byte[4000 + i];
            new java.util.Random(i + epoch).nextBytes(payload);
            Files.write(p, payload);
            String rel = data.relativize(p).toString();
            files.add(new SnapshotManifest.FileEntry(rel, payload.length, Digests.sha256Hex(p), 1,
                    rel.substring(0, rel.lastIndexOf('/'))));
            owners.add("block");
        }
        Path out = dir.resolve("out");
        var parts = new ArchiveWriter(data, out, "yaci-preprod-e" + epoch + "-" + id.substring(0, 8), 10_000)
                .write(files, owners, null);
        var table = new SnapshotManifest.TableManifest("block", 1, "d", "blocks", "CHAIN_DATA", "IMPORT", null,
                "block", "block", "block", "MAPPED", "SLOT_LTE", 1, List.of(), files.size(), List.of("hash"), "f",
                Map.of(), Map.of(), files, "c");
        var point = new ConsistencyPoint("preprod", 1, epoch, epoch * 1000L, epoch * 100L, "ab".repeat(32),
                "cd".repeat(32), 7, 1700000000L, 1);
        var manifest = new SnapshotManifest(ManifestCodec.FORMAT_VERSION, id, "2026-10-17T00:00:0" + (epoch % 10) + "Z",
                "test", "3.0.0", ManifestCodec.SPEC_FORMAT_VERSION, "duck", "1", point, null, List.of("blocks"), Map.of(),
                "s", "f", List.of(table), parts, List.of(), Map.of(), flavor,
                SnapshotManifest.FLAVOR_PRUNED.equals(flavor) ? Map.of("epoch_stake.epochs", "8") : null);
        Path manifestPath = out.resolve("yaci-preprod-e" + epoch + "-" + id.substring(0, 8) + ".manifest.json");
        new ManifestCodec().writeAtomically(manifest, manifestPath);
        Checksums.write(manifest, manifestPath);
        return manifestPath;
    }

    private static final String ID1 = "11111111-1111-1111-1111-111111111111";
    private static final String ID2 = "22222222-2222-2222-2222-222222222222";
    private static final String ID3 = "33333333-3333-3333-3333-333333333333";

    @Test
    void publishesPartsThenManifestAndListsTheSnapshotNewestFirst() throws Exception {
        var store = new LocalSnapshotStore(tmp.resolve("bucket"));
        var repo = new SnapshotRepository(store);
        Path first = snapshot(tmp.resolve("s1"), ID1, 100, null);
        Path second = snapshot(tmp.resolve("s2"), ID2, 101, null);

        var result = repo.publish(first, 2, null);
        repo.publish(second, 2, null);

        assertThat(result.directory()).isEqualTo("preprod/" + ID1 + "/");
        assertThat(store.list("preprod/" + ID1 + "/")).contains("preprod/" + ID1 + "/SHA256SUMS",
                "preprod/" + ID1 + "/" + first.getFileName());
        SnapshotIndex index = repo.index("preprod");
        assertThat(index.protocolMagic()).isEqualTo(1);
        assertThat(index.snapshots()).extracting(SnapshotIndex.Entry::snapshotId).containsExactly(ID2, ID1);
        assertThat(index.snapshots().get(1).manifestSha256()).isEqualTo(Digests.sha256Hex(first));
    }

    @Test
    void aSecondPublishSendsNothingAndKeepsOneIndexEntry() throws Exception {
        var repo = new SnapshotRepository(new LocalSnapshotStore(tmp.resolve("bucket")));
        Path m = snapshot(tmp.resolve("s1"), ID1, 100, null);
        repo.publish(m, 2, null);

        var again = repo.publish(m, 2, null);

        assertThat(again.filesSent()).isZero();
        assertThat(again.filesSkipped()).isGreaterThan(1);
        assertThat(repo.index("preprod").snapshots()).hasSize(1);
    }

    @Test
    void latestSkipsSnapshotsThisReleaseCannotImportAndHonoursTheFlavor() throws Exception {
        var repo = new SnapshotRepository(new LocalSnapshotStore(tmp.resolve("bucket")));
        repo.publish(snapshot(tmp.resolve("s1"), ID1, 100, null), 1, null);
        repo.publish(snapshot(tmp.resolve("s2"), ID2, 102, null), 1, null);
        repo.publish(snapshot(tmp.resolve("s3"), ID3, 103, SnapshotManifest.FLAVOR_PRUNED), 1, null);
        List<String> messages = new ArrayList<>();

        var full = repo.resolve("preprod", "latest", null,
                m -> m.point().epoch() > 101 ? List.of("made by a newer release") : List.of(), messages::add);
        var pruned = repo.resolve("preprod", "latest", "pruned", m -> List.of(), null);
        var byPrefix = repo.resolve("preprod", "2222", null, m -> List.of(), null);

        assertThat(full.entry().snapshotId()).isEqualTo(ID1);
        assertThat(messages).anyMatch(m -> m.contains("Skipping " + ID2) && m.contains("made by a newer release"));
        assertThat(pruned.entry().snapshotId()).isEqualTo(ID3);
        assertThat(pruned.manifest().retention()).containsEntry("epoch_stake.epochs", "8");
        assertThat(byPrefix.entry().snapshotId()).isEqualTo(ID2);
        assertThatThrownBy(() -> repo.resolve("preprod", ID2, null, m -> List.of("too new"), null))
                .hasMessageContaining("cannot be imported by this release");
        assertThatThrownBy(() -> repo.resolve("mainnet", "latest", null, m -> List.of(), null))
                .hasMessageContaining("No snapshots are published for mainnet");
    }

    @Test
    void aManifestThatDoesNotMatchTheIndexIsRefused() throws Exception {
        Path bucket = tmp.resolve("bucket");
        var repo = new SnapshotRepository(new LocalSnapshotStore(bucket));
        Path m = snapshot(tmp.resolve("s1"), ID1, 100, null);
        repo.publish(m, 1, null);
        Path published = bucket.resolve("preprod/" + ID1 + "/" + m.getFileName());
        Files.writeString(published, Files.readString(published).replace("\"test\"", "\"evil\""));

        assertThatThrownBy(() -> repo.resolve("preprod", "latest", null, x -> List.of(), null))
                .hasMessageContaining("does not match the digest in the index");
    }

    @Test
    void downloadIsByteIdenticalResumableAndRejectsATamperedPart() throws Exception {
        Path bucket = tmp.resolve("bucket");
        var repo = new SnapshotRepository(new LocalSnapshotStore(bucket));
        Path m = snapshot(tmp.resolve("s1"), ID1, 100, null);
        repo.publish(m, 1, null);
        var resolved = repo.resolve("preprod", "latest", null, x -> List.of(), null);
        Path out = tmp.resolve("download");

        Path manifest = repo.downloadMetadata(resolved, out);
        repo.downloadParts(resolved, out, p -> true, 2, null);
        repo.downloadParts(resolved, out, p -> true, 2, null);

        assertThat(Files.readAllBytes(manifest)).isEqualTo(Files.readAllBytes(m));
        for (var p : resolved.manifest().parts()) {
            assertThat(Digests.sha256Hex(out.resolve(p.fileName()))).isEqualTo(p.sha256());
        }
        assertThat(Files.readString(out.resolve("SHA256SUMS"))).isEqualTo(Files.readString(m.resolveSibling("SHA256SUMS")));

        var part = resolved.manifest().parts().get(0);
        Path remotePart = bucket.resolve("preprod/" + ID1 + "/" + part.fileName());
        byte[] bytes = Files.readAllBytes(remotePart);
        bytes[bytes.length / 2] ^= 1;
        Files.write(remotePart, bytes);
        Files.delete(out.resolve(part.fileName()));
        assertThatThrownBy(() -> repo.downloadParts(resolved, out, p -> p.equals(part), 1, null))
                .hasMessageContaining("does not match the manifest");
    }

    @Test
    void aPublicHttpLocationIsReadAnonymouslyAndStreamsPartsIntoTheExtractor() throws Exception {
        Path bucket = tmp.resolve("bucket");
        new SnapshotRepository(new LocalSnapshotStore(bucket)).publish(snapshot(tmp.resolve("s1"), ID1, 100, null), 1, null);
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/snapshots/", exchange -> {
            requests.incrementAndGet();
            Path file = bucket.resolve(exchange.getRequestURI().getPath().substring("/snapshots/".length()));
            if (!Files.isRegularFile(file)) {
                exchange.sendResponseHeaders(404, -1);
            } else if (exchange.getRequestMethod().equals("HEAD")) {
                exchange.getResponseHeaders().add("Content-Length", Long.toString(Files.size(file)));
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, Files.size(file));
                try (var out = exchange.getResponseBody()) {
                    Files.copy(file, out);
                }
            }
            exchange.close();
        });
        server.start();
        try (var http = SnapshotLocations.open("http://127.0.0.1:" + server.getAddress().getPort() + "/snapshots", null)) {
            assertThat(http.writable()).isFalse();
            var repo = new SnapshotRepository(http);
            var resolved = repo.resolve("preprod", "latest", null, x -> List.of(), null);
            var source = repo.partSource(resolved);
            assertThat(source.remote()).isTrue();

            Map<String, Long> sizes = new TreeMap<>();
            Map<String, String> digests = new TreeMap<>();
            resolved.manifest().tables().forEach(t -> t.files().forEach(f -> {
                sizes.put(f.path(), f.size());
                digests.put(f.path(), f.sha256());
            }));
            var extractor = new ArchiveExtractor(tmp.resolve("work"), 100, 1 << 24);
            int written = 0;
            for (var part : resolved.manifest().parts()) {
                assertThat(source.size(part)).hasValue(part.size());
                try (InputStream in = source.open(part)) {
                    var r = extractor.extract(in, sizes, digests, f -> true, null);
                    assertThat(r.partSha256()).isEqualTo(part.sha256());
                    written += r.written().size();
                }
            }
            assertThat(written).isEqualTo(sizes.size());
            assertThatThrownBy(() -> repo.publish(tmp.resolve("s1/out").resolve("x"), 1, null))
                    .isInstanceOf(UnsupportedOperationException.class);
        } finally {
            server.stop(0);
        }
        assertThat(requests.get()).isGreaterThan(2);
    }

    @Test
    void theIndexIsRebuiltFromTheManifestsInTheStore() throws Exception {
        Path bucket = tmp.resolve("bucket");
        var repo = new SnapshotRepository(new LocalSnapshotStore(bucket));
        repo.publish(snapshot(tmp.resolve("s1"), ID1, 100, null), 1, null);
        repo.publish(snapshot(tmp.resolve("s2"), ID2, 101, null), 1, null);
        Files.writeString(bucket.resolve("preprod/index.json"), "{\"formatVersion\":1,\"network\":\"preprod\",\"snapshots\":[]}");

        SnapshotIndex rebuilt = repo.rebuildIndex("preprod", 1, null);

        assertThat(rebuilt.snapshots()).extracting(SnapshotIndex.Entry::snapshotId).containsExactly(ID2, ID1);
        assertThat(repo.index("preprod").snapshots()).hasSize(2);
    }

    @Test
    void aConcurrentIndexChangeIsNotLost() throws Exception {
        var store = new LocalSnapshotStore(tmp.resolve("bucket"));
        assertThat(store.putIfUnchanged("preprod/index.json", "a".getBytes(), null)).isTrue();
        String tag = store.read("preprod/index.json").orElseThrow().etag();
        assertThat(store.putIfUnchanged("preprod/index.json", "b".getBytes(), null)).isFalse();
        assertThat(store.putIfUnchanged("preprod/index.json", "b".getBytes(), tag)).isTrue();
        assertThat(store.putIfUnchanged("preprod/index.json", "c".getBytes(), tag)).isFalse();
        assertThat(new String(store.read("preprod/index.json").orElseThrow().bytes())).isEqualTo("b");
    }

    @Test
    void locationsAreParsed() {
        assertThat(SnapshotLocations.open(tmp.toString(), null)).isInstanceOf(LocalSnapshotStore.class);
        assertThat(SnapshotLocations.open(tmp.toUri().toString(), null)).isInstanceOf(LocalSnapshotStore.class);
        assertThat(SnapshotLocations.open("https://snapshots.example.org/v1", null)).isInstanceOf(HttpSnapshotStore.class);
        assertThatThrownBy(() -> SnapshotLocations.open("ftp://x/y", null)).hasMessageContaining("Unsupported");
        assertThatThrownBy(() -> SnapshotLocations.open("s3:///prefix", null)).hasMessageContaining("No bucket");
        assertThatThrownBy(() -> SnapshotLocations.open(" ", null)).hasMessageContaining("--remote");
        var settings = new S3SnapshotStore.Settings("http://localhost:9000", "auto", true, "key", "secret", 0, 4);
        assertThat(settings.toString()).doesNotContain("secret").doesNotContain("key,");
        assertThatThrownBy(() -> new S3SnapshotStore.Settings(null, null, false, null, null, 1 << 20, 1))
                .hasMessageContaining("5MiB");
        try (var s3 = SnapshotLocations.open("s3://bucket/some/prefix", settings)) {
            assertThat(s3.describe()).isEqualTo("s3://bucket/some/prefix/ at http://localhost:9000");
        }
        assertThat(URI.create("http://x/y/").resolve("a/b")).hasToString("http://x/y/a/b");
    }
}
