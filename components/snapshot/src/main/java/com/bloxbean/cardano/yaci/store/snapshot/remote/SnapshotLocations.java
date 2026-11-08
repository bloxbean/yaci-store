package com.bloxbean.cardano.yaci.store.snapshot.remote;

import java.net.URI;
import java.nio.file.Path;

/**
 * Opens a store from a location string (ADR 0005, D1):
 * {@code s3://bucket/prefix}, {@code https://host/prefix} (or {@code http://}), {@code file:///dir} or a
 * plain directory path.
 */
public final class SnapshotLocations {

    private SnapshotLocations() {
    }

    /** @param s3 used only for an {@code s3://} location */
    public static SnapshotStore open(String location, S3SnapshotStore.Settings s3) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("No snapshot location: pass --remote or set store.snapshot.remote.url");
        }
        String l = location.trim();
        if (l.startsWith("s3://")) {
            String rest = l.substring("s3://".length());
            int slash = rest.indexOf('/');
            String bucket = slash < 0 ? rest : rest.substring(0, slash);
            String prefix = slash < 0 ? "" : rest.substring(slash + 1);
            if (bucket.isBlank()) {
                throw new IllegalArgumentException("No bucket in " + location);
            }
            if (s3 == null) {
                throw new IllegalArgumentException("S3 settings are required for " + location);
            }
            return new S3SnapshotStore(bucket, prefix, s3);
        }
        if (l.startsWith("https://") || l.startsWith("http://")) {
            return new HttpSnapshotStore(URI.create(l));
        }
        if (l.startsWith("file:")) {
            return new LocalSnapshotStore(Path.of(URI.create(l)));
        }
        if (l.contains("://")) {
            throw new IllegalArgumentException("Unsupported snapshot location " + location
                    + "; use s3://, https://, file:// or a directory");
        }
        return new LocalSnapshotStore(Path.of(l));
    }
}
