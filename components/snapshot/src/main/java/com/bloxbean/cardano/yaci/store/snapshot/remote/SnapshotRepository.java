package com.bloxbean.cardano.yaci.store.snapshot.remote;

import com.bloxbean.cardano.yaci.store.snapshot.archive.Parallel;
import com.bloxbean.cardano.yaci.store.snapshot.archive.PartSource;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ManifestCodec;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import com.bloxbean.cardano.yaci.store.snapshot.util.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Snapshots in a {@link SnapshotStore}, laid out as ADR 0005 (D2) describes:
 *
 * <pre>
 * &lt;network&gt;/index.json
 * &lt;network&gt;/&lt;snapshot-id&gt;/&lt;base&gt;.manifest.json, .part-NNNNN.zip, SHA256SUMS[.sig, .sig.json]
 * </pre>
 *
 * A snapshot directory holds exactly the files export wrote, so a download is byte-identical to the
 * export and every local command works on it unchanged.
 */
public class SnapshotRepository {

    private static final Logger log = LoggerFactory.getLogger(SnapshotRepository.class);

    public static final String SUMS = "SHA256SUMS";
    public static final String SIGNATURE = "SHA256SUMS.sig";
    public static final String SIGNATURE_INFO = "SHA256SUMS.sig.json";
    public static final String LATEST = "latest";

    private static final int INDEX_ATTEMPTS = 5;
    private static final int DOWNLOAD_ATTEMPTS = 3;

    /** Wait before reopening a remote part at the byte its read failed; doubled for each failure there. */
    public static final java.time.Duration RESUME_BACKOFF = java.time.Duration.ofSeconds(2);

    private final SnapshotStore store;
    private final java.time.Duration resumeBackoff;

    public SnapshotRepository(SnapshotStore store) {
        this(store, RESUME_BACKOFF);
    }

    public SnapshotRepository(SnapshotStore store, java.time.Duration resumeBackoff) {
        this.store = store;
        this.resumeBackoff = resumeBackoff;
    }

    public SnapshotStore store() {
        return store;
    }

    // ------------------------------------------------------------------ publish

    public record PublishResult(SnapshotIndex.Entry entry, String directory, long bytesSent, int filesSent,
                                int filesSkipped, long millis) {}

    /**
     * Uploads a local snapshot. Parts go first, then the checksums and signature, then the manifest,
     * then {@value SnapshotIndex#ENTRY_FILE}, and the index last, so neither ever points at an incomplete
     * snapshot. Files already present with the same size and SHA-256 are skipped, so a rerun resumes.
     *
     * <p>A snapshot published without {@code live} is staged: it is not in the index, so {@code list} and
     * {@code latest} do not see it, but it can be imported by its id. {@link #promote} makes it live.
     *
     * @param threads files uploaded at once
     */
    public PublishResult publish(Path manifestPath, int threads, Consumer<String> progress) throws IOException {
        return publish(manifestPath, threads, false, progress);
    }

    public PublishResult publish(Path manifestPath, int threads, boolean live, Consumer<String> progress) throws IOException {
        if (!store.writable()) {
            throw SnapshotStore.readOnly(store.describe());
        }
        long started = System.currentTimeMillis();
        Path dir = manifestPath.toAbsolutePath().getParent();
        byte[] manifestBytes = Files.readAllBytes(manifestPath);
        SnapshotManifest m = new ManifestCodec().parse(manifestBytes);
        String network = m.point().network();
        String snapshotDir = network + "/" + m.snapshotId() + "/";
        String manifestSha = Digests.sha256Hex(manifestBytes);

        Path sums = dir.resolve(SUMS);
        if (!Files.isRegularFile(sums) || !Files.readString(sums).contains(manifestSha + "  " + manifestPath.getFileName())) {
            throw new IOException(SUMS + " in " + dir + " does not list this manifest; publish the directory export wrote");
        }

        record Upload(String name, Path file, long size, String sha256) {}
        List<Upload> parts = new ArrayList<>();
        for (SnapshotManifest.PartManifest p : m.parts()) {
            Path f = dir.resolve(p.fileName());
            if (!Files.isRegularFile(f) || Files.size(f) != p.size()) {
                throw new IOException("Part " + p.fileName() + " is missing or has the wrong size in " + dir);
            }
            parts.add(new Upload(p.fileName(), f, p.size(), p.sha256()));
        }
        List<Upload> tail = new ArrayList<>();
        tail.add(new Upload(SUMS, sums, Files.size(sums), Digests.sha256Hex(sums)));
        for (String name : List.of(SIGNATURE, SIGNATURE_INFO)) {
            Path f = dir.resolve(name);
            if (Files.isRegularFile(f)) {
                tail.add(new Upload(name, f, Files.size(f), Digests.sha256Hex(f)));
            }
        }
        tail.add(new Upload(manifestPath.getFileName().toString(), manifestPath, manifestBytes.length, manifestSha));

        long total = parts.stream().mapToLong(Upload::size).sum() + tail.stream().mapToLong(Upload::size).sum();
        AtomicLong sent = new AtomicLong();
        AtomicInteger skipped = new AtomicInteger();
        AtomicInteger uploaded = new AtomicInteger();
        say(progress, "publish", String.format("Uploading snapshot %s (%d parts, %s) to %s", m.snapshotId(),
                parts.size(), Progress.bytes(total), store.describe()));
        Function<Upload, Void> upload = u -> {
            try {
                String key = snapshotDir + u.name();
                if (alreadyThere(key, u.size(), u.sha256())) {
                    skipped.incrementAndGet();
                    sent.addAndGet(u.size());
                    return null;
                }
                store.put(key, u.file(), u.sha256(), sent::addAndGet);
                uploaded.incrementAndGet();
                return null;
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        };
        try (Progress.Ticker ignored = Progress.every(Progress.INTERVAL_MILLIS, "publish",
                () -> Progress.rate(sent.get(), total, started, System.currentTimeMillis()), progress)) {
            Parallel.map(parts, Math.max(1, threads), upload::apply);
            for (Upload u : tail) {
                upload.apply(u);
            }
        } catch (Parallel.PartFailure | java.io.UncheckedIOException e) {
            Throwable c = e.getCause();
            throw c instanceof IOException io ? io : new IOException(e.getMessage(), c);
        }

        String keyId = signatureKeyId(dir.resolve(SIGNATURE_INFO));
        SnapshotIndex.Entry entry = SnapshotIndex.Entry.of(m, m.snapshotId() + "/" + manifestPath.getFileName(),
                manifestSha, keyId).withLive(live);
        // A republish keeps a live snapshot live.
        List<SnapshotIndex.Entry> indexed = index(network).snapshots();
        boolean listed = indexed != null && indexed.stream().anyMatch(e -> e.snapshotId().equals(m.snapshotId()));
        if (live || listed) {
            entry = entry.withLive(true);
            store.putBytes(snapshotDir + SnapshotIndex.ENTRY_FILE, entry.toBytes());
            SnapshotIndex.Entry listedEntry = entry;
            updateIndex(network, m.point().protocolMagic(), index -> index.with(listedEntry, Instant.now().toString()));
        } else {
            store.putBytes(snapshotDir + SnapshotIndex.ENTRY_FILE, entry.toBytes());
        }
        say(progress, "publish", String.format("Published %s: %d file(s) sent, %d already there, %s",
                m.snapshotId(), uploaded.get(), skipped.get(), entry.live() ? "live in the index"
                        : "staged (not in the index; import it by id, then promote it)"));
        return new PublishResult(entry, snapshotDir, sent.get(), uploaded.get(), skipped.get(),
                System.currentTimeMillis() - started);
    }

    private boolean alreadyThere(String key, long size, String sha256) throws IOException {
        var info = store.head(key);
        if (info.isEmpty() || info.get().size() != size) {
            return false;
        }
        if (info.get().sha256() != null) {
            return info.get().sha256().equals(sha256);
        }
        // No recorded digest (a local mirror): read it back.
        try (InputStream in = store.open(key)) {
            MessageDigest md = Digests.sha256();
            byte[] buf = new byte[1 << 20];
            int r;
            while ((r = in.read(buf)) != -1) {
                md.update(buf, 0, r);
            }
            return Digests.hex(md.digest()).equals(sha256);
        }
    }

    private static String signatureKeyId(Path info) throws IOException {
        if (!Files.isRegularFile(info)) {
            return null;
        }
        var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(info.toFile());
        return node.hasNonNull("keyId") ? node.get("keyId").asText() : null;
    }

    // ------------------------------------------------------------------ index

    public SnapshotIndex index(String network) throws IOException {
        return store.read(network + "/" + SnapshotIndex.FILE).map(t -> SnapshotIndex.parse(t.bytes()))
                .orElse(SnapshotIndex.empty(network, 0));
    }

    /** Read, change and write back the index, retrying when another publisher changed it meanwhile. */
    private SnapshotIndex updateIndex(String network, long magic, java.util.function.UnaryOperator<SnapshotIndex> change)
            throws IOException {
        String key = network + "/" + SnapshotIndex.FILE;
        for (int attempt = 1; attempt <= INDEX_ATTEMPTS; attempt++) {
            var current = store.read(key);
            SnapshotIndex before = current.map(t -> SnapshotIndex.parse(t.bytes())).orElse(SnapshotIndex.empty(network, magic));
            if (before.protocolMagic() == 0) {
                before = new SnapshotIndex(before.formatVersion(), network, magic, before.updatedAt(), before.snapshots());
            }
            SnapshotIndex after = change.apply(before);
            if (store.putIfUnchanged(key, after.toBytes(), current.map(SnapshotStore.Tagged::etag).orElse(null))) {
                return after;
            }
            log.info("{} changed while it was being updated; retrying ({}/{})", key, attempt, INDEX_ATTEMPTS);
        }
        throw new IOException(key + " kept changing while it was being updated; retry the command");
    }

    /**
     * Every snapshot in the store for {@code network}, live or staged, from the manifests themselves.
     * Needs a store that can list.
     */
    public List<SnapshotIndex.Entry> scan(String network, Consumer<String> progress) throws IOException {
        List<SnapshotIndex.Entry> entries = new ArrayList<>();
        for (String key : store.list(network + "/")) {
            if (!key.endsWith(".manifest.json")) {
                continue;
            }
            byte[] bytes = store.read(key).orElseThrow().bytes();
            SnapshotManifest m = new ManifestCodec().parse(bytes);
            String relative = key.substring(network.length() + 1);
            String dir = relative.contains("/") ? relative.substring(0, relative.lastIndexOf('/') + 1) : "";
            String keyId = null;
            var info = store.read(network + "/" + dir + SIGNATURE_INFO);
            if (info.isPresent()) {
                var node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(info.get().bytes());
                keyId = node.hasNonNull("keyId") ? node.get("keyId").asText() : null;
            }
            boolean live = store.read(network + "/" + dir + SnapshotIndex.ENTRY_FILE)
                    .map(t -> SnapshotIndex.Entry.parse(t.bytes()).live()).orElse(false);
            entries.add(SnapshotIndex.Entry.of(m, relative, Digests.sha256Hex(bytes), keyId).withLive(live));
            say(progress, "index", "Found " + m.snapshotId() + " (epoch " + m.point().epoch() + ", "
                    + (live ? "live" : "staged") + ")");
        }
        return SnapshotIndex.sorted(entries);
    }

    /**
     * Regenerates the network's index from the snapshots in the store that are live, for a store edited
     * by hand. Needs a store that can list.
     */
    public SnapshotIndex rebuildIndex(String network, long protocolMagic, Consumer<String> progress) throws IOException {
        List<SnapshotIndex.Entry> live = scan(network, progress).stream().filter(SnapshotIndex.Entry::live).toList();
        return updateIndex(network, protocolMagic, before ->
                new SnapshotIndex(SnapshotIndex.FORMAT_VERSION, network, protocolMagic, Instant.now().toString(), live));
    }

    /**
     * Makes a staged snapshot live: its {@value SnapshotIndex#ENTRY_FILE} says so, and the index lists
     * it. The manifest is checked against the entry's digest first.
     */
    public SnapshotIndex.Entry promote(String network, String snapshotId) throws IOException {
        SnapshotIndex.Entry entry = stagedEntry(network, snapshotId).withLive(true);
        fetch(network, entry);
        store.putBytes(network + "/" + entry.directory() + SnapshotIndex.ENTRY_FILE, entry.toBytes());
        long magic = new ManifestCodec().parse(store.read(network + "/" + entry.manifest()).orElseThrow().bytes())
                .point().protocolMagic();
        updateIndex(network, magic, index -> index.with(entry, Instant.now().toString()));
        return entry;
    }

    /** Takes a live snapshot out of the index; its files stay, and it can still be imported by its id. */
    public SnapshotIndex.Entry unlist(String network, String snapshotId) throws IOException {
        SnapshotIndex.Entry entry = stagedEntry(network, snapshotId).withLive(false);
        store.putBytes(network + "/" + entry.directory() + SnapshotIndex.ENTRY_FILE, entry.toBytes());
        updateIndex(network, 0, index -> index.without(entry.snapshotId(), Instant.now().toString()));
        return entry;
    }

    /** The entry in {@code <network>/<id>/snapshot.json}; ids are matched exactly. */
    private SnapshotIndex.Entry stagedEntry(String network, String snapshotId) throws IOException {
        if (snapshotId == null || snapshotId.isBlank() || snapshotId.contains("/") || snapshotId.contains("..")) {
            throw new IOException("Invalid snapshot id '" + snapshotId + "'");
        }
        return store.read(network + "/" + snapshotId + "/" + SnapshotIndex.ENTRY_FILE)
                .map(t -> SnapshotIndex.Entry.parse(t.bytes()))
                .orElseThrow(() -> new IOException("Snapshot " + snapshotId + " is not published for " + network
                        + " at " + store.describe() + " (pass the full id)"));
    }

    // ------------------------------------------------------------------ resolve

    /** A snapshot chosen from the index (or, when staged, by its id), with its verified manifest. */
    public record Resolved(String network, SnapshotIndex.Entry entry, byte[] manifestBytes, SnapshotManifest manifest) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Resolved r && Objects.equals(network, r.network) && Objects.equals(entry, r.entry)
                    && Arrays.equals(manifestBytes, r.manifestBytes) && Objects.equals(manifest, r.manifest);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * (31 * Objects.hashCode(network) + Objects.hashCode(entry))
                    + Arrays.hashCode(manifestBytes)) + Objects.hashCode(manifest);
        }

        /** The manifest bytes by their count; the parsed manifest is printed in full. */
        @Override
        public String toString() {
            return "Resolved[network=" + network + ", entry=" + entry + ", manifestBytes="
                    + (manifestBytes == null ? "null" : manifestBytes.length + " bytes") + ", manifest=" + manifest + "]";
        }

        public String directory() {
            return network + "/" + entry.directory();
        }

        public String manifestFileName() {
            String key = entry.manifest();
            return key.substring(key.lastIndexOf('/') + 1);
        }
    }

    public Resolved resolve(String network, String snapshot, Function<SnapshotManifest, List<String>> problems,
                            Consumer<String> progress) throws IOException {
        return resolve(network, snapshot, null, problems, progress);
    }

    /**
     * Picks a snapshot: an id (or a unique prefix of a live one's), or {@value #LATEST} for the newest
     * live snapshot that this release can import. A staged snapshot is found only by its full id. Each
     * candidate's manifest is fetched and checked against the digest in its entry, then against
     * {@code problems}; a {@value #LATEST} candidate with problems is skipped, an explicitly named one is
     * refused.
     *
     * @param flavor   unused; kept for callers built before every snapshot was full
     * @param problems what stops this release importing a manifest; empty when it can
     */
    public Resolved resolve(String network, String snapshot, String flavor,
                            Function<SnapshotManifest, List<String>> problems, Consumer<String> progress) throws IOException {
        SnapshotIndex index = index(network);
        List<SnapshotIndex.Entry> entries = index.snapshots() == null ? List.of() : index.snapshots();
        boolean latest = snapshot == null || snapshot.isBlank() || LATEST.equalsIgnoreCase(snapshot);
        List<SnapshotIndex.Entry> candidates;
        if (latest) {
            candidates = entries;
            if (candidates.isEmpty()) {
                throw new IOException("No live snapshot is published for " + network + " at " + store.describe());
            }
        } else {
            candidates = entries.stream().filter(e -> e.snapshotId().equals(snapshot)
                    || e.snapshotId().startsWith(snapshot)).toList();
            if (candidates.isEmpty()) {
                candidates = List.of(stagedEntry(network, snapshot));
                say(progress, "resolve", "Snapshot " + snapshot + " is staged, not live: importing it by id");
            }
            if (candidates.size() > 1) {
                throw new IOException("Snapshot id " + snapshot + " is ambiguous: "
                        + candidates.stream().map(SnapshotIndex.Entry::snapshotId).toList());
            }
        }
        List<String> skipped = new ArrayList<>();
        for (SnapshotIndex.Entry e : candidates) {
            Resolved r = fetch(network, e);
            List<String> p = problems == null ? List.of() : problems.apply(r.manifest());
            if (p.isEmpty()) {
                return r;
            }
            if (!latest) {
                throw new IOException("Snapshot " + e.snapshotId() + " cannot be imported by this release:\n  - "
                        + String.join("\n  - ", p));
            }
            String reason = e.snapshotId() + " (epoch " + e.epoch() + ", Yaci Store " + e.release() + "): " + p.get(0);
            skipped.add(reason);
            say(progress, "resolve", "Skipping " + reason);
        }
        throw new IOException("None of the " + candidates.size() + " live snapshot(s) can be imported by this release:\n  - "
                + String.join("\n  - ", skipped));
    }

    private Resolved fetch(String network, SnapshotIndex.Entry e) throws IOException {
        byte[] bytes = store.read(network + "/" + e.manifest()).orElseThrow(
                () -> new IOException("The index lists " + e.manifest() + " but it is not in " + store.describe())).bytes();
        String sha = Digests.sha256Hex(bytes);
        if (!sha.equals(e.manifestSha256())) {
            throw new IOException("Manifest " + e.manifest() + " does not match the digest in the index");
        }
        return new Resolved(network, e, bytes, new ManifestCodec().parse(bytes));
    }

    // ------------------------------------------------------------------ download

    /**
     * Writes the manifest, the checksums (and signature) to {@code outDir}. Parts are not fetched here.
     *
     * @return the local manifest
     */
    public Path downloadMetadata(Resolved r, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path manifest = outDir.resolve(r.manifestFileName());
        Files.write(manifest, r.manifestBytes());
        for (String name : List.of(SUMS, SIGNATURE, SIGNATURE_INFO)) {
            var obj = store.read(r.directory() + name);
            if (obj.isPresent()) {
                Files.write(outDir.resolve(name), obj.get().bytes());
            } else if (name.equals(SUMS)) {
                throw new IOException(SUMS + " is missing for snapshot " + r.entry().snapshotId());
            }
        }
        return manifest;
    }

    /**
     * Downloads the parts {@code wanted} accepts into {@code outDir}, several at once, checking each
     * part's size and SHA-256 as it arrives and retrying a failed one. A part already present with
     * the right digest is kept, so a rerun resumes.
     */
    public void downloadParts(Resolved r, Path outDir, Predicate<SnapshotManifest.PartManifest> wanted, int threads,
                              Consumer<String> progress) throws IOException {
        List<SnapshotManifest.PartManifest> parts = r.manifest().parts().stream().filter(wanted).toList();
        long total = parts.stream().mapToLong(SnapshotManifest.PartManifest::size).sum();
        long started = System.currentTimeMillis();
        AtomicLong received = new AtomicLong();
        say(progress, "download", String.format("Downloading %d part(s) (%s) from %s", parts.size(), Progress.bytes(total),
                store.describe()));
        try (Progress.Ticker ignored = Progress.every(Progress.INTERVAL_MILLIS, "download",
                () -> Progress.rate(received.get(), total, started, System.currentTimeMillis()), progress)) {
            Parallel.map(parts, Math.max(1, threads), p -> {
                Path target = outDir.resolve(p.fileName());
                if (Files.isRegularFile(target) && Files.size(target) == p.size()
                        && Digests.sha256Hex(target).equals(p.sha256())) {
                    received.addAndGet(p.size());
                    return null;
                }
                for (int attempt = 1; ; attempt++) {
                    AtomicLong thisAttempt = new AtomicLong();
                    try {
                        downloadPart(r, p, target, n -> {
                            thisAttempt.addAndGet(n);
                            received.addAndGet(n);
                        });
                        return null;
                    } catch (IOException e) {
                        received.addAndGet(-thisAttempt.get());
                        if (attempt >= DOWNLOAD_ATTEMPTS) {
                            throw e;
                        }
                        say(progress, "download", "Retrying " + p.fileName() + " (" + e.getMessage() + ")");
                    }
                }
            });
        } catch (Parallel.PartFailure e) {
            throw e.getCause() instanceof IOException io ? io : new IOException(e.getMessage(), e.getCause());
        }
        say(progress, "download", String.format("Downloaded %d part(s) (%s) in %s", parts.size(), Progress.bytes(total),
                Progress.duration(System.currentTimeMillis() - started)));
    }

    private void downloadPart(Resolved r, SnapshotManifest.PartManifest p, Path target, java.util.function.LongConsumer read)
            throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".download");
        MessageDigest md = Digests.sha256();
        long n = 0;
        try (InputStream in = new DigestInputStream(openPart(r, p), md);
             OutputStream out = Files.newOutputStream(tmp)) {
            byte[] buf = new byte[1 << 20];
            int k;
            while ((k = in.read(buf)) != -1) {
                n += k;
                if (n > p.size()) {
                    throw new IOException(p.fileName() + " is larger than the " + p.size() + " bytes the manifest declares");
                }
                out.write(buf, 0, k);
                read.accept(k);
            }
        }
        if (n != p.size() || !Digests.hex(md.digest()).equals(p.sha256())) {
            Files.deleteIfExists(tmp);
            throw new IOException(p.fileName() + " does not match the manifest (size or SHA-256)");
        }
        Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /** The resolved snapshot's parts, streamed from the store into an import. */
    public PartSource partSource(Resolved r) {
        return new PartSource() {
            @Override
            public String describe() {
                return store.describe() + r.directory();
            }

            @Override
            public boolean remote() {
                return !(store instanceof LocalSnapshotStore);
            }

            @Override
            public OptionalLong size(SnapshotManifest.PartManifest part) throws IOException {
                return store.head(r.directory() + part.fileName()).map(i -> OptionalLong.of(i.size()))
                        .orElse(OptionalLong.empty());
            }

            @Override
            public InputStream open(SnapshotManifest.PartManifest part) throws IOException {
                return openPart(r, part);
            }
        };
    }

    /** A part's bytes; a remote read that fails part way resumes from the next byte. */
    private InputStream openPart(Resolved r, SnapshotManifest.PartManifest p) throws IOException {
        String key = r.directory() + p.fileName();
        return store instanceof LocalSnapshotStore ? store.open(key)
                : new ResumingInputStream(store, key, p.size(), resumeBackoff);
    }

    /** One line per entry, for {@code snapshot list}. */
    public static Map<String, String> describe(SnapshotIndex.Entry e) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("id", e.snapshotId());
        out.put("epoch", Integer.toString(e.epoch()));
        out.put("block", Long.toString(e.blockNumber()));
        out.put("size", Progress.bytes(e.sizeBytes()));
        out.put("release", e.release());
        out.put("signed", e.keyId() == null ? "no" : "key " + e.keyId());
        out.put("state", e.live() ? "live" : "staged");
        out.put("created", e.createdAt());
        return out;
    }

    private static void say(Consumer<String> progress, String phase, String message) {
        log.info(message);
        if (progress != null) {
            progress.accept("[" + phase + "] " + message);
        }
    }
}
