package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.ImportMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.ArrayList;

/** Bounds the non-spillable UTxO list aggregate without changing persisted batch boundaries. */
final class UtxoBatchLoader {
    private static final Logger log = LoggerFactory.getLogger(UtxoBatchLoader.class);
    private static final int MAX_BUCKETS = 65536;
    private static final Pattern SIZE = Pattern.compile("([0-9.]+)\\s*([KMGTPE]?)(I?)B", Pattern.CASE_INSENSITIVE);

    private UtxoBatchLoader() {}

    static boolean supports(SnapshotTableSpec spec) {
        return spec.id().equals("address-utxo") && spec.importSpec().mode() == ImportMode.SQL
                && spec.importSpec().transformVersion() == 5
                && "classpath:/snapshot/sql/address_utxo_v3.sql".equals(spec.importSpec().selectResource());
    }

    static long load(TableLoader loader, DuckPgSession session, ImportBatch batch, ColumnPlan plan,
                     SnapshotManifest manifest, Map<String, List<SnapshotManifest.FileEntry>> dependencies,
                     String schema) throws SQLException {
        long memory;
        try (var st = session.connection().createStatement();
             var rs = st.executeQuery("SELECT current_setting('memory_limit')")) {
            rs.next();
            memory = memoryBytes(rs.getString(1));
        }
        int buckets = initialBuckets(batch.rowsIn(), batch.bytes(), memory);
        while (true) {
            int count = buckets;
            // A key's asset rows always hash together, even across files or partitions. All buckets
            // cover the exact original input once. Each statement releases its aggregate state.
            List<String> selects = new ArrayList<>(count);
            for (int bucket = 0; bucket < count; bucket++) {
                selects.add(loader.sourceSelect(session.connection(), batch.spec(), batch, manifest.point().slot(),
                        manifest.point().epoch(), dependencies, count == 1 ? null
                                : "hash(tx_hash, output_index) % " + count + " = " + bucket));
            }
            if (count > 1) {
                log.info("Loading UTxO batch {} in {} memory-bounded slices", batch.batchId().substring(0, 12), count);
            }
            try {
                return loader.loadBatch(session, batch, plan, selects, manifest.snapshotId(), schema);
            } catch (SQLException failure) {
                // loadBatch has rolled back every slice, including slices already inserted into PG.
                // Never retry other errors, an uncertain rollback, or an already minimal workload.
                if (!isOutOfMemory(failure) || failure.getSuppressed().length != 0) throw failure;
                if (buckets >= MAX_BUCKETS) {
                    throw new SQLException("UTxO load still exceeds memory after " + buckets
                            + " slices. Increase --memory-limit; scanning and a single output must fit in memory.", failure);
                }
                buckets = Math.min(MAX_BUCKETS, buckets * 2);
                log.warn("UTxO batch {} exceeded memory; rolled back and retrying with {} slices",
                        batch.batchId().substring(0, 12), buckets);
            }
        }
    }

    /**
     * Initial estimate; OOM feedback handles skew and unusually wide values. Calibrated on mainnet
     * with DuckDB 1.5.5: the densest day (2.96M asset rows, 96 MB of Parquet) needs 1.5-2GB in one
     * pass, about 700 bytes per asset row or 20x the compressed size. A fifth of the limit is left
     * for the Parquet scan, the block join and the PostgreSQL writer.
     */
    static int initialBuckets(long rows, long compressedBytes, long memoryBytes) {
        double estimate = Math.max(rows * 700.0, compressedBytes * 20.0);
        double budget = Math.max(1, memoryBytes * 0.8);
        int buckets = 1;
        while (buckets < MAX_BUCKETS && estimate / buckets > budget) buckets *= 2;
        return buckets;
    }

    static boolean isOutOfMemory(SQLException failure) {
        return failure.getMessage() != null
                && failure.getMessage().toLowerCase(Locale.ROOT).contains("out of memory");
    }

    static long memoryBytes(String value) {
        var match = SIZE.matcher(value.trim());
        if (!match.matches()) throw new IllegalArgumentException("Unrecognized DuckDB memory limit: " + value);
        int power = " KMGTPE".indexOf(match.group(2).toUpperCase(Locale.ROOT));
        if (match.group(2).isEmpty()) power = 0;
        return (long) Math.min(Long.MAX_VALUE, Double.parseDouble(match.group(1))
                * Math.pow(match.group(3).isEmpty() ? 1000 : 1024, power));
    }
}
