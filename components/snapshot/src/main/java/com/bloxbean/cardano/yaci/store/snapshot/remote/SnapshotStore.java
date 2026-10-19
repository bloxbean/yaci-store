package com.bloxbean.cardano.yaci.store.snapshot.remote;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
    record Tagged(byte[] bytes, String etag) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Tagged t && Arrays.equals(bytes, t.bytes) && Objects.equals(etag, t.etag);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(bytes) + Objects.hashCode(etag);
        }

        @Override
        public String toString() {
            return "Tagged[bytes=" + (bytes == null ? "null" : bytes.length + " bytes") + ", etag=" + etag + "]";
        }
    }

    /** The location, without credentials. */
    String describe();

    /** False for a read-only location such as plain HTTPS. */
    boolean writable();

    Optional<ObjectInfo> head(String key) throws IOException;

    /** The whole object, streamed. The caller closes it. */
    InputStream open(String key) throws IOException;

    /**
     * The object from byte {@code offset} to its end, to resume a read that failed. The default reads
     * the object from the start and discards the bytes before {@code offset}.
     */
    default InputStream open(String key, long offset) throws IOException {
        InputStream in = open(key);
        try {
            in.skipNBytes(offset);
        } catch (IOException e) {
            in.close();
            throw e;
        }
        return in;
    }

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

    /** Writes {@code bytes} at {@code key}, replacing what is there. */
    default void putBytes(String key, byte[] bytes) throws IOException {
        Path tmp = ownerOnlyTempFile();
        try {
            Files.write(tmp, bytes);
            put(key, tmp, com.bloxbean.cardano.yaci.store.snapshot.util.Digests.sha256Hex(bytes), n -> { });
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    /**
     * A new file in the shared temp directory that only this user can read or write. On a file system
     * without POSIX permissions (Windows) the temp directory is already the user's own.
     */
    private static Path ownerOnlyTempFile() throws IOException {
        FileAttribute<?>[] ownerOnly = FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
                ? new FileAttribute<?>[]{PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))}
                : new FileAttribute<?>[0];
        return Files.createTempFile("yaci-snapshot-", ".tmp", ownerOnly);
    }

    @Override
    default void close() {
    }

    static UnsupportedOperationException readOnly(String location) {
        return new UnsupportedOperationException(location + " is read-only; publish to an s3:// or local location");
    }
}
