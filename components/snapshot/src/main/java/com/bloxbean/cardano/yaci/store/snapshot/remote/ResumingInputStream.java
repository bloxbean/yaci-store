package com.bloxbean.cardano.yaci.store.snapshot.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.file.NoSuchFileException;
import java.time.Duration;

/**
 * Reads an object to its end across dropped connections. When a read fails, the object is opened
 * again from the next byte, so a part of several gigabytes does not start over and the bytes already
 * read are kept. The reader still checks the whole object's size and SHA-256: a resumed read that
 * returned other bytes fails that check, and the part is then read again from the start.
 */
final class ResumingInputStream extends InputStream {

    private static final Logger log = LoggerFactory.getLogger(ResumingInputStream.class);

    /** Failures in a row at one position, reopening included, before the read fails. */
    static final int ATTEMPTS_WITHOUT_PROGRESS = 5;

    /** Resumes of one object in all, however much each one read. */
    static final int MAX_RESUMES = 100;

    private final SnapshotStore store;
    private final String key;
    private final long size;
    private final Duration backoff;
    private InputStream in;
    private long position;
    private long failedAt = -1;
    private int failuresHere;
    private int resumes;
    private volatile boolean closed;

    /**
     * @param size the object's size, or -1 when unknown; a read that ends before it is resumed too
     * @param backoff the wait before the first reopen at a position, doubled for each one after it
     */
    ResumingInputStream(SnapshotStore store, String key, long size, Duration backoff) throws IOException {
        this.store = store;
        this.key = key;
        this.size = size;
        this.backoff = backoff;
        this.in = store.open(key);
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        return n < 0 ? -1 : one[0] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return 0;
        }
        while (true) {
            try {
                if (in == null) {
                    in = store.open(key, position);
                }
                int n = in.read(b, off, len);
                if (n > 0) {
                    position += n;
                } else if (n < 0 && size >= 0 && position < size) {
                    throw new IOException("the connection ended at byte " + position + " of " + size);
                }
                return n;
            } catch (NoSuchFileException e) {
                throw e;
            } catch (IOException e) {
                if (closed) {
                    throw e;
                }
                closeQuietly();
                backOff(e);
            }
        }
    }

    /** Waits before the next reopen, or rethrows {@code e} when this position has failed too often. */
    private void backOff(IOException e) throws IOException {
        failuresHere = position == failedAt ? failuresHere + 1 : 1;
        failedAt = position;
        if (failuresHere > ATTEMPTS_WITHOUT_PROGRESS || ++resumes > MAX_RESUMES) {
            throw e;
        }
        log.warn("Reading {} failed at byte {} ({}); resuming from there ({}/{})", key, position, e.getMessage(),
                failuresHere, ATTEMPTS_WITHOUT_PROGRESS);
        try {
            Thread.sleep(backoff.toMillis() << Math.min(failuresHere - 1, 5));
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            InterruptedIOException x = new InterruptedIOException("Interrupted while resuming " + key);
            x.initCause(e);
            throw x;
        }
    }

    private void closeQuietly() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
                // the connection is already broken
            }
            in = null;
        }
    }

    @Override
    public int available() throws IOException {
        return in == null ? 0 : in.available();
    }

    @Override
    public void close() throws IOException {
        closed = true;
        if (in != null) {
            in.close();
        }
    }
}
