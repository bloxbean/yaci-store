package com.bloxbean.cardano.yaci.store.snapshot.remote;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Fails a download that stops sending. The JDK HTTP client's timeout covers only the response
 * headers, so a connection that stalls mid-body would otherwise block a read forever. When no byte
 * arrives for {@code idle}, the stream is closed and the blocked read fails, so the caller's retry
 * starts the part again.
 */
final class StallGuard extends FilterInputStream {

    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "snapshot-stall-guard");
        t.setDaemon(true);
        return t;
    });

    private final long idleNanos;
    private final String what;
    private final ScheduledFuture<?> check;
    private volatile long lastRead = System.nanoTime();
    private volatile boolean stalled;

    StallGuard(InputStream in, Duration idle, String what) {
        super(in);
        this.idleNanos = idle.toNanos();
        this.what = what;
        long period = Math.max(100, Math.min(5_000, idle.toMillis() / 4));
        this.check = WATCHDOG.scheduleAtFixedRate(this::checkIdle, period, period, TimeUnit.MILLISECONDS);
    }

    private void checkIdle() {
        if (System.nanoTime() - lastRead > idleNanos) {
            stalled = true;
            check.cancel(false);
            try {
                in.close();
            } catch (IOException ignored) {
                // the blocked read fails either way
            }
        }
    }

    @Override
    public int read() throws IOException {
        int b = guarded(super.read());
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return guarded(super.read(b, off, len));
    }

    /** Skips by reading, so a long skip counts as activity and is stopped by a stall like a read. */
    @Override
    public long skip(long n) throws IOException {
        byte[] buf = new byte[(int) Math.min(Math.max(n, 0), 1 << 16)];
        long skipped = 0;
        while (skipped < n) {
            int k = read(buf, 0, (int) Math.min(buf.length, n - skipped));
            if (k < 0) {
                break;
            }
            skipped += k;
        }
        return skipped;
    }

    private int guarded(int result) throws IOException {
        if (stalled) {
            throw new IOException(what + " stalled: no data for " + Duration.ofNanos(idleNanos).toSeconds() + "s");
        }
        lastRead = System.nanoTime();
        return result;
    }

    @Override
    public void close() throws IOException {
        check.cancel(false);
        super.close();
    }
}
