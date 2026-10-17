package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckDb;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeFile;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.ConsistencyPoint;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.Retention;
import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
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
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Splits the tables a pruning node keeps only part of into a current and a history tier (ADR 0005,
 * D11). Both tiers are in the snapshot, in separate parts: a full import reads both, an import into a
 * target that prunes the table reads only the current tier, so it downloads what a pruned snapshot
 * would have held.
 *
 * <ul>
 *   <li>{@code tx_input}: current = inputs spent within the last {@code utxoSafeBlocks} blocks before
 *       the point; history = the older ones;</li>
 *   <li>{@code address_utxo}: current = outputs not spent before that window, so unspent at the point
 *       or spent within the window, which a rollback inside the cursor tail un-spends; history = the
 *       outputs spent before it. Every row of an output is in the same tier;</li>
 *   <li>{@code epoch_stake}: current = the newest {@code epochStakeEpochs} epoch partitions, history =
 *       the older ones, unchanged. The import picks its own number of epochs.</li>
 * </ul>
 *
 * <p>The UTxO relations are rewritten as Parquet in the same daily partitions and columns, so the
 * importer's transforms, batching and validation are unchanged; rows after the point are left out,
 * as the importer's cutoff would. Every source row up to the point lands in exactly one tier, which is
 * checked before anything is packaged. The anti-join of every output against every spent input works
 * one hash bucket of keys at a time under the export's memory limit.
 */
final class TieredExport {

    private static final Logger log = LoggerFactory.getLogger(TieredExport.class);

    static final String ADDRESS_UTXO = "address_utxo";
    static final String TX_INPUT = "tx_input";
    static final String EPOCH_STAKE = "epoch_stake";

    private static final Pattern EPOCH_DIRECTORY = Pattern.compile("(?:^|/)epoch=(-?\\d+)(?:/|$)");
    private static final String CURRENT = "current";
    private static final String HISTORY = "history";

    /**
     * One table's tiers.
     *
     * @param history paths of the files only a full import needs
     * @param split   the current tier's rows and bounds, for a table whose current tier an import selects
     *                by retention key; null when the import selects it otherwise ({@code epoch_stake})
     */
    record Tiers(Set<String> history, SnapshotManifest.HistorySplit split) {}

    /**
     * The plans with every file of the split tables (current first, then history), where each rewritten
     * file is by its path in the snapshot, and each split table's tiers by spec id.
     */
    record Result(List<TablePlan> plans, Map<String, Path> rewritten, Map<String, Tiers> tiers) {}

    private final DuckLakeCatalog catalog;
    private final Path outDir;
    private final String memoryLimit;
    private final Consumer<String> progress;
    /** Files already written and counted, so writing a relation in chunks counts each file once. */
    private final Map<String, DuckLakeFile> counted = new java.util.HashMap<>();

    TieredExport(DuckLakeCatalog catalog, Path workDir, String memoryLimit, Consumer<String> progress) {
        this.catalog = catalog;
        this.outDir = workDir.resolve("tiers");
        this.memoryLimit = memoryLimit;
        this.progress = progress;
    }

    Result apply(List<TablePlan> plans, ConsistencyPoint point, long snapshotId, int utxoSafeBlocks,
                 int epochStakeEpochs) throws SQLException, IOException {
        if (epochStakeEpochs < Retention.MIN_EPOCH_STAKE_EPOCHS) {
            throw new IllegalArgumentException("The current tier must keep at least " + Retention.MIN_EPOCH_STAKE_EPOCHS
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
        Map<String, Tiers> tiers = new TreeMap<>();
        List<TablePlan> out = new ArrayList<>();

        long windowSlot = windowStartSlot(point, utxoSafeBlocks, snapshotId);
        say(String.format("Current tier: UTxOs unspent at block %d or spent from block %d (slot %d) on; epoch_stake"
                + " from the newest %d epochs", point.blockNumber(), point.blockNumber() - utxoSafeBlocks, windowSlot,
                epochStakeEpochs));

        Path spill = outDir.resolve(".spill");
        Files.createDirectories(spill);
        try (Connection duck = DuckDb.open(memoryLimit, spill, DuckDb.threadsFor(memoryLimit))) {
            DuckDb.exec(duck, "SET preserve_insertion_order=false");
            for (TablePlan plan : plans) {
                String relation = plan.spec().relation();
                switch (relation) {
                    case TX_INPUT -> out.add(windowSlot == Long.MIN_VALUE ? plan
                            : splitTxInput(duck, plan, point, windowSlot, snapshotId, utxoSafeBlocks, rewritten, tiers));
                    case ADDRESS_UTXO -> {
                        TablePlan spent = byRelation.get(TX_INPUT);
                        if (spent == null || windowSlot == Long.MIN_VALUE) {
                            out.add(plan);
                        } else {
                            out.add(splitAddressUtxo(duck, plan, spent, point, windowSlot, utxoSafeBlocks, rewritten, tiers));
                        }
                    }
                    case EPOCH_STAKE -> out.add(splitEpochs(plan, epochStakeEpochs, tiers));
                    default -> out.add(plan);
                }
            }
        } finally {
            deleteRecursively(spill);
        }
        return new Result(out, rewritten, tiers);
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

    /**
     * Files entirely before the window go to history unread; the others are rewritten into both tiers
     * by {@code spent_at_slot}.
     */
    private TablePlan splitTxInput(Connection duck, TablePlan plan, ConsistencyPoint point, long windowSlot,
                                   long snapshotId, int safeBlocks, Map<String, Path> rewritten,
                                   Map<String, Tiers> tiers) throws SQLException, IOException {
        long started = System.currentTimeMillis();
        Set<String> before = catalog.fileRowCountsEntirelyBelow(TX_INPUT, "spent_at_slot", windowSlot - 1, snapshotId).keySet();
        List<DuckLakeFile> unread = plan.files().stream().filter(f -> before.contains(f.relativePath())).toList();
        List<DuckLakeFile> candidates = plan.files().stream().filter(f -> !before.contains(f.relativePath())).toList();
        Written w = candidates.isEmpty() ? new Written(List.of(), List.of())
                : write(duck, plan, "SELECT *, spent_at_slot < " + windowSlot + " AS __history FROM "
                + DuckDb.readParquet(catalog.parquetList(candidates)) + " WHERE spent_at_slot <= " + point.slot(), rewritten);

        List<DuckLakeFile> history = new ArrayList<>(unread);
        history.addAll(w.history());
        long currentRows = rows(w.current());
        long historyRows = rows(history);
        if (currentRows + historyRows != plan.rowCount()) {
            throw new IllegalStateException(String.format("tx_input tiers hold %,d + %,d rows, but the table has %,d"
                    + " up to the point", currentRows, historyRows, plan.rowCount()));
        }
        say(String.format("tx_input: %,d current and %,d history rows (%d files rewritten, %d unchanged) in %s",
                currentRows, historyRows, candidates.size(), unread.size(),
                Progress.duration(System.currentTimeMillis() - started)));
        return tiered(duck, plan, w.current(), history, currentRows, Retention.TX_INPUT_SAFE_BLOCKS, safeBlocks, tiers);
    }

    /** Spent keys and output keys are split into this many buckets, so each join holds 1/BUCKETS of them. */
    static final int BUCKETS = 64;
    /** Date partitions written per COPY, so only this many output files are open at once. */
    static final int PARTITIONS_PER_WRITE = 32;

    /**
     * Outputs not spent before the window are current, the others history. Memory is bounded by a
     * bucket, not by the chain:
     *
     * <ol>
     *   <li>the keys of inputs spent before the window, and the keys of every output at the point, are
     *       written to the spill directory in {@link #BUCKETS} buckets by {@code hash(tx_hash)};</li>
     *   <li>each bucket's outputs are anti-joined with its spent keys, giving the current outputs;</li>
     *   <li>every row up to the point is written, {@link #PARTITIONS_PER_WRITE} date partitions at a
     *       time, to the current tier when its output is current and to history otherwise.</li>
     * </ol>
     */
    private TablePlan splitAddressUtxo(Connection duck, TablePlan plan, TablePlan spent, ConsistencyPoint point,
                                       long windowSlot, int safeBlocks, Map<String, Path> rewritten,
                                       Map<String, Tiers> tiers) throws SQLException, IOException {
        long started = System.currentTimeMillis();
        Path work = outDir.resolve(".utxo");
        deleteRecursively(work);
        Files.createDirectories(work);
        String outputs = DuckDb.readParquet(catalog.parquetList(plan.files()));
        String bucket = "CAST(hash(tx_hash) % " + BUCKETS + " AS INTEGER) AS b";
        try {
            copy(duck, "SELECT tx_hash, output_index, " + bucket + " FROM "
                    + DuckDb.readParquet(catalog.parquetList(spent.files())) + " WHERE spent_at_slot < " + windowSlot,
                    work.resolve("spent"), "b");
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
                String select = !Files.isDirectory(s) ? "SELECT * FROM " + bucketOutputs
                        : "SELECT o.* FROM " + bucketOutputs + " o ANTI JOIN " + DuckDb.readParquet(glob(s))
                        + " x ON o.tx_hash = x.tx_hash AND o.output_index = x.output_index";
                DuckDb.exec(duck, "COPY (" + select + ") TO "
                        + Identifiers.literal(kept.resolve("kept_" + b + ".parquet").toString()) + " (FORMAT parquet)");
            }
            long outputsKept = DuckDb.queryLong(duck, "SELECT count(*) FROM " + DuckDb.readParquet(glob(kept)), 0);
            say(String.format("address_utxo: %,d of %,d outputs current, %s so far", outputsKept, plan.rowCount(),
                    Progress.duration(System.currentTimeMillis() - started)));

            // Every row, a few date partitions at a time, into the tier of its output.
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
            long sourceRows = 0;
            Written w = new Written(List.of(), List.of());
            for (List<DuckLakeFile> c : chunks) {
                String rows = DuckDb.readParquet(catalog.parquetList(c));
                sourceRows += DuckDb.queryLong(duck, "SELECT count(*) FROM " + rows + " WHERE slot <= " + point.slot(), 0);
                w = write(duck, plan, "SELECT u.*, NOT EXISTS (SELECT 1 FROM (SELECT tx_hash, output_index FROM "
                        + DuckDb.readParquet(glob(kept)) + " WHERE \"date\" IN (SELECT DISTINCT \"date\" FROM " + rows + ")) k"
                        + " WHERE k.tx_hash = u.tx_hash AND k.output_index = u.output_index) AS __history"
                        + " FROM " + rows + " u WHERE u.slot <= " + point.slot(), rewritten);
            }
            long currentRows = rows(w.current());
            long historyRows = rows(w.history());
            if (currentRows + historyRows != sourceRows) {
                throw new IllegalStateException(String.format("address_utxo tiers hold %,d + %,d rows, but the source has"
                        + " %,d up to the point", currentRows, historyRows, sourceRows));
            }
            say(String.format("address_utxo: %,d current and %,d history rows in %d files, written in %s", currentRows,
                    historyRows, w.current().size() + w.history().size(),
                    Progress.duration(System.currentTimeMillis() - started)));
            return tiered(duck, plan, w.current(), w.history(), outputsKept, Retention.UTXO_SPENT_SAFE_BLOCKS,
                    safeBlocks, tiers);
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

    /** Every file written so far, by tier. */
    private record Written(List<DuckLakeFile> current, List<DuckLakeFile> history) {}

    /**
     * Runs {@code select}, whose last column {@code __history} names each row's tier, into Parquet under
     * the relation's own layout: one directory per {@code date}, as the source export has, so batching
     * and dependency narrowing are unchanged. Files are named by tier, so both share a partition.
     */
    private Written write(Connection duck, TablePlan plan, String select, Map<String, Path> rewritten)
            throws SQLException, IOException {
        String prefix = relationPrefix(plan);
        Path dir = outDir.resolve(prefix);
        Path staging = outDir.resolve(".staging");
        deleteRecursively(staging);
        Files.createDirectories(dir);
        // Partitioned by a copy of the date, so the date column itself stays in the file and the tier
        // column does not.
        DuckDb.exec(duck, "COPY (SELECT *, \"date\" AS __date FROM (" + select + ")) TO "
                + Identifiers.literal(staging.toAbsolutePath().toString())
                + " (FORMAT parquet, COMPRESSION zstd, PARTITION_BY (__history, __date), WRITE_PARTITION_COLUMNS false,"
                + " FILENAME_PATTERN 'part_{uuid}')");
        try (Stream<Path> walk = Files.walk(staging)) {
            for (Path p : walk.filter(f -> f.toString().endsWith(".parquet")).sorted().toList()) {
                Path rel = staging.relativize(p);
                String tier = rel.getName(0).toString().equals("__history=true") ? HISTORY : CURRENT;
                String date = rel.getName(1).toString().substring("__date=".length());
                Path target = dir.resolve("date=" + date).resolve(tier + "_" + p.getFileName());
                Files.createDirectories(target.getParent());
                Files.move(p, target);
            }
        } finally {
            deleteRecursively(staging);
        }
        List<DuckLakeFile> current = new ArrayList<>();
        List<DuckLakeFile> history = new ArrayList<>();
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
                (p.getFileName().toString().startsWith(HISTORY + "_") ? history : current).add(known);
                rewritten.put(rel, p.toAbsolutePath());
            }
        }
        current.sort(Comparator.comparing(DuckLakeFile::relativePath));
        history.sort(Comparator.comparing(DuckLakeFile::relativePath));
        return new Written(current, history);
    }

    /**
     * The table with its current files first, then its history files; its row count and bounds stay the
     * whole table's, and the current tier's go in the split.
     */
    private TablePlan tiered(Connection duck, TablePlan plan, List<DuckLakeFile> current, List<DuckLakeFile> history,
                             long currentRows, String key, int value, Map<String, Tiers> tiers) throws SQLException {
        Map<String, String> bounds = bounds(duck, plan, current);
        tiers.put(plan.spec().id(), new Tiers(history.stream().map(DuckLakeFile::relativePath)
                .collect(Collectors.toCollection(TreeSet::new)),
                new SnapshotManifest.HistorySplit(key, Integer.toString(value), currentRows, bounds)));
        List<DuckLakeFile> files = new ArrayList<>(current);
        files.addAll(history);
        return new TablePlan(plan.spec(), files, plan.rowCount(), plan.exactRowCount(), plan.sourceColumns(),
                plan.bounds(), plan.columnFingerprint(), plan.problems());
    }

    /** Observed bounds of {@code files}, written by this pass. */
    private Map<String, String> bounds(Connection duck, TablePlan plan, List<DuckLakeFile> files) throws SQLException {
        Map<String, String> bounds = new TreeMap<>();
        if (files.isEmpty()) {
            return bounds;
        }
        String scan = DuckDb.readParquet(files.stream().map(f -> Identifiers.literal(outDir.resolve(f.relativePath()).toString()))
                .collect(Collectors.joining(", ", "[", "]")));
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
        return bounds;
    }

    /** The newest {@code epochs} epoch partitions are current, the older ones history; nothing is rewritten. */
    private TablePlan splitEpochs(TablePlan plan, int epochs, Map<String, Tiers> tiers) {
        long fileRows = rows(plan.files());
        if (fileRows != plan.rowCount()) {
            throw new IllegalStateException("epoch_stake's " + plan.rowCount() + " rows are not the " + fileRows
                    + " rows of its files; it cannot be split by epoch");
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
        int oldestCurrent = newest - epochs + 1;
        List<DuckLakeFile> current = plan.files().stream().filter(f -> epochOf.get(f) >= oldestCurrent).toList();
        List<DuckLakeFile> history = plan.files().stream().filter(f -> epochOf.get(f) < oldestCurrent).toList();
        say(String.format("epoch_stake: epochs %d to %d current (%,d rows), %d older epochs history (%,d rows)",
                oldestCurrent, newest, rows(current), history.stream().map(epochOf::get).distinct().count(), rows(history)));
        tiers.put(plan.spec().id(), new Tiers(history.stream().map(DuckLakeFile::relativePath)
                .collect(Collectors.toCollection(TreeSet::new)), null));
        List<DuckLakeFile> files = new ArrayList<>(current);
        files.addAll(history);
        return new TablePlan(plan.spec(), files, plan.rowCount(), plan.exactRowCount(), plan.sourceColumns(),
                plan.bounds(), plan.columnFingerprint(), plan.problems());
    }

    private static long rows(List<DuckLakeFile> files) {
        return files.stream().mapToLong(DuckLakeFile::rowCount).sum();
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

    private void say(String message) {
        log.info(message);
        if (progress != null) {
            progress.accept("[tiers] " + message);
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
