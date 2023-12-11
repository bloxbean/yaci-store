package com.bloxbean.cardano.yaci.store.rocksdb.types;

import com.bloxbean.cardano.yaci.store.rocksdb.config.RocksDBConfig;
import com.bloxbean.cardano.yaci.store.rocksdb.serializer.Serializer;
import lombok.SneakyThrows;
import org.rocksdb.*;

import java.util.HashSet;
import java.util.Set;

public class RocksDBMultiSet {
    private final static String PREFIX = ":";
    private final RocksDB db;
    private final ColumnFamilyHandle columnFamilyHandle;
    private final Serializer keySerializer;
    private final Serializer valueSerializer;

    public RocksDBMultiSet(RocksDBConfig rocksDBConfig, String columnFamily) {
        this.db = rocksDBConfig.getRocksDB();
        this.columnFamilyHandle = rocksDBConfig.getColumnFamilyHandle(columnFamily);
        this.keySerializer = rocksDBConfig.getKeySerializer();
        this.valueSerializer = rocksDBConfig.getValueSerializer();
    }

    @SneakyThrows
    public void add(String listName, String value) {
        db.put(columnFamilyHandle, keySerializer.serialize(listName + PREFIX + value), new byte[0]);
    }

    @SneakyThrows
    public void add(WriteBatch writeBatch, String listName, String value) {
        writeBatch.put(columnFamilyHandle, keySerializer.serialize(listName + PREFIX + value), new byte[0]);
    }

    @SneakyThrows
    public boolean contains(String listName, String value) {
        byte[] val = db.get(columnFamilyHandle, keySerializer.serialize(listName + PREFIX + value));
        return val != null;
    }

    @SneakyThrows
    public void remove(String listName, String value) {
        db.delete(columnFamilyHandle, keySerializer.serialize(listName + PREFIX + value));
    }

    @SneakyThrows
    public void remove(WriteBatch writeBatch, String listName, String value) {
        writeBatch.delete(columnFamilyHandle, keySerializer.serialize(listName + PREFIX + value));
    }

    @SneakyThrows
    public Set<String> members(String listName) {
        Set<String> members = new HashSet<>();
        String prefix = listName + PREFIX;
        try (RocksIterator iterator = db.newIterator(columnFamilyHandle, new ReadOptions().setAutoPrefixMode(true))) {
            for (iterator.seek(keySerializer.serialize(prefix)); iterator.isValid(); iterator.next()) {
                String key = new String(iterator.key());
                if (!key.startsWith(prefix)) {
                    break; // Break if the key no longer starts with the prefix
                }
                String member = key.substring(prefix.length());
                members.add(member);
            }
        }
        return members;
    }
}

