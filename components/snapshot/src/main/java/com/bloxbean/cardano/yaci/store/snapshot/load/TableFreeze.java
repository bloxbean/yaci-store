package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import com.bloxbean.cardano.yaci.store.snapshot.util.Progress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Freezes the loaded tables while none of their keys and indexes exist yet.
 *
 * <p>A freshly loaded page is written three more times otherwise: the key rebuild's scan sets its
 * hint bits, validation reads it, and the first autovacuum freezes it and sets the visibility map.
 * With data checksums, on by default from PostgreSQL 18, a hint-bit change after a checkpoint logs a
 * full page image, so on mainnet the WAL reached twice the size of the restored database and the
 * rebuild waited on WAL writes, and autovacuum then ran for hours after the import. One
 * {@code VACUUM (FREEZE)} per table does that work once, without index passes, and leaves pages the
 * rebuild, validation and autovacuum only read.
 */
final class TableFreeze {

    static final long INTERVAL_MILLIS = 60_000;

    /** A larger ring than VACUUM's default, so dirty pages are not flushed, WAL first, every few KB. */
    static final String BUFFER_USAGE_LIMIT = "256MB";

    private TableFreeze() {
    }

    /** Freezes every leaf table below {@code tables}, largest first, on up to {@code workers} connections. */
    static void freeze(Connection pg, String schema, List<String> tables, int workers,
                       DeferredIndexes.ConnectionFactory connections, Consumer<String> say) throws SQLException {
        Map<String, Long> sizes = sizes(pg, schema, Autovacuum.leafTables(pg, schema, tables));
        if (sizes.isEmpty()) {
            return;
        }
        String options = pg.getMetaData().getDatabaseMajorVersion() >= 16
                ? "FREEZE, BUFFER_USAGE_LIMIT '" + BUFFER_USAGE_LIMIT + "'" : "FREEZE";
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>(sizes.keySet());
        long total = sizes.values().stream().mapToLong(Long::longValue).sum();

        long started = System.currentTimeMillis();
        int threads = Math.max(1, Math.min(workers, sizes.size()));
        Set<String> running = ConcurrentHashMap.newKeySet();
        AtomicInteger done = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try (Progress.Ticker ignored = Progress.every(INTERVAL_MILLIS, null,
                () -> done.get() + "/" + sizes.size() + " table(s) frozen, elapsed "
                        + LoadProgress.duration(System.currentTimeMillis() - started)
                        + (running.isEmpty() ? "" : "; freezing " + String.join(", ", new TreeSet<>(running))),
                say)) {
            List<Future<Void>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    try (Connection c = connections.open(); Statement st = c.createStatement()) {
                        for (String table = queue.poll(); table != null; table = queue.poll()) {
                            running.add(table);
                            st.execute("VACUUM (" + options + ") " + Identifiers.quote(schema) + "."
                                    + Identifiers.quote(table));
                            running.remove(table);
                            done.incrementAndGet();
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
            throw new SQLException("Interrupted while freezing tables", e);
        } catch (java.util.concurrent.ExecutionException e) {
            queue.clear();
            Throwable cause = e.getCause();
            throw cause instanceof SQLException sql ? sql : new SQLException("VACUUM (FREEZE) failed", cause);
        } finally {
            pool.shutdownNow();
        }
        say.accept("Froze " + sizes.size() + " table(s), " + Progress.bytes(total) + ", in "
                + LoadProgress.duration(System.currentTimeMillis() - started));
    }

    /** Leaf table to heap size, largest first. */
    private static Map<String, Long> sizes(Connection pg, String schema, List<String> leaves) throws SQLException {
        Map<String, Long> out = new LinkedHashMap<>();
        try (PreparedStatement ps = pg.prepareStatement("SELECT c.relname, pg_relation_size(c.oid)"
                + " FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace"
                + " WHERE n.nspname = ? AND c.relname = ANY (?) ORDER BY 2 DESC, 1")) {
            ps.setString(1, schema);
            ps.setArray(2, pg.createArrayOf("text", leaves.toArray()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.put(rs.getString(1), rs.getLong(2));
                }
            }
        }
        return out;
    }
}
