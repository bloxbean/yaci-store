package com.bloxbean.cardano.yaci.store.snapshot.remote;

import com.bloxbean.cardano.yaci.store.snapshot.archive.ArchiveExtractor;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Against a real S3-compatible service: MinIO locally, Cloudflare R2 for the final check. Runs only
 * when {@code SNAPSHOT_S3_IT_URL} is set, under a fresh prefix it removes afterwards.
 *
 * <pre>
 * SNAPSHOT_S3_IT_URL=s3://bucket/some/prefix
 * SNAPSHOT_S3_IT_ENDPOINT=http://localhost:9000        (R2: https://&lt;account&gt;.r2.cloudflarestorage.com)
 * SNAPSHOT_S3_IT_REGION=auto
 * SNAPSHOT_S3_IT_ACCESS_KEY_ID=...
 * SNAPSHOT_S3_IT_SECRET_ACCESS_KEY=...
 * SNAPSHOT_S3_IT_PUBLIC_URL=https://...                 (optional: the same prefix read anonymously)
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "SNAPSHOT_S3_IT_URL", matches = "s3://.+")
class S3SnapshotStoreIT {

    @TempDir
    Path tmp;

    private String run;
    private SnapshotStore store;

    private static String env(String name) {
        return System.getenv(name);
    }

    @BeforeEach
    void open() {
        run = "it-" + System.currentTimeMillis();
        String endpoint = env("SNAPSHOT_S3_IT_ENDPOINT");
        var settings = new S3SnapshotStore.Settings(endpoint, env("SNAPSHOT_S3_IT_REGION"),
                endpoint != null && !endpoint.isBlank(), env("SNAPSHOT_S3_IT_ACCESS_KEY_ID"),
                env("SNAPSHOT_S3_IT_SECRET_ACCESS_KEY"), S3SnapshotStore.Settings.MIN_PIECE_BYTES, 3);
        store = SnapshotLocations.open(env("SNAPSHOT_S3_IT_URL").replaceAll("/+$", "") + "/" + run, settings);
    }

    @AfterEach
    void cleanUp() throws Exception {
        for (String key : store.list("")) {
            store.delete(key);
        }
        assertThat(store.list("")).isEmpty();
        store.close();
    }

    @Test
    void storesSmallAndMultipartObjectsWithTheirDigest() throws Exception {
        Path small = tmp.resolve("small.bin");
        Path large = tmp.resolve("large.bin");
        byte[] bytes = new byte[12 << 20];
        new Random(7).nextBytes(bytes);
        Files.write(large, bytes);
        Files.write(small, "hello".getBytes());
        AtomicLong sent = new AtomicLong();

        store.put("a/small.bin", small, Digests.sha256Hex(small), sent::addAndGet);
        store.put("a/large.bin", large, Digests.sha256Hex(large), sent::addAndGet);

        assertThat(sent.get()).isEqualTo(5 + bytes.length);
        var info = store.head("a/large.bin").orElseThrow();
        assertThat(info.size()).isEqualTo(bytes.length);
        assertThat(info.sha256()).isEqualTo(Digests.sha256Hex(large));
        try (InputStream in = store.open("a/large.bin")) {
            assertThat(Digests.sha256Hex(in.readAllBytes())).isEqualTo(Digests.sha256Hex(bytes));
        }
        assertThat(store.list("a/")).containsExactly("a/large.bin", "a/small.bin");
        assertThat(store.head("a/missing")).isEmpty();
        assertThat(store.read("a/missing")).isEmpty();
    }

    @Test
    void conditionalWritesDetectAConcurrentChange() throws Exception {
        assertThat(store.putIfUnchanged("index.json", "a".getBytes(), null)).isTrue();
        String tag = store.read("index.json").orElseThrow().etag();
        assertThat(store.putIfUnchanged("index.json", "b".getBytes(), null)).isFalse();
        assertThat(store.putIfUnchanged("index.json", "b".getBytes(), tag)).isTrue();
        assertThat(store.putIfUnchanged("index.json", "c".getBytes(), tag)).isFalse();
        assertThat(new String(store.read("index.json").orElseThrow().bytes())).isEqualTo("b");
    }

    @Test
    void publishResolveDownloadAndStreamASnapshot() throws Exception {
        Path manifest = SnapshotRepositoryTest.snapshot(tmp.resolve("s"), "44444444-4444-4444-4444-444444444444", 100, null);
        var repo = new SnapshotRepository(store);

        repo.publish(manifest, 2, true, null);
        assertThat(repo.publish(manifest, 2, true, null).filesSent()).isZero();
        var resolved = repo.resolve("preprod", "latest", null, m -> List.of(), null);
        Path out = tmp.resolve("download");
        repo.downloadMetadata(resolved, out);
        repo.downloadParts(resolved, out, p -> true, 2, null);

        Map<String, Long> sizes = new TreeMap<>();
        resolved.manifest().tables().forEach(t -> t.files().forEach(f -> sizes.put(f.path(), f.size())));
        var source = repo.partSource(resolved);
        for (var part : resolved.manifest().parts()) {
            assertThat(Digests.sha256Hex(out.resolve(part.fileName()))).isEqualTo(part.sha256());
            try (InputStream in = source.open(part)) {
                var r = new ArchiveExtractor(tmp.resolve("x"), 100, 1 << 24).extract(in, sizes, null, f -> true, null);
                assertThat(r.partSha256()).isEqualTo(part.sha256());
            }
        }

        String publicUrl = env("SNAPSHOT_S3_IT_PUBLIC_URL");
        if (publicUrl != null && !publicUrl.isBlank()) {
            try (var http = SnapshotLocations.open(publicUrl.replaceAll("/+$", "") + "/" + run, null)) {
                var anonymous = new SnapshotRepository(http).resolve("preprod", "latest", null, m -> List.of(), null);
                assertThat(anonymous.entry().snapshotId()).isEqualTo(resolved.entry().snapshotId());
            }
        }
    }
}
