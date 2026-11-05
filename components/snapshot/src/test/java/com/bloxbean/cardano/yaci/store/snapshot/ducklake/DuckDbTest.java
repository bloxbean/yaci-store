package com.bloxbean.cardano.yaci.store.snapshot.ducklake;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DuckDbTest {

    @Test
    void parsesDuckDbSizes() {
        assertThat(DuckDb.parseBytes("1GB")).isEqualTo(1_000_000_000L);
        assertThat(DuckDb.parseBytes("4GiB")).isEqualTo(4L * 1024 * 1024 * 1024);
        assertThat(DuckDb.parseBytes("512mb")).isEqualTo(512_000_000L);
        assertThat(DuckDb.parseBytes("1.5GB")).isEqualTo(1_500_000_000L);
        assertThat(DuckDb.parseBytes("2048")).isEqualTo(2048L);
        assertThatThrownBy(() -> DuckDb.parseBytes("lots")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void threadsFollowTheMemoryLimitNotTheCoreCount() {
        int cores = Runtime.getRuntime().availableProcessors();
        assertThat(DuckDb.threadsFor("256MB")).isEqualTo(1);
        assertThat(DuckDb.threadsFor("1GiB")).isEqualTo(Math.min(cores, 2));
        assertThat(DuckDb.threadsFor("1TB")).isEqualTo(cores);
    }
}
