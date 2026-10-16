package com.bloxbean.cardano.yaci.store.snapshot.validate;

import com.bloxbean.cardano.yaci.store.snapshot.load.DeferredIndexes;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Reads each loaded table once for every per-row validation check.
 *
 * <p>Row count, required-column nulls, the cutoff and, for {@code address_utxo}, the amount checks
 * all come from one {@code count(*) FILTER (...)} pass. On mainnet the checks used to be separate
 * queries on one connection: {@code address_utxo} alone was read seven times and validation took
 * longer than the load and the index rebuild together.
 */
final class TableScanner {

    static final long INTERVAL_MILLIS = 60_000;

    /** What to read from one table; {@code cutoffColumn} is null when the table has no cutoff. */
    record Target(String table, List<String> requiredColumns, String cutoffColumn, long cutoffLimit,
                  boolean addressUtxo, long expectedRows) {}

    /** {@code utxo} is null unless the target asked for the address_utxo checks. */
    record Result(long rows, List<Long> nulls, Long beyondCutoff, UtxoResult utxo) {}

    record UtxoResult(long badAmounts, long lovelaceMismatch, long multiLovelace, long missingBlock) {}

    private TableScanner() {
    }

    /**
     * Scans the targets largest first. With a connection factory and more than one worker the tables
     * are read in parallel; otherwise they are read in turn on {@code shared}.
     */
    static Map<String, Result> scan(String schema, List<Target> targets, Connection shared,
                                    DeferredIndexes.ConnectionFactory connections, int workers,
                                    Consumer<String> say) throws SQLException {
        Map<String, Result> results = new ConcurrentHashMap<>();
        if (targets.isEmpty()) {
            return results;
        }
        List<Target> ordered = new ArrayList<>(targets);
        ordered.sort(Comparator.comparingLong(Target::expectedRows).reversed());
        int threads = connections == null ? 1 : Math.max(1, Math.min(workers, ordered.size()));
        long started = System.currentTimeMillis();
        say.accept("Scanning " + ordered.size() + " table(s) with " + threads + " connection(s)");

        Set<String> running = ConcurrentHashMap.newKeySet();
        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "snapshot-validate-progress");
            t.setDaemon(true);
            return t;
        });
        timer.scheduleAtFixedRate(() -> say.accept(results.size() + "/" + ordered.size()
                        + " table(s) scanned, elapsed " + elapsed(started)
                        + (running.isEmpty() ? "" : "; scanning " + String.join(", ", running))),
                INTERVAL_MILLIS, INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        try {
            if (threads == 1) {
                for (Target t : ordered) {
                    running.add(t.table());
                    results.put(t.table(), scanOne(shared, schema, t));
                    running.remove(t.table());
                }
            } else {
                parallel(schema, ordered, connections, threads, results, running);
            }
        } finally {
            timer.shutdownNow();
        }
        say.accept("Scanned " + ordered.size() + " table(s) in " + elapsed(started));
        return results;
    }

    private static void parallel(String schema, List<Target> ordered, DeferredIndexes.ConnectionFactory connections,
                                 int threads, Map<String, Result> results, Set<String> running) throws SQLException {
        ConcurrentLinkedQueue<Target> queue = new ConcurrentLinkedQueue<>(ordered);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    try (Connection c = connections.open()) {
                        // The largest table is scanned first and alone at the end; let its query use the
                        // server's parallel workers. On mainnet address_utxo's single scan otherwise ran
                        // with the default 2 while the other connections had finished.
                        try (Statement st = c.createStatement()) {
                            st.execute("SET max_parallel_workers_per_gather = " + threads);
                        }
                        for (Target t = queue.poll(); t != null; t = queue.poll()) {
                            running.add(t.table());
                            results.put(t.table(), scanOne(c, schema, t));
                            running.remove(t.table());
                        }
                    }
                    return null;
                }));
            }
            for (Future<Void> f : futures) {
                f.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while scanning tables", e);
        } catch (ExecutionException e) {
            queue.clear();
            Throwable cause = e.getCause();
            throw cause instanceof SQLException sql ? sql : new SQLException("Table scan failed", cause);
        } finally {
            pool.shutdownNow();
        }
    }

    static Result scanOne(Connection pg, String schema, Target t) throws SQLException {
        try (Statement st = pg.createStatement(); ResultSet rs = st.executeQuery(sql(schema, t))) {
            rs.next();
            int i = 1;
            long rows = rs.getLong(i++);
            List<Long> nulls = new ArrayList<>();
            for (int n = 0; n < t.requiredColumns().size(); n++) {
                nulls.add(rs.getLong(i++));
            }
            Long beyond = t.cutoffColumn() == null ? null : rs.getLong(i++);
            UtxoResult utxo = t.addressUtxo()
                    ? new UtxoResult(rs.getLong(i++), rs.getLong(i++), rs.getLong(i++), rs.getLong(i))
                    : null;
            return new Result(rows, nulls, beyond, utxo);
        }
    }

    static String sql(String schema, Target t) {
        StringBuilder sql = new StringBuilder("SELECT count(*)");
        for (String c : t.requiredColumns()) {
            sql.append(", count(*) FILTER (WHERE u.").append(Identifiers.quote(c)).append(" IS NULL)");
        }
        if (t.cutoffColumn() != null) {
            sql.append(", count(*) FILTER (WHERE u.").append(Identifiers.quote(t.cutoffColumn()))
                    .append(" > ").append(t.cutoffLimit()).append(')');
        }
        String table = Identifiers.quote(schema) + "." + Identifiers.quote(t.table());
        if (t.addressUtxo()) {
            // The lovelace quantities come from a JSON path, not a subquery over jsonb_array_elements:
            // a subquery that refers to the row is parallel-restricted and ran in the leader only, so
            // the 355M-row mainnet scan used one core. More than one lovelace entry fails its own
            // check; the total is then not compared again.
            sql.append(", count(*) FILTER (WHERE CASE WHEN jsonb_typeof(u.amounts) = 'array'"
                            + " THEN jsonb_array_length(u.amounts) = 0 ELSE true END)")
                    .append(", count(*) FILTER (WHERE CASE jsonb_array_length(u.lovelace)"
                            + " WHEN 0 THEN u.lovelace_amount IS DISTINCT FROM 0"
                            + " WHEN 1 THEN u.lovelace_amount IS DISTINCT FROM (u.lovelace->>0)::numeric"
                            + " ELSE true END)")
                    .append(", count(*) FILTER (WHERE jsonb_array_length(u.lovelace) > 1)")
                    .append(", count(*) FILTER (WHERE u.block IS NULL)");
            table = "(SELECT r.*, CASE WHEN jsonb_typeof(r.amounts) = 'array'"
                    + " THEN jsonb_path_query_array(r.amounts, '$[*] ? (@.unit == \"lovelace\").quantity')"
                    + " ELSE '[]'::jsonb END AS lovelace FROM " + table + " r)";
        }
        sql.append(" FROM ").append(table).append(" u");
        return sql.toString();
    }

    private static String elapsed(long started) {
        long s = (System.currentTimeMillis() - started) / 1000;
        return s >= 3600 ? String.format("%dh%02dm", s / 3600, s % 3600 / 60)
                : String.format("%dm%02ds", s / 60, s % 60);
    }
}
