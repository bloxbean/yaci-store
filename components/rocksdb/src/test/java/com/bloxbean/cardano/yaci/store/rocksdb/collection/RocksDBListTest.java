package com.bloxbean.cardano.yaci.store.rocksdb.collection;

import com.bloxbean.cardano.yaci.store.rocksdb.RocksDBBaseTest;
import com.bloxbean.cardano.yaci.store.rocksdb.types.RocksDBList;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RocksDBListTest extends RocksDBBaseTest {

    @Override
    public String getColumnFamilies() {
        return "list-cf";
    }

    @Test
    void add() {
        var list = new RocksDBList(rocksDBConfig, "list-cf", "list1");
        list.add("one");
        list.add("two");
        list.add("three");

        assertEquals(3, list.size());
    }

    @Test
    void get() {
        var list = new RocksDBList(rocksDBConfig, "list-cf", "list1");
        list.add("one");
        list.add("two");
        list.add("three");
        list.add("four");

        assertEquals(4, list.size());
        assertThat(list.get(0)).isEqualTo("one");
        assertThat(list.get(1)).isEqualTo("two");
        assertThat(list.get(2)).isEqualTo("three");
        assertThat(list.get(3)).isEqualTo("four");
    }

}
