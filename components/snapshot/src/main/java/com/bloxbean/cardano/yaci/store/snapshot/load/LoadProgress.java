package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.util.Progress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Time-based load progress: one status line every {@link #INTERVAL_MILLIS} while the load runs, with
 * batches, Parquet bytes, rows, rate, an ETA, and the tables currently loading. The line comes from
 * a timer, not from batch completions: the largest batches go first and can each take many minutes.
 *
 * <p>The ETA is estimated per table ({@link #remainingMillis}), because the cost of a byte differs
 * by table: on mainnet a byte-based ETA said 7 minutes at 79% while the remaining {@code
 * address_utxo} batches took 21, and 10 seconds at 100% of bytes with 1,300 small batches left.
 *
 * <p>Status lines start with {@code [load]} so a caller that samples per-item events can still
 * print every one of them.
 */
final class LoadProgress implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(LoadProgress.class);
    static final long INTERVAL_MILLIS = 30_000;

    /** One batch to load, for the per-table totals. */
    record Planned(String specId, long bytes) {}

    /**
     * One table's planned and finished work. {@code busyMillis} is worker time spent on its finished
     * batches; {@code minBatchMillis} the shortest of them, a floor for each batch still to come.
     */
    record TableLoad(long bytes, int batches, long doneBytes, int doneBatches, long busyMillis,
                     long minBatchMillis) {
        TableLoad plus(long batchBytes, long millis) {
            return new TableLoad(bytes, batches, doneBytes + batchBytes, doneBatches + 1, busyMillis + millis,
                    Math.min(minBatchMillis, millis));
        }
    }

    private final int batches;
    private final long bytes;
    private final int workers;
    private final Consumer<String> sink;
    private final long startedAt = System.currentTimeMillis();
    private final AtomicInteger doneBatches = new AtomicInteger();
    private final AtomicLong doneBytes = new AtomicLong();
    private final AtomicLong doneRows = new AtomicLong();
    private final Map<String, AtomicInteger> active = new ConcurrentHashMap<>();
    private final Map<String, TableLoad> tables = new ConcurrentHashMap<>();
    private ScheduledExecutorService timer;

    LoadProgress(List<Planned> planned, int workers, Consumer<String> sink) {
        this.batches = planned.size();
        this.bytes = planned.stream().mapToLong(Planned::bytes).sum();
        this.workers = Math.max(1, workers);
        this.sink = sink;
        planned.forEach(p -> tables.merge(p.specId(), new TableLoad(p.bytes(), 1, 0, 0, 0, Long.MAX_VALUE),
                (a, b) -> new TableLoad(a.bytes() + b.bytes(), a.batches() + 1, 0, 0, 0, Long.MAX_VALUE)));
    }

    void say(String message) {
        log.info(message);
        if (sink != null) {
            sink.accept("[load] " + message);
        }
    }

    /** Starts the periodic status line; {@link #close()} stops it. */
    LoadProgress start() {
        timer = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("snapshot-load-progress").daemon().factory());
        timer.scheduleAtFixedRate(() -> say(line(System.currentTimeMillis())),
                INTERVAL_MILLIS, INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        return this;
    }

    @Override
    public void close() {
        if (timer != null) {
            timer.shutdownNow();
        }
    }

    /** @return the start time to pass to {@link #finished} */
    long started(String specId) {
        active.computeIfAbsent(specId, k -> new AtomicInteger()).incrementAndGet();
        return System.currentTimeMillis();
    }

    void finished(String specId, long rows, long batchBytes, long startedAt) {
        active.computeIfPresent(specId, (k, n) -> n.decrementAndGet() <= 0 ? null : n);
        tables.computeIfPresent(specId, (k, t) -> t.plus(batchBytes, System.currentTimeMillis() - startedAt));
        doneBatches.incrementAndGet();
        doneBytes.addAndGet(batchBytes);
        doneRows.addAndGet(rows);
    }

    void done() {
        long elapsed = System.currentTimeMillis() - startedAt;
        say(String.format("Loaded %,d batches, %,d rows in %s (%,d rows/s)", doneBatches.get(),
                doneRows.get(), duration(elapsed), elapsed > 0 ? doneRows.get() * 1000 / elapsed : 0));
    }

    String line(long now) {
        long elapsed = Math.max(1, now - startedAt);
        long byteCount = doneBytes.get();
        double fraction = bytes > 0 ? (double) byteCount / bytes : 0;
        long remaining = remainingMillis(tables.values(), workers);
        String eta = remaining >= 0 && doneBatches.get() < batches ? duration(remaining) : "-";
        String running = new TreeMap<>(active).entrySet().stream()
                .map(e -> e.getKey() + (e.getValue().get() > 1 ? "×" + e.getValue().get() : ""))
                .collect(Collectors.joining(", "));
        return String.format("%,d/%,d batches  %s/%s (%.0f%%)  %,d rows  %,d rows/s  elapsed %s  ETA %s  [%s]",
                doneBatches.get(), batches, bytes(byteCount), bytes(bytes), fraction * 100,
                doneRows.get(), doneRows.get() * 1000 / elapsed, duration(elapsed), eta, running);
    }

    /**
     * Wall time left for the unfinished batches, or -1 until a batch has finished.
     *
     * <p>Each table's remaining bytes are costed at that table's own observed worker time per byte,
     * or at the overall rate when none of its batches has finished. Batches go largest first, so
     * a table's last batches are its smallest, and their fixed cost outweighs their bytes: a table's
     * shortest finished batch is the floor for each one left. The work is spread over the workers,
     * except that a table with fewer batches left than workers cannot use them all.
     */
    static long remainingMillis(Collection<TableLoad> tables, int workers) {
        long doneBytes = 0;
        long busy = 0;
        int finished = 0;
        for (TableLoad t : tables) {
            doneBytes += t.doneBytes();
            busy += t.busyMillis();
            finished += t.doneBatches();
        }
        if (finished == 0) {
            return -1;
        }
        double overallPerByte = doneBytes > 0 ? (double) busy / doneBytes : 0;
        double total = 0;
        double longest = 0;
        for (TableLoad t : tables) {
            int left = t.batches() - t.doneBatches();
            if (left <= 0) {
                continue;
            }
            double perByte = t.doneBytes() > 0 ? (double) t.busyMillis() / t.doneBytes() : overallPerByte;
            double work = (t.bytes() - t.doneBytes()) * perByte;
            if (t.doneBatches() > 0) {
                work = Math.max(work, (double) left * t.minBatchMillis());
            }
            total += work;
            longest = Math.max(longest, work / Math.min(workers, left));
        }
        return (long) Math.max(total / Math.max(1, workers), longest);
    }

    static String bytes(long n) {
        return Progress.bytes(n);
    }

    static String duration(long millis) {
        return Progress.duration(millis);
    }
}
