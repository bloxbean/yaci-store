package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.spec.CompletedEpochType;
import com.bloxbean.cardano.yaci.store.snapshot.spec.CoverageType;
import com.bloxbean.cardano.yaci.store.snapshot.spec.PartitionStrategy;
import com.bloxbean.cardano.yaci.store.snapshot.spec.RestoreMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Chooses the single Cardano point a snapshot restores to.
 *
 * <p>It is deliberately <em>not</em> the maximum block in DuckLake. Daily and epoch exports advance
 * independently, so the newest block can be ahead of the derived state that must accompany it. The
 * point is the last block of the newest epoch that every gating table fully supports, after each
 * table's own declared rule is applied.
 */
public class ConsistencyPointSelector {

    private static final Logger log = LoggerFactory.getLogger(ConsistencyPointSelector.class);
    static final String BLOCK_SPEC_ID = "block";

    /**
     * @param completedEpoch the selected epoch
     * @param gating         per-spec supported completed epoch, for reporting why the epoch was chosen
     * @param limitedBy      the spec ids that pinned the result
     */
    public record Selection(int completedEpoch,
                            ConsistencyPoint point,
                            Map<String, Integer> gating,
                            List<String> limitedBy,
                            long maxExportedBlock,
                            List<String> blockers) {

        public boolean ok() {
            return blockers.isEmpty();
        }
    }

    private final DuckLakeCatalog catalog;
    private final Map<String, Map<String, Long>> completedPartitions;

    public ConsistencyPointSelector(DuckLakeCatalog catalog) {
        this(catalog, Map.of());
    }

    /**
     * @param completedPartitions the source export journal (exporter id → partition value → completed
     *                            row count). It lets a table that is exported but holds no rows yet, such
     *                            as the Conway governance tables of a pre-Conway chain, still gate and
     *                            cover the point.
     */
    public ConsistencyPointSelector(DuckLakeCatalog catalog, Map<String, Map<String, Long>> completedPartitions) {
        this.catalog = catalog;
        this.completedPartitions = completedPartitions == null ? Map.of() : completedPartitions;
    }

    /**
     * @param specs           all specifications considered for this snapshot
     * @param snapshotId      the pinned DuckLake catalog snapshot
     * @param network         network name recorded in the manifest
     * @param protocolMagic   protocol magic recorded in the manifest
     * @param minConfirmations required distance in blocks between the point and the newest exported
     *                         block, so the snapshot sits behind the finality buffer
     */
    public Selection select(List<SnapshotTableSpec> specs, long snapshotId, String network,
                            long protocolMagic, long minConfirmations) throws SQLException {
        return select(specs, snapshotId, network, protocolMagic, minConfirmations, 0);
    }

    /**
     * @param targetEpoch restore to this completed epoch rather than the newest one every gating
     *                    table supports. Useful for qualification runs that need to replay several
     *                    epoch boundaries. It may never exceed what the data actually supports.
     */
    public Selection select(List<SnapshotTableSpec> specs, long snapshotId, String network,
                            long protocolMagic, long minConfirmations, int targetEpoch)
            throws SQLException {
        List<String> blockers = new ArrayList<>();
        Map<String, Integer> gating = new LinkedHashMap<>();

        for (SnapshotTableSpec spec : specs) {
            if (spec.restore() != RestoreMode.IMPORT) {
                continue;
            }
            SnapshotTableSpec.CompletedEpochRule rule = spec.consistency().completedEpoch();
            if (rule.type() == CompletedEpochType.NONE) {
                continue;
            }
            Optional<long[]> bounds = bounds(spec, rule.column(), snapshotId);
            if (bounds.isEmpty()) {
                blockers.add("Table '" + spec.id() + "' gates the consistency point on '" + rule.column()
                        + "' but the export has no statistics for it and no completed empty epoch partitions"
                        + " in the export journal (is the export empty?)");
                continue;
            }
            long max = bounds.get()[1];
            int supported = (int) (rule.type() == CompletedEpochType.MAX_EPOCH ? max : max + rule.offset());
            gating.put(spec.id(), supported);
        }

        if (gating.isEmpty()) {
            blockers.add("No table gates the consistency point; at least one epoch-complete table is required");
            return new Selection(-1, null, gating, List.of(), -1, blockers);
        }

        int supportedEpoch = gating.values().stream().mapToInt(Integer::intValue).min().orElse(-1);
        int completedEpoch = supportedEpoch;
        if (targetEpoch > 0) {
            if (targetEpoch > supportedEpoch) {
                blockers.add("Requested target epoch " + targetEpoch + " is beyond the newest epoch "
                        + "the export supports (" + supportedEpoch + ")");
            }
            completedEpoch = targetEpoch;
        }
        final int selectedEpoch = completedEpoch;
        List<String> limitedBy = targetEpoch > 0
                ? List.of("explicit --target-epoch " + targetEpoch + " (newest supported: " + supportedEpoch + ")")
                : gating.entrySet().stream()
                        .filter(e -> e.getValue() == selectedEpoch)
                        .map(Map.Entry::getKey)
                        .sorted()
                        .toList();

        SnapshotTableSpec blockSpec = specs.stream()
                .filter(s -> s.id().equals(BLOCK_SPEC_ID))
                .findFirst()
                .orElse(null);
        if (blockSpec == null || blockSpec.relation() == null) {
            blockers.add("The 'block' specification is required to resolve the consistency point");
            return new Selection(completedEpoch, null, gating, limitedBy, -1, blockers);
        }

        long maxExportedBlock = catalog.numericBounds(blockSpec.relation(), "number", snapshotId)
                .map(b -> b[1]).orElse(-1L);

        ConsistencyPoint point = lastBlockOfEpoch(blockSpec.relation(), completedEpoch, snapshotId,
                network, protocolMagic);
        if (point == null) {
            blockers.add("No block found for epoch " + completedEpoch + " in the export");
            return new Selection(completedEpoch, null, gating, limitedBy, maxExportedBlock, blockers);
        }

        if (maxExportedBlock >= 0 && maxExportedBlock - point.blockNumber() < minConfirmations) {
            blockers.add("Point block " + point.blockNumber() + " is only "
                    + (maxExportedBlock - point.blockNumber()) + " blocks behind the newest exported block "
                    + maxExportedBlock + "; at least " + minConfirmations + " are required");
        }

        // Every table must actually reach the chosen point.
        for (SnapshotTableSpec spec : specs) {
            if (spec.restore() != RestoreMode.IMPORT) {
                continue;
            }
            SnapshotTableSpec.CoverageRule cov = spec.consistency().coverage();
            if (cov.type() == CoverageType.NONE) {
                continue;
            }
            Optional<long[]> b = bounds(spec, cov.column(), snapshotId);
            if (b.isEmpty()) {
                blockers.add("Table '" + spec.id() + "' declares coverage on '" + cov.column()
                        + "' but the export has no statistics for it and no completed empty epoch partitions"
                        + " in the export journal");
                continue;
            }
            long max = b.get()[1];
            long required = cov.type() == CoverageType.SLOT_GTE_CUT ? point.slot() : completedEpoch;
            if (max < required) {
                blockers.add("Table '" + spec.id() + "' only reaches " + cov.column() + "=" + max
                        + " but the consistency point requires >= " + required);
            }
        }

        log.info("Selected completed epoch {} (limited by {}), point block {} slot {}",
                completedEpoch, limitedBy, point.blockNumber(), point.slot());
        return new Selection(completedEpoch, point, gating, limitedBy, maxExportedBlock, blockers);
    }

    /**
     * The catalog's bounds of {@code column}, or for a table without rows the epochs its export journal
     * records as completed and empty.
     */
    private Optional<long[]> bounds(SnapshotTableSpec spec, String column, long snapshotId) throws SQLException {
        Optional<long[]> bounds = catalog.numericBounds(spec.relation(), column, snapshotId);
        if (bounds.isPresent() || spec.source() == null) {
            return bounds;
        }
        return emptyExportBounds(spec, column,
                completedPartitions.getOrDefault(spec.source().exporterId(), Map.of()));
    }

    /**
     * Epoch bounds proven by the export journal alone, for a table the catalog has no rows for.
     *
     * <p>Only an epoch-partitioned table whose gating column is its partition column qualifies, and
     * only when every completed partition recorded zero rows: a journal that claims rows the catalog
     * lacks is a broken export, not an empty one, and stays a blocker. Per-partition completeness up
     * to the point is still checked afterwards by {@link ExportCoverage}.
     */
    static Optional<long[]> emptyExportBounds(SnapshotTableSpec spec, String column, Map<String, Long> journal) {
        SnapshotTableSpec.Partition partition = spec.source().partition();
        if (partition == null || partition.strategy() != PartitionStrategy.EPOCH
                || !column.equals(partition.column()) || journal.isEmpty()) {
            return Optional.empty();
        }
        String prefix = column + "=";
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (Map.Entry<String, Long> e : journal.entrySet()) {
            if (e.getValue() == null || e.getValue() != 0 || !e.getKey().startsWith(prefix)) {
                return Optional.empty();
            }
            long epoch;
            try {
                epoch = Long.parseLong(e.getKey().substring(prefix.length()));
            } catch (NumberFormatException ex) {
                return Optional.empty();
            }
            min = Math.min(min, epoch);
            max = Math.max(max, epoch);
        }
        return Optional.of(new long[]{min, max});
    }

    /** Read the last block of {@code epoch} straight from the block Parquet files that can hold it. */
    ConsistencyPoint lastBlockOfEpoch(String blockRelation, int epoch, long snapshotId,
                                      String network, long protocolMagic) throws SQLException {
        List<DuckLakeFile> candidates =
                catalog.filesOverlapping(blockRelation, "epoch", epoch, epoch, snapshotId);
        if (candidates.isEmpty()) {
            return null;
        }
        String sql = "SELECT hash, number, slot, prev_hash, era, CAST(epoch(block_time) AS BIGINT) AS bt, epoch"
                + " FROM " + DuckDb.readParquet(catalog.parquetList(candidates))
                + " WHERE epoch = " + epoch
                + " ORDER BY number DESC LIMIT 1";
        try (Statement st = catalog.connection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (!rs.next()) {
                return null;
            }
            return new ConsistencyPoint(network, protocolMagic, rs.getInt("epoch"), rs.getLong("slot"),
                    rs.getLong("number"), rs.getString("hash"), rs.getString("prev_hash"),
                    rs.getInt("era"), rs.getLong("bt"), snapshotId);
        }
    }
}
