package com.bloxbean.cardano.yaci.store.snapshot.remote;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.LongConsumer;

/**
 * A public bucket or CDN, read anonymously over HTTP(S). Read-only, and it cannot list: snapshots
 * are found through the published {@code index.json}.
 */
public class HttpSnapshotStore implements SnapshotStore {

    /** A download that sends nothing for this long is failed, so it can be retried. */
    public static final Duration DEFAULT_STALL_TIMEOUT = Duration.ofMinutes(2);

    private final URI base;
    private final HttpClient client;
    private final Duration stallTimeout;

    public HttpSnapshotStore(URI base) {
        this(base, DEFAULT_STALL_TIMEOUT);
    }

    public HttpSnapshotStore(URI base, Duration stallTimeout) {
        this.stallTimeout = stallTimeout;
        String b = base.toString();
        this.base = URI.create(b.endsWith("/") ? b : b + "/");
        this.client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    @Override
    public String describe() {
        return base.toString();
    }

    @Override
    public boolean writable() {
        return false;
    }

    private URI uri(String key) {
        return base.resolve(key);
    }

    @Override
    public Optional<ObjectInfo> head(String key) throws IOException {
        HttpResponse<Void> r = send(HttpRequest.newBuilder(uri(key)).method("HEAD", HttpRequest.BodyPublishers.noBody()),
                HttpResponse.BodyHandlers.discarding());
        if (r.statusCode() == 404 || r.statusCode() == 403) {
            return Optional.empty();
        }
        check(r, key);
        long size = r.headers().firstValueAsLong("content-length").orElse(-1);
        return Optional.of(new ObjectInfo(size, r.headers().firstValue("etag").orElse(null),
                r.headers().firstValue("x-amz-meta-sha256").orElse(null)));
    }

    @Override
    public InputStream open(String key) throws IOException {
        HttpResponse<InputStream> r = send(HttpRequest.newBuilder(uri(key)).GET(), HttpResponse.BodyHandlers.ofInputStream());
        if (r.statusCode() == 404 || r.statusCode() == 403) {
            r.body().close();
            throw new NoSuchFileException(key + " is not at " + base);
        }
        if (r.statusCode() / 100 != 2) {
            r.body().close();
            throw new IOException("GET " + uri(key) + " returned HTTP " + r.statusCode());
        }
        return new StallGuard(r.body(), stallTimeout, "GET " + uri(key));
    }

    @Override
    public Optional<Tagged> read(String key) throws IOException {
        HttpResponse<byte[]> r = send(HttpRequest.newBuilder(uri(key)).GET(), HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() == 404 || r.statusCode() == 403) {
            return Optional.empty();
        }
        check(r, key);
        return Optional.of(new Tagged(r.body(), r.headers().firstValue("etag").orElse(null)));
    }

    @Override
    public List<String> list(String prefix) {
        throw new UnsupportedOperationException(base + " cannot be listed; snapshots are found through index.json");
    }

    @Override
    public void put(String key, Path file, String sha256, LongConsumer sent) {
        throw SnapshotStore.readOnly(describe());
    }

    @Override
    public boolean putIfUnchanged(String key, byte[] bytes, String ifMatch) {
        throw SnapshotStore.readOnly(describe());
    }

    @Override
    public void delete(String key) {
        throw SnapshotStore.readOnly(describe());
    }

    private <T> HttpResponse<T> send(HttpRequest.Builder request, HttpResponse.BodyHandler<T> handler) throws IOException {
        try {
            return client.send(request.timeout(Duration.ofMinutes(5)).build(), handler);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted", e);
        }
    }

    private void check(HttpResponse<?> r, String key) throws IOException {
        if (r.statusCode() / 100 != 2) {
            throw new IOException(r.request().method() + " " + uri(key) + " returned HTTP " + r.statusCode());
        }
    }
}
