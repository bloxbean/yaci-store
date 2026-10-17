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
        List<String> declaredLossy,
        Map<String, String> tableSchemaFingerprints,
        /**
         * {@code full}, or {@code pruned} for a snapshot that keeps only {@link #retention}'s rows. Null means
         * full. Exports write full snapshots, with pruneable history in parts of its own (ADR 0005, D11).
         */
        String flavor,
        /**
         * What a pruned or import-time-reduced snapshot keeps, e.g. {@code epoch_stake.epochs=8}. Empty
         * or null for a full copy. See {@code Retention}.
         */
        Map<String, String> retention,
        /** Git commit the exporting release was built from, when known; release versions do not carry it. */
        String yaciStoreCommit
) {

    public static final String FLAVOR_FULL = "full";
    public static final String FLAVOR_PRUNED = "pruned";

    public SnapshotManifest(int formatVersion, String snapshotId, String createdAt, String producer,
                            String yaciStoreVersion, String specFormatVersion, String duckdbVersion,
                            String ducklakeFormatVersion, ConsistencyPoint point, String genesisHash,
                            List<String> modules, Map<String, String> pruningSettings, String schemaFingerprint,
                            String flywayFingerprint, List<TableManifest> tables, List<PartManifest> parts,
                            List<String> declaredLossy, Map<String, String> tableSchemaFingerprints, String flavor,
                            Map<String, String> retention) {
        this(formatVersion, snapshotId, createdAt, producer, yaciStoreVersion, specFormatVersion,
                duckdbVersion, ducklakeFormatVersion, point, genesisHash, modules, pruningSettings,
                schemaFingerprint, flywayFingerprint, tables, parts, declaredLossy, tableSchemaFingerprints,
                flavor, retention, null);
    }

    public SnapshotManifest(int formatVersion, String snapshotId, String createdAt, String producer,
                            String yaciStoreVersion, String specFormatVersion, String duckdbVersion,
                            String ducklakeFormatVersion, ConsistencyPoint point, String genesisHash,
                            List<String> modules, Map<String, String> pruningSettings, String schemaFingerprint,
                            String flywayFingerprint, List<TableManifest> tables, List<PartManifest> parts,
                            List<String> declaredLossy, Map<String, String> tableSchemaFingerprints) {
        this(formatVersion, snapshotId, createdAt, producer, yaciStoreVersion, specFormatVersion,
                duckdbVersion, ducklakeFormatVersion, point, genesisHash, modules, pruningSettings,
                schemaFingerprint, flywayFingerprint, tables, parts, declaredLossy, tableSchemaFingerprints,
                null, null);
    }

    public SnapshotManifest(int formatVersion, String snapshotId, String createdAt, String producer,
                            String yaciStoreVersion, String specFormatVersion, String duckdbVersion,
                            String ducklakeFormatVersion, ConsistencyPoint point, String genesisHash,
                            List<String> modules, Map<String, String> pruningSettings, String schemaFingerprint,
                            String flywayFingerprint, List<TableManifest> tables, List<PartManifest> parts,
                            List<String> declaredLossy) {
        this(formatVersion, snapshotId, createdAt, producer, yaciStoreVersion, specFormatVersion,
                duckdbVersion, ducklakeFormatVersion, point, genesisHash, modules, pruningSettings,
                schemaFingerprint, flywayFingerprint, tables, parts, declaredLossy, Map.of());
    }

    /** The flavor, {@link #FLAVOR_FULL} when the manifest has none. */
    public String flavorOrFull() {
        return flavor == null ? FLAVOR_FULL : flavor;
    }

    /** {@code version}, with the commit when the version does not already name it. */
    public String release() {
        return yaciStoreCommit == null || yaciStoreCommit.isBlank()
                || (yaciStoreVersion != null && yaciStoreVersion.contains(yaciStoreCommit))
                ? yaciStoreVersion : yaciStoreVersion + " (" + yaciStoreCommit + ")";
    }

    /** {@code key=value} of every history split, e.g. {@code address_utxo.spent-safe-blocks=2160}. */
    public Map<String, String> historySplits() {
        Map<String, String> out = new java.util.TreeMap<>();
        tables.stream().filter(t -> t.historySplit() != null)
                .forEach(t -> out.put(t.historySplit().key(), t.historySplit().value()));
        return out;
    }

    public Map<String, String> retentionOrEmpty() {
        return retention == null ? Map.of() : retention;
    }

    /** A copy with different tables and retention; everything else, the parts included, is unchanged. */
    public SnapshotManifest withTables(List<TableManifest> newTables, String newFlavor, Map<String, String> newRetention) {
        return new SnapshotManifest(formatVersion, snapshotId, createdAt, producer, yaciStoreVersion,
                specFormatVersion, duckdbVersion, ducklakeFormatVersion, point, genesisHash, modules,
                pruningSettings, schemaFingerprint, flywayFingerprint, newTables, parts, declaredLossy,
                tableSchemaFingerprints, newFlavor, newRetention, yaciStoreCommit);
    }

    public TableManifest table(String specId) {
        return tables.stream().filter(t -> t.specId().equals(specId)).findFirst().orElse(null);
    }

    public long totalRows() {
        return tables.stream().mapToLong(TableManifest::rowCount).sum();
    }

    /**
     * Per-table snapshot facts. {@code columnFingerprint} covers the observed Parquet column
     * names/types so schema drift fails at import preflight rather than corrupting data.
     *
     * <p>{@code specDigest} is the exporting release's whole specification, import side included.
     * {@code contractDigest} is its export contract alone ({@code SnapshotTableSpec.contractDigest()}),
     * and is what an importer requires to match. Manifests written before it existed have none and
     * still require the whole specification to match.
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
            /**
             * Source column name to DuckLake type, as recorded by the producing catalog. The importer
             * plans against these rather than the types DuckDB infers when reading Parquet.
             */
            Map<String, String> sourceColumns,
            Map<String, String> bounds,
            List<FileEntry> files,
            String contractDigest,
            /**
             * Set when the table has a pruned tier ({@link FileEntry#tier}): what an import that prunes
             * this table loads instead of the whole table. Null for a table without one.
             */
            HistorySplit historySplit
    ) {
        public TableManifest(String specId, int specVersion, String specDigest, String module, String kind,
                             String restore, String reason, String sourceRelation, String exporterId,
                             String targetTable, String loadMode, String cutoffRule, int transformVersion,
                             List<String> dependencies, long rowCount, List<String> key,
                             String columnFingerprint, Map<String, String> sourceColumns,
                             Map<String, String> bounds, List<FileEntry> files, String contractDigest) {
            this(specId, specVersion, specDigest, module, kind, restore, reason, sourceRelation, exporterId,
                    targetTable, loadMode, cutoffRule, transformVersion, dependencies, rowCount, key,
                    columnFingerprint, sourceColumns, bounds, files, contractDigest, null);
        }

        public TableManifest(String specId, int specVersion, String specDigest, String module, String kind,
                             String restore, String reason, String sourceRelation, String exporterId,
                             String targetTable, String loadMode, String cutoffRule, int transformVersion,
                             List<String> dependencies, long rowCount, List<String> key,
                             String columnFingerprint, Map<String, String> sourceColumns,
                             Map<String, String> bounds, List<FileEntry> files) {
            this(specId, specVersion, specDigest, module, kind, restore, reason, sourceRelation, exporterId,
                    targetTable, loadMode, cutoffRule, transformVersion, dependencies, rowCount, key,
                    columnFingerprint, sourceColumns, bounds, files, null);
        }

        /** The same table with only {@code keptFiles}, their row count and bounds, and no history split. */
        public TableManifest reduced(List<FileEntry> keptFiles, long keptRows, Map<String, String> keptBounds) {
            return new TableManifest(specId, specVersion, specDigest, module, kind, restore, reason, sourceRelation,
                    exporterId, targetTable, loadMode, cutoffRule, transformVersion, dependencies, keptRows, key,
                    columnFingerprint, sourceColumns, keptBounds, keptFiles, contractDigest, null);
        }
    }

    /**
     * The pruned tier of a table (ADR 0005, D11). An import whose retention includes {@code key=value}
     * loads the files not marked {@link FileEntry#TIER_FULL}: {@code rowCount} rows, with {@code bounds}.
     * A full import loads the files not marked {@link FileEntry#TIER_PRUNED} and expects the table's own
     * count and bounds.
     *
     * @param key   a {@code Retention} key, e.g. {@code address_utxo.spent-safe-blocks}
     * @param value the window the split was made with, e.g. {@code 2160}
     */
    public record HistorySplit(String key, String value, long rowCount, Map<String, String> bounds) {}

    /**
     * One packaged Parquet file, with the path it must be restored to relative to the data dir.
     *
     * @param tier which imports load the file (ADR 0005, D11): {@link #TIER_FULL} only a full import,
     *             {@link #TIER_PRUNED} only an import that prunes the table, null both. Null is omitted
     *             from the JSON, so manifests without tiers are unchanged.
     */
    public record FileEntry(
            String path,
            long size,
            String sha256,
            long rowCount,
            String partition,
            String tier
    ) {
        /** Only a full import loads it: the table's history, or its whole original export. */
        public static final String TIER_FULL = "full";
        /** Only an import that prunes the table loads it: the current rows, rewritten. */
        public static final String TIER_PRUNED = "pruned";

        public FileEntry(String path, long size, String sha256, long rowCount, String partition) {
            this(path, size, sha256, rowCount, partition, null);
        }

        public boolean fullOnly() {
            return TIER_FULL.equals(tier);
        }

        public boolean prunedOnly() {
            return TIER_PRUNED.equals(tier);
        }
    }

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
