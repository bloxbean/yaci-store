package com.bloxbean.cardano.yaci.store.snapshot.remote;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.LongConsumer;

/**
 * Where snapshots are published to and downloaded from (ADR 0005, D1): an S3-compatible bucket, a
 * public HTTPS location or a local directory. Keys are {@code /}-separated and relative to the
 * location's base, e.g. {@code mainnet/<snapshot-id>/SHA256SUMS}.
 *
 * <p>Implementations never log or return credentials; {@link #describe()} is safe to print.
 */
public interface SnapshotStore extends AutoCloseable {

    /** An object's size, entity tag and the SHA-256 recorded when it was published, when known. */
    record ObjectInfo(long size, String etag, String sha256) {}

    /** A small object's content and the entity tag to pass to {@link #putIfUnchanged}. */
    record Tagged(byte[] bytes, String etag) {}

    /** The location, without credentials. */
    String describe();

    /** False for a read-only location such as plain HTTPS. */
    boolean writable();

    Optional<ObjectInfo> head(String key) throws IOException;

    /** The whole object, streamed. The caller closes it. */
    InputStream open(String key) throws IOException;

    /** A small object (an index, a manifest) and its entity tag; empty when it does not exist. */
    Optional<Tagged> read(String key) throws IOException;

    /** Every key under {@code prefix}, at any depth. */
    List<String> list(String prefix) throws IOException;

    /**
     * Uploads a file, recording its SHA-256 with it.
     *
     * @param sent told the bytes sent as they go, for progress; may be null
     */
    void put(String key, Path file, String sha256, LongConsumer sent) throws IOException;

    /**
     * Writes a small object, only if it is still in the expected state: {@code ifMatch} is the entity
     * tag the caller last read, or null to require that the object does not exist yet.
     *
     * @return false when another writer changed the object first
     */
    boolean putIfUnchanged(String key, byte[] bytes, String ifMatch) throws IOException;

    /** Removes an object; nothing happens when it does not exist. */
    void delete(String key) throws IOException;

    @Override
    default void close() {
    }

    static UnsupportedOperationException readOnly(String location) {
        return new UnsupportedOperationException(location + " is read-only; publish to an s3:// or local location");
    }
}
