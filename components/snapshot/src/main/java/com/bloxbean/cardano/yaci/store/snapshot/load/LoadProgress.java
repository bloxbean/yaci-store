package com.bloxbean.cardano.yaci.store.snapshot.load;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Time-based load progress: one status line at most every {@link #INTERVAL_MILLIS}, with batches,
 * Parquet bytes, rows, rate, an ETA from the byte rate, and the tables currently loading.
 *
 * <p>Status lines start with {@code [load]} so a caller that samples per-item events can still
 * print every one of them.
 */
final class LoadProgress {
    private static final Logger log = LoggerFactory.getLogger(LoadProgress.class);
    static final long INTERVAL_MILLIS = 30_000;

    private final int batches;
    private final long bytes;
    private final Consumer<String> sink;
    private final long startedAt = System.currentTimeMillis();
    private final AtomicLong lastReport = new AtomicLong(startedAt);
    private final AtomicInteger doneBatches = new AtomicInteger();
    private final AtomicLong doneBytes = new AtomicLong();
    private final AtomicLong doneRows = new AtomicLong();
    private final Map<String, AtomicInteger> active = new ConcurrentHashMap<>();

    LoadProgress(int batches, long bytes, Consumer<String> sink) {
        this.batches = batches;
        this.bytes = bytes;
        this.sink = sink;
    }

    void say(String message) {
        log.info(message);
        if (sink != null) {
            sink.accept("[load] " + message);
        }
    }

    void started(String specId) {
        active.computeIfAbsent(specId, k -> new AtomicInteger()).incrementAndGet();
    }

    void finished(String specId, long rows, long batchBytes) {
        active.computeIfPresent(specId, (k, n) -> n.decrementAndGet() <= 0 ? null : n);
        doneBatches.incrementAndGet();
        doneBytes.addAndGet(batchBytes);
        doneRows.addAndGet(rows);
        long now = System.currentTimeMillis();
        long last = lastReport.get();
        if (now - last >= INTERVAL_MILLIS && lastReport.compareAndSet(last, now)) {
            say(line(now));
        }
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
        String eta = byteCount > 0 && fraction < 1
                ? duration((long) (elapsed * (1 - fraction) / fraction)) : "-";
        String tables = new TreeMap<>(active).entrySet().stream()
                .map(e -> e.getKey() + (e.getValue().get() > 1 ? "×" + e.getValue().get() : ""))
                .collect(Collectors.joining(", "));
        return String.format("%,d/%,d batches  %s/%s (%.0f%%)  %,d rows  %,d rows/s  elapsed %s  ETA %s  [%s]",
                doneBatches.get(), batches, bytes(byteCount), bytes(bytes), fraction * 100,
                doneRows.get(), doneRows.get() * 1000 / elapsed, duration(elapsed), eta, tables);
    }

    static String bytes(long n) {
        if (n >= 1L << 30) return String.format("%.1f GiB", n / (double) (1L << 30));
        if (n >= 1L << 20) return String.format("%.1f MiB", n / (double) (1L << 20));
        return String.format("%,d B", n);
    }

    static String duration(long millis) {
        long s = millis / 1000;
        if (s >= 3600) return String.format("%dh%02dm", s / 3600, (s % 3600) / 60);
        if (s >= 60) return String.format("%dm%02ds", s / 60, s % 60);
        return s + "s";
    }
}
