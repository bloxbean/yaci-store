package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.load.LoadProgress.TableLoad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoadProgressTest {

    private static final long NONE = Long.MAX_VALUE;

    @Test
    void unknownUntilABatchHasFinished() {
        assertThat(LoadProgress.remainingMillis(List.of(new TableLoad(100, 4, 0, 0, 0, NONE)), 4)).isEqualTo(-1);
    }

    @Test
    void eachTableIsCostedAtItsOwnRate() {
        // address_utxo costs 100 ms a byte, block 10: half of address_utxo's bytes are left, so 5 s
        // remain. The overall byte rate (6 s for 150 of 200 bytes) would have said 2 s.
        var utxo = new TableLoad(100, 10, 50, 5, 5_000, 1_000);
        var block = new TableLoad(100, 10, 100, 10, 1_000, 100);
        assertThat(LoadProgress.remainingMillis(List.of(utxo, block), 1)).isEqualTo(5_000);
    }

    @Test
    void manySmallBatchesLeftAreCostedPerBatch() {
        // 99.9% of the bytes are done but 90 small batches are left, each at least as long as the
        // shortest finished one (2 s): 180 s of work over 8 workers.
        var utxo = new TableLoad(10_000, 100, 9_990, 10, 99_900, 2_000);
        assertThat(LoadProgress.remainingMillis(List.of(utxo), 8)).isEqualTo(22_500);
    }

    @Test
    void aTableNotStartedYetUsesTheOverallRate() {
        var done = new TableLoad(100, 1, 100, 1, 1_000, 1_000);
        var pending = new TableLoad(50, 5, 0, 0, 0, NONE);
        assertThat(LoadProgress.remainingMillis(List.of(done, pending), 1)).isEqualTo(500);
        assertThat(LoadProgress.remainingMillis(List.of(done, pending), 5)).isEqualTo(100);
    }

    @Test
    void aSingleBatchLeftCannotBeSplitOverWorkers() {
        var big = new TableLoad(1_000, 2, 500, 1, 50_000, 50_000);
        var small = new TableLoad(80, 8, 0, 0, 0, NONE);
        // 50 s for the one big batch dominates the 8 small ones spread over the other workers.
        assertThat(LoadProgress.remainingMillis(List.of(big, small), 8)).isEqualTo(50_000);
    }

    @Test
    void statusLineAddsUpPlannedBatchesPerTable() {
        var progress = new LoadProgress(List.of(new LoadProgress.Planned("a", 10),
                new LoadProgress.Planned("a", 20), new LoadProgress.Planned("b", 5)), 2, null);
        assertThat(progress.line(System.currentTimeMillis())).startsWith("0/3 batches  0 B/35 B (0%)")
                .contains("ETA -");
        long started = progress.started("a");
        progress.finished("a", 7, 20, started);
        assertThat(progress.line(System.currentTimeMillis())).startsWith("1/3 batches  20 B/35 B (57%)  7 rows")
                .doesNotContain("ETA -");
    }
}
