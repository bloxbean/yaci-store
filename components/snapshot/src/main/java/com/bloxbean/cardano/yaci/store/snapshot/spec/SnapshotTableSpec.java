package com.bloxbean.cardano.yaci.store.snapshot.spec;

import java.util.List;
import java.util.Map;

/**
 * Versioned, declarative description of how one table participates in a snapshot.
 *
 * <p>A specification is additive: it describes an <em>already produced</em> analytics relation and
 * the PostgreSQL target it restores into. It never registers, schedules or executes an analytics
 * exporter, and existing exporter configuration (including
 * {@code application-custom-exporters.yml}) is untouched by its presence.
 *
 * <p>Instances are immutable and are loaded from YAML by {@link SnapshotSpecLoader}.
 */
public record SnapshotTableSpec(
        String id,
        int specVersion,
        String module,
        TableKind kind,
        RestoreMode restore,
        /** Mandatory human-readable justification for every non-{@link RestoreMode#IMPORT} table. */
        String reason,
        Source source,
        Consistency consistency,
        Import importSpec,
        Validation validation,
        /** Target column to the reason its value cannot be recovered from the current export. */
        Map<String, String> lossy,
        /** SHA-256 of the canonical YAML bytes this spec was loaded from. */
        String digest,
        /** Where the spec came from, for diagnostics only. Never written to a manifest. */
        String origin
) {

    /** Stable registry key. */
    public String key() {
        return id + "@" + specVersion;
    }

    public boolean hasSource() {
        return source != null;
    }

    public String targetTable() {
        return importSpec != null ? importSpec.targetTable() : null;
    }

    /** The analytics relation packaged for this spec, or {@code null} when the table has no export. */
    public String relation() {
        return source != null ? source.ducklakeRelation() : null;
    }

    /**
     * Link to the already-registered analytics exporter and the DuckLake relation it produces.
     * Purely descriptive: used for coverage validation and packaging, never for registration.
     */
    public record Source(
            String exporterId,
            String ducklakeRelation,
            Partition partition
    ) {}

    public record Partition(PartitionStrategy strategy, String column) {}

    /**
     * How the table constrains, and is constrained by, the snapshot consistency point.
     *
     * @param completedEpoch how far this table gates the newest selectable completed epoch
     * @param cutoff         the predicate that removes rows beyond the selected point
     * @param coverage       a post-selection assertion that the table actually reaches the point
     */
    public record Consistency(
            CompletedEpochRule completedEpoch,
            CutoffRule cutoff,
            CoverageRule coverage
    ) {}

    /**
     * @param offset signed adjustment applied to the observed maximum of {@code column}.
     *               For example {@code block} exports the partial current epoch, so it supports
     *               {@code max(epoch) - 1}; {@code reward} is keyed by earned epoch and supports
     *               completed epoch {@code max(epoch) + 2}.
     */
    public record CompletedEpochRule(CompletedEpochType type, String column, int offset) {
        public static CompletedEpochRule none() {
            return new CompletedEpochRule(CompletedEpochType.NONE, null, 0);
        }
    }

    /**
     * @param offset for {@link CutoffType#EPOCH_LTE_OFFSET}: rows are kept while
     *               {@code column <= completedEpoch - offset}.
     */
    public record CutoffRule(CutoffType type, String column, int offset) {
        public static CutoffRule none() {
            return new CutoffRule(CutoffType.NONE, null, 0);
        }
    }

    public record CoverageRule(CoverageType type, String column) {
        public static CoverageRule none() {
            return new CoverageRule(CoverageType.NONE, null);
        }
    }

    /**
     * The PostgreSQL restore contract.
     *
     * @param columns              explicit target-column mappings; every other target column must be
     *                             matched by identical source name, listed in {@code useTargetDefaults},
     *                             or the spec is rejected
     * @param ignoreSourceColumns  export-only columns (for example the DuckLake partition column)
     * @param useTargetDefaults    target columns intentionally left to their PostgreSQL default
     * @param selectResource       {@link ImportMode#SQL} only: classpath resource holding one read-only SELECT
     * @param dependencies         other spec ids whose files this transform reads
     * @param targetPartitioning   set when the PostgreSQL target is range-partitioned by epoch
     */
    public record Import(
            String targetTable,
            ImportMode mode,
            Map<String, Column> columns,
            List<String> ignoreSourceColumns,
            List<String> useTargetDefaults,
            String selectResource,
            int transformVersion,
            List<String> dependencies,
            BatchBoundary batchBoundary,
            int batchSize,
            TargetPartitioning targetPartitioning,
            String handler
    ) {}

    /**
     * One target column's value source. Exactly one of {@code source}, {@code constant} or
     * {@code useDefault} is set; {@code converter} may accompany {@code source}.
     */
    public record Column(String source, String converter, String constant, boolean useDefault) {}

    /**
     * PostgreSQL range partitioning that the importer must pre-create so rows do not land in the
     * default partition. Naming matches the runtime {@code PostgresPartitionManager}.
     */
    public record TargetPartitioning(String column, String partitionPrefix) {}

    /**
     * @param sourceKey source columns whose distinct-value count equals the number of rows the import
     *                  will produce. Needed only when a transform changes cardinality, as the
     *                  flattened {@code address_utxo} regroup does; otherwise the source row count is
     *                  the target row count.
     */
    public record Validation(List<String> key, List<String> bounds, List<String> requiredColumns,
                             List<String> sourceKey) {}
}
