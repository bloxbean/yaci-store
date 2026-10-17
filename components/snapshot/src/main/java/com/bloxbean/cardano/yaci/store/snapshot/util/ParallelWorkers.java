package com.bloxbean.cardano.yaci.store.snapshot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Postgres hands out parallel workers when a query or index build starts, from one pool shared by
 * every session. Started together with smaller work, the longest one can find the pool empty and run
 * in a single process. Callers start it first and the rest once {@link #awaitLaunched} returns.
 */
public final class ParallelWorkers {
    private static final Logger log = LoggerFactory.getLogger(ParallelWorkers.class);

    /** How long the other work waits for the first one's workers before starting anyway. */
    public static final long WAIT_MILLIS = 120_000;

    private ParallelWorkers() {
    }

    /** The backend process id of {@code c}, to pass to {@link #awaitLaunched}. */
    public static int backendPid(Connection c) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT pg_backend_pid()")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Waits until the backend {@code pid} has parallel workers running, {@code firstDone} is counted
     * down, or {@link #WAIT_MILLIS} passed. Work that gets no workers is not waited for longer.
     * {@code monitor} is a separate connection, used under its own lock.
     */
    public static void awaitLaunched(Connection monitor, AtomicInteger pid, CountDownLatch firstDone) {
        long deadline = System.currentTimeMillis() + WAIT_MILLIS;
        try {
            while (!firstDone.await(500, TimeUnit.MILLISECONDS) && System.currentTimeMillis() < deadline) {
                if (pid.get() == 0) {
                    continue;
                }
                synchronized (monitor) {
                    try (PreparedStatement ps = monitor.prepareStatement(
                            "SELECT count(*) FROM pg_stat_activity WHERE leader_pid = ?")) {
                        ps.setInt(1, pid.get());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getLong(1) > 0) {
                                return;
                            }
                        }
                    } catch (SQLException e) {
                        log.debug("Unable to read parallel workers", e);
                        return;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
