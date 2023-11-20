package com.bloxbean.cardano.yaci.store.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListUtilTest {

    @Test
    void partition() {
        List<Integer> list = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22);
        List<List<Integer>> partitions = ListUtil.partition(list, 4);
        for (var partition : partitions) {
            System.out.println(partition);
        }

        assertThat(partitions).hasSize(6);
        assertThat(partitions.get(5)).hasSize(2);
    }
}
