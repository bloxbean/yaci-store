package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import java.util.List;
import java.util.Map;

/**
 * Canonical description of one snapshot. Serialised by {@link ManifestCodec} with a fixed field
 * order so the same inputs always produce byte-identical JSON.
 *
 * <p>It deliberately contains no database URL, user name, password, host name or local absolute
 * path.
 */
public record SnapshotManifest(
        int formatVersion,
        String snapshotId,
        String createdAt,
        String producer,
        String yaciStoreVersion,
        String specFormatVersion,
        String duckdbVersion,
        String ducklakeFormatVersion,
        ConsistencyPoint point,
        String genesisHash,
        List<String> modules,
        Map<String, String> pruningSettings,
        String schemaFingerprint,
        String flywayFingerprint,
        List<TableManifest> tables,
        List<PartManifest> parts,
        /** Non-empty means the snapshot is knowingly not an exact database copy. */
        List<String> declaredLossy
) {

    public TableManifest table(String specId) {
        return tables.stream().filter(t -> t.specId().equals(specId)).findFirst().orElse(null);
    }

    public long totalRows() {
        return tables.stream().mapToLong(TableManifest::rowCount).sum();
    }

    /**
     * Per-table snapshot facts. {@code columnFingerprint} covers the observed Parquet column
     * names/types so schema drift fails at import preflight rather than corrupting data.
     */
    public record TableManifest(
            String specId,
            int specVersion,
            String specDigest,
            String module,
            String kind,
            String restore,
            String reason,
            String sourceRelation,
            String exporterId,
            String targetTable,
            String loadMode,
            String cutoffRule,
            int transformVersion,
            List<String> dependencies,
            long rowCount,
            List<String> key,
            String columnFingerprint,
            Map<String, String> bounds,
            List<FileEntry> files
    ) {}

    /** One packaged Parquet file, with the path it must be restored to relative to the data dir. */
    public record FileEntry(
            String path,
            long size,
            String sha256,
            long rowCount,
            String partition
    ) {}

    /** One independently downloadable and verifiable ZIP part. */
    public record PartManifest(
            int partNumber,
            String fileName,
            long size,
            String sha256,
            int entryCount,
            List<String> tables
    ) {}
}
