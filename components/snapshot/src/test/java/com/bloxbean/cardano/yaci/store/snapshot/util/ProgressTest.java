package com.bloxbean.cardano.yaci.store.snapshot.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class ProgressTest {

    @Test
    void rateReportsBytesThroughputAndEta() {
        long gib = 1L << 30;
        // 1 GiB of 4 in 10 s: 107 MB/s, 30 s to go.
        assertThat(Progress.rate(gib, 4 * gib, 0, 10_000))
                .isEqualTo("1.0 GiB/4.0 GiB (25%)  107 MB/s  elapsed 10s  ETA 30s");
        assertThat(Progress.rate(0, 4 * gib, 0, 10_000)).endsWith("ETA -");
        assertThat(Progress.rate(4 * gib, 4 * gib, 0, 10_000)).endsWith("ETA -");
    }

    @Test
    void tickerTagsLinesSkipsNullsAndStopsWhenClosed() throws Exception {
        List<String> lines = new CopyOnWriteArrayList<>();
        List<String> untagged = new CopyOnWriteArrayList<>();
        try (Progress.Ticker ignored = Progress.every(10, "load", () -> "status", lines::add);
             Progress.Ticker quiet = Progress.every(10, null, () -> null, untagged::add);
             Progress.Ticker plain = Progress.every(10, null, () -> "raw", untagged::add)) {
            for (int i = 0; i < 200 && (lines.isEmpty() || untagged.isEmpty()); i++) {
                Thread.sleep(10);
            }
        }
        int seen = lines.size();
        Thread.sleep(50);
        assertThat(lines).isNotEmpty().allMatch("[load] status"::equals).hasSize(seen);
        assertThat(untagged).isNotEmpty().allMatch("raw"::equals);
    }
}
