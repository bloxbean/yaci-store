package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Collects planner statistics for the loaded tables.
 *
 * <p>A bulk-loaded table has none until autovacuum reaches it, and without them PostgreSQL guesses a
 * third of the rows for every range predicate. On mainnet the app's first restart rollback,
 * {@code DELETE ... WHERE slot > ?} on {@code transaction}, then read the whole 131 GB table instead
 * of using {@code idx_transaction_slot}, and the handlers' own queries are planned the same way.
 * {@code ANALYZE} samples a fixed number of rows per table, so it takes minutes, not a table scan.
 */
final class TableStatistics {

    private TableStatistics() {
    }

    /** Analyzes the tables largest first on up to {@code workers} connections. */
    static void analyze(String schema, List<String> tables, Map<String, Long> rowsPerTable, int workers,
                        DeferredIndexes.ConnectionFactory connections, Consumer<String> say) throws SQLException {
        if (tables.isEmpty()) {
            return;
        }
        List<String> ordered = new ArrayList<>(tables);
        ordered.sort(Comparator.comparingLong((String t) -> rowsPerTable.getOrDefault(t, 0L)).reversed());
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>(ordered);

        long started = System.currentTimeMillis();
        int threads = Math.max(1, Math.min(workers, ordered.size()));
        say.accept("Analyzing " + ordered.size() + " table(s) with " + threads + " connection(s)");
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Void>> running = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                running.add(pool.submit(() -> {
                    try (Connection c = connections.open(); Statement st = c.createStatement()) {
                        for (String table = queue.poll(); table != null; table = queue.poll()) {
                            st.execute("ANALYZE " + Identifiers.quote(schema) + "." + Identifiers.quote(table));
                        }
                    }
                    return null;
                }));
            }
            for (Future<Void> f : running) {
                f.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while analyzing tables", e);
        } catch (java.util.concurrent.ExecutionException e) {
            queue.clear();
            Throwable cause = e.getCause();
            throw cause instanceof SQLException sql ? sql : new SQLException("ANALYZE failed", cause);
        } finally {
            pool.shutdownNow();
        }
        say.accept("Analyzed " + ordered.size() + " table(s) in "
                + LoadProgress.duration(System.currentTimeMillis() - started));
    }
}
