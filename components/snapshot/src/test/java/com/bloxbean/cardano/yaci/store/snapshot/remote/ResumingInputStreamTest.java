package com.bloxbean.cardano.yaci.store.snapshot.remote;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.LongConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumingInputStreamTest {

    private static final byte[] DATA = new byte[100_000];

    static {
        new java.util.Random(7).nextBytes(DATA);
    }

    @Test
    void aDroppedConnectionResumesFromTheNextByte() throws Exception {
        // Each connection breaks after 30,000 bytes, as a flaky CDN does.
        var store = new FlakyStore(30_000, 0, false);

        byte[] read;
        try (InputStream in = new ResumingInputStream(store, "k", DATA.length, Duration.ZERO)) {
            read = in.readAllBytes();
        }

        assertThat(read).isEqualTo(DATA);
        assertThat(store.offsets).containsExactly(0L, 30_000L, 60_000L, 90_000L);
    }

    @Test
    void aConnectionThatEndsEarlyWithoutAnErrorIsResumedToo() throws Exception {
        var store = new FlakyStore(40_000, 0, true);

        try (InputStream in = new ResumingInputStream(store, "k", DATA.length, Duration.ZERO)) {
            assertThat(in.readAllBytes()).isEqualTo(DATA);
        }
        assertThat(store.offsets).containsExactly(0L, 40_000L, 80_000L);
    }

    @Test
    void givesUpAfterRepeatedFailuresAtOnePosition() throws Exception {
        // The first connection breaks at 30,000; every reopen after it fails.
        var store = new FlakyStore(30_000, Integer.MAX_VALUE, false);

        try (InputStream in = new ResumingInputStream(store, "k", DATA.length, Duration.ZERO)) {
            assertThatThrownBy(in::readAllBytes).isInstanceOf(IOException.class);
        }
        assertThat(store.offsets).hasSize(1 + ResumingInputStream.ATTEMPTS_WITHOUT_PROGRESS);
    }

    @Test
    void aMissingObjectIsNotRetried() throws Exception {
        var store = new FlakyStore(30_000, 0, false) {
            @Override
            public InputStream open(String key, long offset) throws IOException {
                InputStream in = super.open(key, offset);
                if (offset > 0) {
                    throw new NoSuchFileException(key);
                }
                return in;
            }
        };
        try (InputStream in = new ResumingInputStream(store, "k", DATA.length, Duration.ZERO)) {
            assertThatThrownBy(in::readAllBytes).isInstanceOf(NoSuchFileException.class);
        }
        assertThat(store.offsets).containsExactly(0L, 30_000L);
    }

    /** Serves {@link #DATA}; each connection delivers at most {@code perConnection} bytes. */
    static class FlakyStore implements SnapshotStore {
        final List<Long> offsets = new ArrayList<>();
        private final int perConnection;
        private int failingReopens;
        private final boolean endQuietly;

        FlakyStore(int perConnection, int failingReopens, boolean endQuietly) {
            this.perConnection = perConnection;
            this.failingReopens = failingReopens;
            this.endQuietly = endQuietly;
        }

        @Override
        public InputStream open(String key) throws IOException {
            return open(key, 0);
        }

        @Override
        public InputStream open(String key, long offset) throws IOException {
            offsets.add(offset);
            if (offset > 0 && failingReopens-- > 0) {
                throw new IOException("connection refused");
            }
            int from = (int) offset;
            int to = Math.min(DATA.length, from + perConnection);
            InputStream slice = new ByteArrayInputStream(DATA, from, to - from);
            return new InputStream() {
                @Override
                public int read() throws IOException {
                    int b = slice.read();
                    return b < 0 && to < DATA.length && !endQuietly ? fail() : b;
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    int n = slice.read(b, off, len);
                    return n < 0 && to < DATA.length && !endQuietly ? fail() : n;
                }

                private int fail() throws IOException {
                    throw new IOException("connection reset");
                }
            };
        }

        @Override public String describe() { return "flaky"; }
        @Override public boolean writable() { return false; }
        @Override public Optional<ObjectInfo> head(String key) { return Optional.empty(); }
        @Override public Optional<Tagged> read(String key) { return Optional.empty(); }
        @Override public List<String> list(String prefix) { return List.of(); }
        @Override public void put(String key, Path file, String sha256, LongConsumer sent) { }
        @Override public boolean putIfUnchanged(String key, byte[] bytes, String ifMatch) { return false; }
        @Override public void delete(String key) { }
    }
}
