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

public class SnapshotRepositoryTest {

    @TempDir
    Path tmp;

    /** A real exported snapshot directory: parts, manifest and SHA256SUMS. */
    public static Path snapshot(Path dir, String id, int epoch, String flavor) throws IOException {
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

        var result = repo.publish(first, 2, true, null);
        repo.publish(second, 2, true, null);

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
        repo.publish(m, 2, true, null);

        var again = repo.publish(m, 2, true, null);

        assertThat(again.filesSent()).isZero();
        assertThat(again.filesSkipped()).isGreaterThan(1);
        assertThat(repo.index("preprod").snapshots()).hasSize(1);
    }

    @Test
    void latestSkipsSnapshotsThisReleaseCannotImport() throws Exception {
        var repo = new SnapshotRepository(new LocalSnapshotStore(tmp.resolve("bucket")));
        repo.publish(snapshot(tmp.resolve("s1"), ID1, 100, null), 1, true, null);
        repo.publish(snapshot(tmp.resolve("s2"), ID2, 102, null), 1, true, null);
        List<String> messages = new ArrayList<>();

        var latest = repo.resolve("preprod", "latest",
                m -> m.point().epoch() > 101 ? List.of("made by a newer release") : List.of(), messages::add);
        var byPrefix = repo.resolve("preprod", "2222", m -> List.of(), null);

        assertThat(latest.entry().snapshotId()).isEqualTo(ID1);
        assertThat(messages).anyMatch(m -> m.contains("Skipping " + ID2) && m.contains("made by a newer release"));
        assertThat(byPrefix.entry().snapshotId()).isEqualTo(ID2);
        assertThatThrownBy(() -> repo.resolve("preprod", ID2, m -> List.of("too new"), null))
                .hasMessageContaining("cannot be imported by this release");
        assertThatThrownBy(() -> repo.resolve("mainnet", "latest", m -> List.of(), null))
                .hasMessageContaining("No live snapshot is published for mainnet");
    }

    @Test
    void aStagedSnapshotIsImportableByIdButNotListedUntilPromoted() throws Exception {
        Path bucket = tmp.resolve("bucket");
        var repo = new SnapshotRepository(new LocalSnapshotStore(bucket));
        repo.publish(snapshot(tmp.resolve("s1"), ID1, 100, null), 1, true, null);
        var staged = repo.publish(snapshot(tmp.resolve("s2"), ID2, 101, null), 1, null);
        List<String> messages = new ArrayList<>();

        assertThat(staged.entry().live()).isFalse();
        assertThat(repo.index("preprod").snapshots()).extracting(SnapshotIndex.Entry::snapshotId).containsExactly(ID1);
        assertThat(repo.resolve("preprod", "latest", m -> List.of(), null).entry().snapshotId()).isEqualTo(ID1);
        var byId = repo.resolve("preprod", ID2, m -> List.of(), messages::add);
        assertThat(byId.entry().snapshotId()).isEqualTo(ID2);
        assertThat(byId.entry().live()).isFalse();
        assertThat(messages).anyMatch(m -> m.contains("staged"));
        // A staged snapshot is found by its full id only.
        assertThatThrownBy(() -> repo.resolve("preprod", "2222", m -> List.of(), null))
                .hasMessageContaining("is not published");
        assertThat(repo.scan("preprod", null)).extracting(e -> e.snapshotId() + ":" + e.live())
                .containsExactly(ID2 + ":false", ID1 + ":true");
        // Rebuilding the index does not make it live.
        assertThat(repo.rebuildIndex("preprod", 1, null).snapshots()).extracting(SnapshotIndex.Entry::snapshotId)
                .containsExactly(ID1);

        var promoted = repo.promote("preprod", ID2);

        assertThat(promoted.live()).isTrue();
        assertThat(repo.resolve("preprod", "latest", m -> List.of(), null).entry().snapshotId()).isEqualTo(ID2);
        assertThat(SnapshotIndex.Entry.parse(Files.readAllBytes(bucket.resolve("preprod/" + ID2 + "/snapshot.json"))).live())
                .isTrue();
        // Republishing a live snapshot keeps it live.
        repo.publish(snapshot(tmp.resolve("s2"), ID2, 101, null), 1, null);
        assertThat(repo.index("preprod").snapshots()).extracting(SnapshotIndex.Entry::snapshotId).containsExactly(ID2, ID1);

        repo.unlist("preprod", ID2);

        assertThat(repo.index("preprod").snapshots()).extracting(SnapshotIndex.Entry::snapshotId).containsExactly(ID1);
        assertThat(repo.resolve("preprod", ID2, m -> List.of(), null).entry().live()).isFalse();
        assertThatThrownBy(() -> repo.promote("preprod", "../x")).hasMessageContaining("Invalid snapshot id");
    }

    @Test
    void aManifestThatDoesNotMatchTheIndexIsRefused() throws Exception {
        Path bucket = tmp.resolve("bucket");
        var repo = new SnapshotRepository(new LocalSnapshotStore(bucket));
        Path m = snapshot(tmp.resolve("s1"), ID1, 100, null);
        repo.publish(m, 1, true, null);
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
        repo.publish(m, 1, true, null);
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
    void aPartNameThatIsNotAPlainFileIsRefusedWhenTheManifestIsRead() throws Exception {
        Path m = snapshot(tmp.resolve("s1"), ID1, 100, null);
        String json = Files.readString(m);
        String name = new ManifestCodec().parse(json.getBytes()).parts().get(0).fileName();
        for (String bad : List.of("../victim.txt", "/tmp/victim.zip", "sub/part.zip", "sub\\\\part.zip", "..", ".",
                ".hidden.zip", "", "C:part.zip")) {
            byte[] crafted = json.replace("\"" + name + "\"", "\"" + bad + "\"").getBytes();
            assertThatThrownBy(() -> new ManifestCodec().parse(crafted))
                    .as(bad).hasStackTraceContaining("Invalid file name for archive part");
        }
    }

    @Test
    void aCraftedManifestCannotWriteOutsideTheDownloadDirectory() throws Exception {
        Path bucket = tmp.resolve("bucket");
        var repo = new SnapshotRepository(new LocalSnapshotStore(bucket));
        Path m = snapshot(tmp.resolve("s1"), ID1, 100, null);
        repo.publish(m, 1, true, null);
        Path victim = Files.writeString(tmp.resolve("victim.txt"), "keep");
        // The attacker controls the store: rewrite a part's name and every digest that covers the manifest.
        Path published = bucket.resolve("preprod/" + ID1 + "/" + m.getFileName());
        String oldSha = Digests.sha256Hex(published);
        String name = new ManifestCodec().parse(Files.readAllBytes(published)).parts().get(0).fileName();
        Files.writeString(published, Files.readString(published).replace("\"" + name + "\"", "\"../victim.txt\""));
        String newSha = Digests.sha256Hex(published);
        for (Path f : List.of(bucket.resolve("preprod/index.json"), bucket.resolve("preprod/" + ID1 + "/snapshot.json"))) {
            Files.writeString(f, Files.readString(f).replace(oldSha, newSha));
        }

        assertThatThrownBy(() -> {
            var resolved = repo.resolve("preprod", ID1, null, x -> List.of(), null);
            Path out = tmp.resolve("download");
            repo.downloadMetadata(resolved, out);
            repo.downloadParts(resolved, out, p -> true, 2, null);
        }).hasStackTraceContaining("Invalid file name for archive part");
        assertThat(Files.readString(victim)).isEqualTo("keep");
    }

    @Test
    void anIndexEntryWhoseKeyOrIdLeavesItsDirectoryIsRefused() throws Exception {
        Path bucket = tmp.resolve("bucket");
        var repo = new SnapshotRepository(new LocalSnapshotStore(bucket));
        repo.publish(snapshot(tmp.resolve("s1"), ID1, 100, null), 1, true, null);
        Path index = bucket.resolve("preprod/index.json");
        String original = Files.readString(index);

        Files.writeString(index, original.replace("\"" + ID1 + "/", "\"../" + ID1 + "/"));
        assertThatThrownBy(() -> repo.resolve("preprod", "latest", null, x -> List.of(), null))
                .hasStackTraceContaining("Invalid file name for manifest key");

        Files.writeString(index, original.replace("\"snapshotId\" : \"" + ID1, "\"snapshotId\" : \"../" + ID1));
        assertThatThrownBy(() -> repo.resolve("preprod", "latest", null, x -> List.of(), null))
                .hasStackTraceContaining("Invalid file name for snapshot id");
    }

    @Test
    void namesAreResolvedOnlyInsideTheDirectory() throws Exception {
        assertThat(SnapshotRepository.inside(tmp, "a.part-00001.zip")).isEqualTo(tmp.toAbsolutePath().resolve("a.part-00001.zip"));
        for (String bad : List.of("../x.zip", "/abs.zip", "a/b.zip", "..", "")) {
            assertThatThrownBy(() -> SnapshotRepository.inside(tmp, bad)).as(bad).isInstanceOf(IOException.class);
        }
    }

    @Test
    void linksAlreadyInTheDownloadDirectoryAreReplacedNotWrittenThrough() throws Exception {
        Path bucket = tmp.resolve("bucket");
        var repo = new SnapshotRepository(new LocalSnapshotStore(bucket));
        Path m = snapshot(tmp.resolve("s1"), ID1, 100, null);
        repo.publish(m, 1, true, null);
        var resolved = repo.resolve("preprod", "latest", null, x -> List.of(), null);
        var part = resolved.manifest().parts().get(0);
        Path out = Files.createDirectories(tmp.resolve("download"));
        Path victim = Files.writeString(tmp.resolve("victim.txt"), "keep");
        List<String> linked = List.of(part.fileName(), part.fileName() + ".download", "SHA256SUMS",
                m.getFileName().toString());
        try {
            for (String n : linked) {
                Files.createSymbolicLink(out.resolve(n), victim);
            }
        } catch (UnsupportedOperationException | IOException e) {
            org.junit.jupiter.api.Assumptions.abort("symbolic links are not available: " + e.getMessage());
        }

        repo.downloadMetadata(resolved, out);
        repo.downloadParts(resolved, out, p -> true, 2, null);

        assertThat(Files.readString(victim)).isEqualTo("keep");
        for (String n : List.of(part.fileName(), "SHA256SUMS", m.getFileName().toString())) {
            assertThat(Files.isSymbolicLink(out.resolve(n))).as(n).isFalse();
        }
        assertThat(Digests.sha256Hex(out.resolve(part.fileName()))).isEqualTo(part.sha256());
        assertThat(Files.readAllBytes(out.resolve(m.getFileName()))).isEqualTo(Files.readAllBytes(m));
        try (var files = Files.list(out)) {
            assertThat(files.map(f -> f.getFileName().toString())).noneMatch(n -> n.endsWith(".download") || n.endsWith(".tmp"));
        }
    }

    @Test
    void aBrokenDownloadResumesWithARangeRequestOrReadsPastTheStartWhenTheServerIgnoresRanges() throws Exception {
        Path bucket = tmp.resolve("bucket");
        new SnapshotRepository(new LocalSnapshotStore(bucket)).publish(snapshot(tmp.resolve("s1"), ID1, 100, null), 1, true, null);
        for (boolean honourRange : new boolean[]{true, false}) {
            Path out = Files.createDirectories(tmp.resolve("download-" + honourRange));
            try (var server = new TestHttpServer(bucket, true, honourRange);
                 var http = new HttpSnapshotStore(java.net.URI.create(server.url()), java.time.Duration.ofSeconds(1))) {
                var repo = new SnapshotRepository(http, java.time.Duration.ZERO);
                var resolved = repo.resolve("preprod", "latest", null, x -> List.of(), null);
                List<String> said = new java.util.concurrent.CopyOnWriteArrayList<>();

                repo.downloadParts(resolved, out, p -> true, 2, said::add);

                for (var p : resolved.manifest().parts()) {
                    assertThat(Digests.sha256Hex(out.resolve(p.fileName()))).isEqualTo(p.sha256());
                }
                // Every part broke once and was resumed, not downloaded again.
                assertThat(server.rangeRequests()).isEqualTo(resolved.manifest().parts().size());
                assertThat(said).noneMatch(m -> m.contains("Retrying"));
            }
        }
    }

    @Test
    void aPublicHttpLocationIsReadAnonymouslyAndStreamsPartsIntoTheExtractor() throws Exception {
        Path bucket = tmp.resolve("bucket");
        new SnapshotRepository(new LocalSnapshotStore(bucket)).publish(snapshot(tmp.resolve("s1"), ID1, 100, null), 1, true, null);
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
            assertThatThrownBy(() -> repo.publish(tmp.resolve("s1/out").resolve("x"), 1, true, null))
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
        repo.publish(snapshot(tmp.resolve("s1"), ID1, 100, null), 1, true, null);
        repo.publish(snapshot(tmp.resolve("s2"), ID2, 101, null), 1, true, null);
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
