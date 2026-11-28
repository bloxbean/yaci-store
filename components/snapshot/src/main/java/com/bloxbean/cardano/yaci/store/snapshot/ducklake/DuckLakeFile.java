package com.bloxbean.cardano.yaci.store.snapshot.ducklake;

/**
 * One immutable Parquet file, addressed by its path relative to the analytics data directory.
 *
 * @param relativePath e.g. {@code main/block/date=2026-08-24/ducklake-....parquet}
 */
public record DuckLakeFile(String relativePath, long rowCount, long sizeBytes) {

    /**
     * The partition directory this file belongs to, used as the {@code WHOLE_PARTITION} batch key.
     * Returns the parent path, or an empty string for an unpartitioned relation.
     */
    public String partition() {
        int slash = relativePath.lastIndexOf('/');
        return slash < 0 ? "" : relativePath.substring(0, slash);
    }
}
