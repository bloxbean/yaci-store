package com.bloxbean.cardano.yaci.store.snapshot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Progress conventions shared by export, verify, import and validation.
 *
 * <p>A progress consumer receives three kinds of messages: phase banners ({@link #PHASE_PREFIX}),
 * which a caller may number; status lines, which start with {@code [tag]} and come from a timer or
 * mark the end of a step; and untagged per-item events, which a caller is free to ignore.
 */
public final class Progress {
    private static final Logger log = LoggerFactory.getLogger(Progress.class);

    public static final String PHASE_PREFIX = "[phase] ";

    /** Interval of the time-based status lines of the I/O-bound steps. */
    public static final long INTERVAL_MILLIS = 30_000;

    private Progress() {
    }

    /** Announces the start of a phase, with its totals in {@code title}. */
    public static void phase(Consumer<String> sink, String title) {
        log.info(title);
        if (sink != null) {
            sink.accept(PHASE_PREFIX + title);
        }
    }

    /**
     * Sends {@code [tag] line} every {@code millis} until closed, and logs it. With a null tag the
     * line is sent as is, for a sink that tags and logs its messages itself. A null line is skipped,
     * so the supplier can stay quiet when there is nothing new to say.
     */
    public static Ticker every(long millis, String tag, Supplier<String> line, Consumer<String> sink) {
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("snapshot-progress").daemon().factory());
        if (sink != null) {
            timer.scheduleAtFixedRate(() -> {
                String s = line.get();
                if (s == null) {
                    return;
                }
                if (tag == null) {
                    sink.accept(s);
                } else {
                    log.info(s);
                    sink.accept("[" + tag + "] " + s);
                }
            }, millis, millis, TimeUnit.MILLISECONDS);
        }
        return new Ticker(timer);
    }

    /** Stops the periodic status line. */
    public record Ticker(ScheduledExecutorService timer) implements AutoCloseable {
        @Override
        public void close() {
            timer.shutdownNow();
        }
    }

    public static String bytes(long n) {
        if (n >= 1L << 30) return String.format("%.1f GiB", n / (double) (1L << 30));
        if (n >= 1L << 20) return String.format("%.1f MiB", n / (double) (1L << 20));
        return String.format("%,d B", n);
    }

    public static String duration(long millis) {
        long s = millis / 1000;
        if (s >= 3600) return String.format("%dh%02dm", s / 3600, (s % 3600) / 60);
        if (s >= 60) return String.format("%dm%02ds", s / 60, s % 60);
        return s + "s";
    }

    /**
     * {@code done/total  MB/s  ETA} for a byte-bounded step started at {@code startedAt}; the ETA is
     * "-" until some bytes are done.
     */
    public static String rate(long done, long total, long startedAt, long now) {
        long elapsed = Math.max(1, now - startedAt);
        String eta = done > 0 && done < total ? duration((long) (elapsed * (double) (total - done) / done)) : "-";
        return String.format("%s/%s (%.0f%%)  %.0f MB/s  elapsed %s  ETA %s", bytes(done), bytes(total),
                total > 0 ? done * 100.0 / total : 0, done / 1e3 / elapsed, duration(elapsed), eta);
    }
}
