package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.Retention;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import com.bloxbean.cardano.yaci.store.snapshot.util.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Turns a full export's table plans into a pruned snapshot's (ADR 0005, D8): what a node running
 * {@code UtxoPruningService} and {@code EpochStakePruningService} would hold at the point.
 *
 * <ul>
 *   <li>{@code tx_input}: inputs spent within the last {@code utxoSafeBlocks} blocks before the point;</li>
 *   <li>{@code address_utxo}: outputs not spent before that window, so unspent at the point or spent
 *       within the window, which a rollback inside the cursor tail un-spends;</li>
 *   <li>{@code epoch_stake}: the newest {@code epochStakeEpochs} epoch partitions, unchanged.</li>
 * </ul>
 *
 * <p>The two UTxO relations are rewritten as Parquet in the same daily partitions and columns, so the
 * importer's transforms, batching and validation are unchanged. The anti-join of every output against
 * every spent input is one DuckDB query under the export's memory limit, with a spill directory, so a
 * build side larger than memory spills instead of failing.
 */
final class PrunedExport {

    private static final Logger log = LoggerFactory.getLogger(PrunedExport.class);

    static final String ADDRESS_UTXO = "address_utxo";
    static final String TX_INPUT = "tx_input";
    static final String EPOCH_STAKE = "epoch_stake";

    private static final Pattern EPOCH_DIRECTORY = Pattern.compile("(?:^|/)epoch=(-?\\d+)(?:/|$)");

    /** The rewritten plans, and where each rewritten file is, by its path in the snapshot. */
    record Result(List<TablePlan> plans, Map<String, Path> rewritten, Map<String, String> retention) {}

    private final DuckLakeCatalog catalog;
    private final Path outDir;
    private final String memoryLimit;
    private final Consumer<String> progress;
    /** Files already written and counted, so writing a relation in chunks counts each file once. */
    private final Map<String, DuckLakeFile> counted = new java.util.HashMap<>();

    PrunedExport(DuckLakeCatalog catalog, Path workDir, String memoryLimit, Consumer<String> progress) {
        this.catalog = catalog;
        this.outDir = workDir.resolve("pruned");
        this.memoryLimit = memoryLimit;
        this.progress = progress;
    }

    Result apply(List<TablePlan> plans, ConsistencyPoint point, long snapshotId, int utxoSafeBlocks,
                 int epochStakeEpochs) throws SQLException, IOException {
        if (epochStakeEpochs < Retention.MIN_EPOCH_STAKE_EPOCHS) {
            throw new IllegalArgumentException("A pruned snapshot must keep at least " + Retention.MIN_EPOCH_STAKE_EPOCHS
                    + " epochs of epoch_stake");
        }
        if (utxoSafeBlocks < 0) {
            throw new IllegalArgumentException("utxo-safe-blocks must not be negative");
        }
        deleteRecursively(outDir);
        Files.createDirectories(outDir);
        Map<String, TablePlan> byRelation = new LinkedHashMap<>();
        plans.forEach(p -> byRelation.put(p.spec().relation(), p));
        Map<String, Path> rewritten = new TreeMap<>();
        Map<String, String> retention = new TreeMap<>();
        List<TablePlan> out = new ArrayList<>();

        long windowSlot = windowStartSlot(point, utxoSafeBlocks, snapshotId);
        say(String.format("Keeping UTxOs unspent at block %d, or spent from block %d (slot %d) on; epoch_stake from"
                + " the newest %d epochs", point.blockNumber(), point.blockNumber() - utxoSafeBlocks, windowSlot, epochStakeEpochs));

        Path spill = outDir.resolve(".spill");
        Files.createDirectories(spill);
        try (Connection duck = DuckDb.open(memoryLimit, spill, DuckDb.threadsFor(memoryLimit))) {
            DuckDb.exec(duck, "SET preserve_insertion_order=false");
            for (TablePlan plan : plans) {
                String relation = plan.spec().relation();
                switch (relation) {
                    case TX_INPUT -> {
                        out.add(pruneTxInput(duck, plan, point, windowSlot, snapshotId, rewritten));
                        retention.put(Retention.TX_INPUT_SAFE_BLOCKS, Integer.toString(utxoSafeBlocks));
                    }
                    case ADDRESS_UTXO -> {
                        TablePlan spent = byRelation.get(TX_INPUT);
                        if (spent == null) {
                            throw new IllegalStateException("A pruned snapshot needs tx_input to find spent outputs");
                        }
                        out.add(pruneAddressUtxo(duck, plan, spent, point, windowSlot, rewritten));
                        retention.put(Retention.UTXO_SPENT_SAFE_BLOCKS, Integer.toString(utxoSafeBlocks));
                    }
                    case EPOCH_STAKE -> {
                        out.add(newestEpochs(plan, epochStakeEpochs));
                        retention.put(Retention.EPOCH_STAKE_EPOCHS, Integer.toString(epochStakeEpochs));
                    }
                    default -> out.add(plan);
                }
            }
        } finally {
            deleteRecursively(spill);
        }
        return new Result(out, rewritten, retention);
    }

    /** Slot of the first block inside the window, the block {@code utxoSafeBlocks} before the point. */
    private long windowStartSlot(ConsistencyPoint point, int safeBlocks, long snapshotId) throws SQLException {
        long first = point.blockNumber() - safeBlocks;
        if (first <= 0) {
            return Long.MIN_VALUE;
        }
        List<DuckLakeFile> files = catalog.filesOverlapping("block", "number", first, first, snapshotId);
        if (files.isEmpty()) {
            throw new IllegalStateException("Block " + first + " is not in the block export");
        }
        long slot = DuckDb.queryLong(catalog.connection(), "SELECT slot FROM "
                + DuckDb.readParquet(catalog.parquetList(files)) + " WHERE number = " + first, Long.MIN_VALUE);
        if (slot == Long.MIN_VALUE) {
            throw new IllegalStateException("Block " + first + " is not in the block export");
        }
        return slot;
    }

    private TablePlan pruneTxInput(Connection duck, TablePlan plan, ConsistencyPoint point, long windowSlot,
                                   long snapshotId, Map<String, Path> rewritten) throws SQLException, IOException {
        long started = System.currentTimeMillis();
        // Files entirely before the window are dropped unread.
        Set<String> before = windowSlot == Long.MIN_VALUE ? Set.of()
                : catalog.fileRowCountsEntirelyBelow(TX_INPUT, "spent_at_slot", windowSlot - 1, snapshotId).keySet();
        List<DuckLakeFile> candidates = plan.files().stream().filter(f -> !before.contains(f.relativePath())).toList();
        String where = "spent_at_slot <= " + point.slot()
                + (windowSlot == Long.MIN_VALUE ? "" : " AND spent_at_slot >= " + windowSlot);
        List<DuckLakeFile> files = candidates.isEmpty() ? List.of()
                : write(duck, plan, "SELECT * FROM " + DuckDb.readParquet(catalog.parquetList(candidates)) + " WHERE " + where,
                rewritten);
        long rows = files.stream().mapToLong(DuckLakeFile::rowCount).sum();
        say(String.format("tx_input: %,d of %,d rows kept, from %d of %d files, in %s", rows, plan.rowCount(),
                candidates.size(), plan.files().size(), Progress.duration(System.currentTimeMillis() - started)));
        return replan(duck, plan, files, rows);
    }

    /** Spent keys and output keys are split into this many buckets, so each join holds 1/BUCKETS of them. */
    static final int BUCKETS = 64;
    /** Date partitions written per COPY, so only this many output files are open at once. */
    static final int PARTITIONS_PER_WRITE = 32;

    /**
     * Outputs not spent before the window. Memory is bounded by a bucket, not by the chain:
     *
     * <ol>
     *   <li>the keys of inputs spent before the window, and the keys of every output at the point, are
     *       written to the spill directory in {@link #BUCKETS} buckets by {@code hash(tx_hash)};</li>
     *   <li>each bucket's outputs are anti-joined with its spent keys, giving the kept outputs;</li>
     *   <li>the kept outputs' rows are written {@link #PARTITIONS_PER_WRITE} date partitions at a time.</li>
     * </ol>
     */
    private TablePlan pruneAddressUtxo(Connection duck, TablePlan plan, TablePlan spent, ConsistencyPoint point,
                                       long windowSlot, Map<String, Path> rewritten) throws SQLException, IOException {
        long started = System.currentTimeMillis();
        Path work = outDir.resolve(".utxo");
        deleteRecursively(work);
        Files.createDirectories(work);
        String outputs = DuckDb.readParquet(catalog.parquetList(plan.files()));
        String bucket = "CAST(hash(tx_hash) % " + BUCKETS + " AS INTEGER) AS b";
        try {
            if (windowSlot != Long.MIN_VALUE) {
                copy(duck, "SELECT tx_hash, output_index, " + bucket + " FROM "
                        + DuckDb.readParquet(catalog.parquetList(spent.files())) + " WHERE spent_at_slot < " + windowSlot,
                        work.resolve("spent"), "b");
            }
            // One row per output when asset_index 1 is every output's first asset; otherwise every row, made
            // distinct per bucket below.
            long firstAssets = !plan.sourceColumns().containsKey("asset_index") ? -1
                    : DuckDb.queryLong(duck, "SELECT count(*) FROM " + outputs
                    + " WHERE asset_index = 1 AND slot <= " + point.slot(), -1);
            boolean onePerOutput = firstAssets == plan.rowCount();
            copy(duck, "SELECT tx_hash, output_index, \"date\", " + bucket + " FROM " + outputs
                    + " WHERE slot <= " + point.slot() + (onePerOutput ? " AND asset_index = 1" : ""),
                    work.resolve("outputs"), "b");
            say(String.format("address_utxo: keys bucketed in %s", Progress.duration(System.currentTimeMillis() - started)));

            Path kept = work.resolve("kept");
            Files.createDirectories(kept);
            for (int b = 0; b < BUCKETS; b++) {
                Path o = work.resolve("outputs/b=" + b);
                if (!Files.isDirectory(o)) {
                    continue;
                }
                String bucketOutputs = "(SELECT " + (onePerOutput ? "" : "DISTINCT ") + "tx_hash, output_index, \"date\" FROM "
                        + DuckDb.readParquet(glob(o)) + ")";
                Path s = work.resolve("spent/b=" + b);
                String select = windowSlot == Long.MIN_VALUE || !Files.isDirectory(s) ? "SELECT * FROM " + bucketOutputs
                        : "SELECT o.* FROM " + bucketOutputs + " o ANTI JOIN " + DuckDb.readParquet(glob(s))
                        + " x ON o.tx_hash = x.tx_hash AND o.output_index = x.output_index";
                DuckDb.exec(duck, "COPY (" + select + ") TO "
                        + Identifiers.literal(kept.resolve("kept_" + b + ".parquet").toString()) + " (FORMAT parquet)");
            }
            long outputsKept = DuckDb.queryLong(duck, "SELECT count(*) FROM " + DuckDb.readParquet(glob(kept)), 0);
            say(String.format("address_utxo: %,d of %,d outputs kept, %s so far", outputsKept, plan.rowCount(),
                    Progress.duration(System.currentTimeMillis() - started)));

            // Write the kept outputs' rows, a few date partitions at a time.
            Map<String, List<DuckLakeFile>> byPartition = plan.files().stream()
                    .collect(Collectors.groupingBy(DuckLakeFile::partition, TreeMap::new, Collectors.toList()));
            List<List<DuckLakeFile>> chunks = new ArrayList<>();
            List<DuckLakeFile> chunk = new ArrayList<>();
            int partitions = 0;
            for (List<DuckLakeFile> files : byPartition.values()) {
                chunk.addAll(files);
                if (++partitions % PARTITIONS_PER_WRITE == 0) {
                    chunks.add(chunk);
                    chunk = new ArrayList<>();
                }
            }
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            List<DuckLakeFile> files = new ArrayList<>();
            for (List<DuckLakeFile> c : chunks) {
                String rows = DuckDb.readParquet(catalog.parquetList(c));
                files = write(duck, plan, "SELECT u.* FROM " + rows + " u SEMI JOIN (SELECT tx_hash, output_index FROM "
                        + DuckDb.readParquet(glob(kept)) + " WHERE \"date\" IN (SELECT DISTINCT \"date\" FROM " + rows + ")) k"
                        + " ON u.tx_hash = k.tx_hash AND u.output_index = k.output_index WHERE u.slot <= " + point.slot(),
                        rewritten);
            }
            long rowsKept = files.stream().mapToLong(DuckLakeFile::rowCount).sum();
            say(String.format("address_utxo: %,d rows in %d files written in %s", rowsKept, files.size(),
                    Progress.duration(System.currentTimeMillis() - started)));
            return replan(duck, plan, files, outputsKept);
        } finally {
            deleteRecursively(work);
        }
    }

    /** {@code select} into {@code dir}, one directory per value of {@code partitionColumn}. */
    private static void copy(Connection duck, String select, Path dir, String partitionColumn) throws SQLException {
        DuckDb.exec(duck, "COPY (" + select + ") TO " + Identifiers.literal(dir.toString())
                + " (FORMAT parquet, PARTITION_BY (" + partitionColumn + "), OVERWRITE_OR_IGNORE true)");
    }

    private static String glob(Path dir) {
        return Identifiers.literal(dir.resolve("**/*.parquet").toString());
    }

    /**
     * Runs {@code select} into Parquet under the relation's own directory layout: one directory per
     * {@code date}, as the source export has, so batching and dependency narrowing are unchanged.
     */
    private List<DuckLakeFile> write(Connection duck, TablePlan plan, String select, Map<String, Path> rewritten)
            throws SQLException, IOException {
        String prefix = relationPrefix(plan);
        Path dir = outDir.resolve(prefix);
        Files.createDirectories(dir);
        DuckDb.exec(duck, "COPY (" + select + ") TO " + Identifiers.literal(dir.toAbsolutePath().toString())
                + " (FORMAT parquet, COMPRESSION zstd, PARTITION_BY (\"date\"), WRITE_PARTITION_COLUMNS true,"
                + " FILENAME_PATTERN 'pruned_{i}', OVERWRITE_OR_IGNORE true)");
        List<DuckLakeFile> files = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.filter(f -> f.toString().endsWith(".parquet")).sorted().toList()) {
                String rel = prefix + "/" + dir.relativize(p).toString().replace('\\', '/');
                DuckLakeFile known = counted.get(rel);
                if (known == null) {
                    long rows = DuckDb.queryLong(duck, "SELECT count(*) FROM "
                            + DuckDb.readParquet("[" + Identifiers.literal(p.toAbsolutePath().toString()) + "]"), 0);
                    known = new DuckLakeFile(rel, rows, Files.size(p));
                    counted.put(rel, known);
                }
                files.add(known);
                rewritten.put(rel, p.toAbsolutePath());
            }
        }
        files.sort(Comparator.comparing(DuckLakeFile::relativePath));
        return files;
    }

    /** The plan with the rewritten files, their row count and their observed bounds. */
    private TablePlan replan(Connection duck, TablePlan plan, List<DuckLakeFile> files, long rows) throws SQLException {
        Map<String, String> bounds = new TreeMap<>();
        if (!files.isEmpty()) {
            Map<String, Path> paths = new LinkedHashMap<>();
            files.forEach(f -> paths.put(f.relativePath(), outDir.resolve(f.relativePath())));
            String scan = DuckDb.readParquet(list(files, paths));
            for (String column : plan.spec().validation().bounds()) {
                String q = Identifiers.quote(column);
                try (Statement st = duck.createStatement();
                     ResultSet rs = st.executeQuery("SELECT CAST(min(" + q + ") AS VARCHAR), CAST(max(" + q + ") AS VARCHAR) FROM " + scan)) {
                    if (rs.next() && rs.getString(1) != null) {
                        bounds.put(column + ".min", rs.getString(1));
                        bounds.put(column + ".max", rs.getString(2));
                    }
                }
            }
        }
        return new TablePlan(plan.spec(), files, rows, true, plan.sourceColumns(), bounds, plan.columnFingerprint(),
                plan.problems());
    }

    /** The newest {@code epochs} epoch partitions, as exported. */
    private TablePlan newestEpochs(TablePlan plan, int epochs) {
        long fileRows = plan.files().stream().mapToLong(DuckLakeFile::rowCount).sum();
        if (fileRows != plan.rowCount()) {
            throw new IllegalStateException("epoch_stake's " + plan.rowCount() + " rows are not the " + fileRows
                    + " rows of its files; it cannot be reduced to its newest epochs");
        }
        Map<DuckLakeFile, Integer> epochOf = new LinkedHashMap<>();
        for (DuckLakeFile f : plan.files()) {
            Matcher m = EPOCH_DIRECTORY.matcher(f.relativePath());
            if (!m.find()) {
                throw new IllegalStateException(f.relativePath() + " is not in an epoch partition");
            }
            epochOf.put(f, Integer.parseInt(m.group(1)));
        }
        int newest = epochOf.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int oldestKept = newest - epochs + 1;
        List<DuckLakeFile> kept = plan.files().stream().filter(f -> epochOf.get(f) >= oldestKept).toList();
        long rows = kept.stream().mapToLong(DuckLakeFile::rowCount).sum();
        Map<String, String> bounds = new TreeMap<>(plan.bounds());
        bounds.put("epoch.min", Integer.toString(kept.stream().mapToInt(epochOf::get).min().orElse(oldestKept)));
        bounds.put("epoch.max", Integer.toString(newest));
        say(String.format("epoch_stake: epochs %d to %d, %,d of %,d rows", oldestKept, newest, rows, plan.rowCount()));
        return new TablePlan(plan.spec(), kept, rows, true, plan.sourceColumns(), bounds, plan.columnFingerprint(),
                plan.problems());
    }

    /** {@code main/address_utxo} for {@code main/address_utxo/date=2026-01-01/x.parquet}. */
    private static String relationPrefix(TablePlan plan) {
        String relation = plan.spec().relation();
        for (DuckLakeFile f : plan.files()) {
            int at = f.relativePath().indexOf(relation + "/");
            if (at >= 0) {
                return f.relativePath().substring(0, at + relation.length());
            }
        }
        return "main/" + relation;
    }

    private static String list(List<DuckLakeFile> files, Map<String, Path> paths) {
        return files.stream().map(f -> Identifiers.literal(paths.get(f.relativePath()).toString()))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    private void say(String message) {
        log.info(message);
        if (progress != null) {
            progress.accept("[prune] " + message);
        }
    }

    static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
