package com.bloxbean.cardano.yaci.store.rocksdb.collection;

import com.bloxbean.cardano.yaci.store.rocksdb.RocksDBBaseTest;
import com.bloxbean.cardano.yaci.store.rocksdb.types.RocksDBSet;
import org.junit.jupiter.api.Test;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RocksDBSetTest extends RocksDBBaseTest {

    @Override
    public String getColumnFamilies() {
        return "list1";
    }

    @Test
    void addAndContains() {
        RocksDBSet rocksDBSet = new RocksDBSet(rocksDBConfig, "list1", "set1");
        rocksDBSet.add("one");
        rocksDBSet.add("two");
        rocksDBSet.add("one");
        rocksDBSet.add("nine");

        assertTrue(rocksDBSet.contains("one"));
        assertTrue(rocksDBSet.contains("two"));
        assertFalse(rocksDBSet.contains("three"));
        assertFalse(rocksDBSet.contains("four"));
        assertTrue(rocksDBSet.contains("nine"));
    }

    @Test
    void addAndContains_batch() throws Exception {
        RocksDBSet rocksDBSet = new RocksDBSet(rocksDBConfig, "list1", "set1");
        WriteBatch writeBatch = new WriteBatch();
        rocksDBSet.add(writeBatch, "one");
        rocksDBSet.add(writeBatch, "two");
        rocksDBSet.add(writeBatch, "one");
        rocksDBSet.add(writeBatch, "nine");

        rocksDBConfig.getRocksDB().write(new WriteOptions(), writeBatch);

        assertTrue(rocksDBSet.contains("one"));
        assertTrue(rocksDBSet.contains("two"));
        assertFalse(rocksDBSet.contains("three"));
        assertFalse(rocksDBSet.contains("four"));
        assertTrue(rocksDBSet.contains("nine"));
    }

    @Test
    void remove() {
        RocksDBSet rocksDBSet = new RocksDBSet(rocksDBConfig, "list1", "set1");
        rocksDBSet.add("one");
        rocksDBSet.add("two");
        rocksDBSet.add("one");
        rocksDBSet.add("nine");

        rocksDBSet.remove("one");

        Set<String> members = rocksDBSet.members();
        assertThat(members).hasSize(2);
        assertThat(members).contains("two", "nine");
    }

    @Test
    void remove_batch() throws Exception {
        RocksDBSet rocksDBSet = new RocksDBSet(rocksDBConfig, "list1", "set1");
        WriteBatch writeBatch = new WriteBatch();
        rocksDBSet.add(writeBatch, "one");
        rocksDBSet.add(writeBatch, "two");
        rocksDBSet.add(writeBatch, "one");
        rocksDBSet.add(writeBatch, "nine");

        rocksDBConfig.getRocksDB().write(new WriteOptions(), writeBatch);

        rocksDBSet.remove("one");

        Set<String> members = rocksDBSet.members();
        assertThat(members).hasSize(2);
        assertThat(members).contains("two", "nine");
    }

    @Test
    void members() {
        RocksDBSet rocksDBSet = new RocksDBSet(rocksDBConfig, "list1", "set1");
        rocksDBSet.add("one");
        rocksDBSet.add("two");
        rocksDBSet.add("one");
        rocksDBSet.add("nine");

        Set<String> members = rocksDBSet.members();
        assertEquals(3, members.size());
        assertThat(members).contains("one", "two", "nine");
    }
}
