package com.bloxbean.cardano.yaci.store.snapshot.load;

import org.junit.jupiter.api.Test;
import java.sql.SQLException;
import static org.assertj.core.api.Assertions.assertThat;

class UtxoBatchLoaderTest {
    @Test
    void smallerMemoryUsesMoreSlicesAndWideInputsAreAccountedFor() {
        assertThat(UtxoBatchLoader.initialBuckets(200_000, 10_000_000, 128_000_000))
                .isGreaterThan(UtxoBatchLoader.initialBuckets(200_000, 10_000_000, 4_000_000_000L));
        assertThat(UtxoBatchLoader.initialBuckets(10, 100_000_000, 128_000_000)).isGreaterThan(1);
        assertThat(UtxoBatchLoader.initialBuckets(0, 0, 128_000_000)).isEqualTo(1);
        // The densest mainnet day ran in one pass with 2GB; 3GB per worker must not slice it.
        assertThat(UtxoBatchLoader.initialBuckets(2_960_901, 98_666_229, 3_000_000_000L)).isEqualTo(1);
        assertThat(UtxoBatchLoader.initialBuckets(2_960_901, 98_666_229, 1_000_000_000L)).isEqualTo(4);
    }

    @Test
    void readsDuckDbMemoryUnitsAndRetriesOnlyOutOfMemory() {
        assertThat(UtxoBatchLoader.memoryBytes("1 GiB")).isEqualTo(1073741824L);
        assertThat(UtxoBatchLoader.memoryBytes("128 MB")).isEqualTo(128000000L);
        assertThat(UtxoBatchLoader.memoryBytes("953.6 MiB")).isBetween(999000000L, 1000000000L);
        assertThat(UtxoBatchLoader.isOutOfMemory(new SQLException("Out of Memory Error: failed to allocate"))).isTrue();
        assertThat(UtxoBatchLoader.isOutOfMemory(new SQLException("duplicate key value"))).isFalse();
    }
}
