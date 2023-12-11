package com.bloxbean.cardano.yaci.store.rocksdb.collection;

import com.bloxbean.cardano.yaci.store.rocksdb.RocksDBBaseTest;
import com.bloxbean.cardano.yaci.store.rocksdb.types.RocksDBMultiSet;
import org.junit.jupiter.api.Test;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class RocksDBMultiSetTest extends RocksDBBaseTest {

    @Override
    public String getColumnFamilies() {
        return "list-cf";
    }

    @Test
    void addAndContains() {
        RocksDBMultiSet rocksDBSet = new RocksDBMultiSet(rocksDBConfig, "list-cf");
        String list1 = "list1";
        rocksDBSet.add(list1, "one");
        rocksDBSet.add(list1, "two");
        rocksDBSet.add(list1, "one");
        rocksDBSet.add(list1, "nine");

        String list2 = "list2";
        rocksDBSet.add(list2, "aaa");
        rocksDBSet.add(list2, "bbb");
        rocksDBSet.add(list2, "ccc");
        rocksDBSet.add(list2, "ddd");

        assertTrue(rocksDBSet.contains(list1, "one"));
        assertTrue(rocksDBSet.contains(list1, "two"));
        assertFalse(rocksDBSet.contains(list1, "three"));
        assertFalse(rocksDBSet.contains(list1, "four"));
        assertTrue(rocksDBSet.contains(list1, "nine"));
        assertFalse(rocksDBSet.contains(list1, "aaa"));
        assertFalse(rocksDBSet.contains(list1, "bbb"));
        assertFalse(rocksDBSet.contains(list1, "ccc"));
        assertFalse(rocksDBSet.contains(list1, "ddd"));

        assertTrue(rocksDBSet.contains(list2, "aaa"));
        assertTrue(rocksDBSet.contains(list2, "bbb"));
        assertTrue(rocksDBSet.contains(list2, "ccc"));
        assertTrue(rocksDBSet.contains(list2, "ddd"));
        assertFalse(rocksDBSet.contains(list2, "one"));
        assertFalse(rocksDBSet.contains(list2, "two"));
        assertFalse(rocksDBSet.contains(list2, "nine"));
    }

    @Test
    void addAndContains_batch() throws Exception {
        RocksDBMultiSet rocksDBSet = new RocksDBMultiSet(rocksDBConfig, "list-cf");
        WriteBatch writeBatch = new WriteBatch();

        String list1 = "list1";
        rocksDBSet.add(writeBatch, list1, "one");
        rocksDBSet.add(writeBatch,list1, "two");
        rocksDBSet.add(writeBatch,list1, "one");
        rocksDBSet.add(writeBatch,list1, "nine");

        String list2 = "list2";
        rocksDBSet.add(writeBatch,list2, "aaa");
        rocksDBSet.add(writeBatch,list2, "bbb");
        rocksDBSet.add(writeBatch,list2, "ccc");
        rocksDBSet.add(writeBatch,list2, "ddd");

        rocksDBConfig.getRocksDB().write(new WriteOptions(), writeBatch);

        assertTrue(rocksDBSet.contains(list1, "one"));
        assertTrue(rocksDBSet.contains(list1, "two"));
        assertFalse(rocksDBSet.contains(list1, "three"));
        assertFalse(rocksDBSet.contains(list1, "four"));
        assertTrue(rocksDBSet.contains(list1, "nine"));
        assertFalse(rocksDBSet.contains(list1, "aaa"));
        assertFalse(rocksDBSet.contains(list1, "bbb"));
        assertFalse(rocksDBSet.contains(list1, "ccc"));
        assertFalse(rocksDBSet.contains(list1, "ddd"));

        assertTrue(rocksDBSet.contains(list2, "aaa"));
        assertTrue(rocksDBSet.contains(list2, "bbb"));
        assertTrue(rocksDBSet.contains(list2, "ccc"));
        assertTrue(rocksDBSet.contains(list2, "ddd"));
        assertFalse(rocksDBSet.contains(list2, "one"));
        assertFalse(rocksDBSet.contains(list2, "two"));
        assertFalse(rocksDBSet.contains(list2, "nine"));
    }

    @Test
    void remove() throws Exception {
        RocksDBMultiSet rocksDBSet = new RocksDBMultiSet(rocksDBConfig, "list-cf");
        WriteBatch writeBatch = new WriteBatch();

        String setName = "set1";
        rocksDBSet.add(writeBatch, setName, "one");
        rocksDBSet.add(writeBatch, setName, "two");
        rocksDBSet.add(writeBatch, setName, "one");
        rocksDBSet.add(writeBatch, setName, "nine");

        rocksDBSet.remove(writeBatch, setName, "one");

        rocksDBConfig.getRocksDB().write(new WriteOptions(), writeBatch);

        Set<String> members = rocksDBSet.members(setName);
        assertThat(members).hasSize(2);
        assertThat(members).contains("two", "nine");
    }

    @Test
    void remove_batch() throws Exception {
        RocksDBMultiSet rocksDBSet = new RocksDBMultiSet(rocksDBConfig, "list-cf");
        String setName = "set2";
        WriteBatch writeBatch = new WriteBatch();
        rocksDBSet.add(writeBatch, setName, "one");
        rocksDBSet.add(writeBatch, setName, "two");
        rocksDBSet.add(writeBatch, setName, "one");
        rocksDBSet.add(writeBatch, setName, "nine");

        rocksDBConfig.getRocksDB().write(new WriteOptions(), writeBatch);

        rocksDBSet.remove(setName, "one");

        Set<String> members = rocksDBSet.members(setName);
        assertThat(members).hasSize(2);
        assertThat(members).contains("two", "nine");
    }

    @Test
    void members() {
        RocksDBMultiSet rocksDBSet = new RocksDBMultiSet(rocksDBConfig, "list-cf");
        String setName = "set3";
        rocksDBSet.add(setName, "one");
        rocksDBSet.add(setName,"two");
        rocksDBSet.add(setName,"one");
        rocksDBSet.add(setName,"nine");

        Set<String> members = rocksDBSet.members(setName);
        assertEquals(3, members.size());
        assertThat(members).contains("one", "two", "nine");
    }
}
