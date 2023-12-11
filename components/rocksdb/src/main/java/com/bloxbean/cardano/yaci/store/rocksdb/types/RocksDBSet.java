package com.bloxbean.cardano.yaci.store.rocksdb.types;

import com.bloxbean.cardano.yaci.store.rocksdb.config.RocksDBConfig;
import com.bloxbean.cardano.yaci.store.rocksdb.serializer.Serializer;
import lombok.SneakyThrows;
import org.rocksdb.*;

import java.util.HashSet;
import java.util.Set;

public class RocksDBSet {
    private final static String PREFIX = ":";
    private final RocksDB db;
    private final String name;
    private final ColumnFamilyHandle columnFamilyHandle;
    private final Serializer keySerializer;
    private final Serializer valueSerializer;

    public RocksDBSet(RocksDBConfig rocksDBConfig, String columnFamily, String name) {
        this.db = rocksDBConfig.getRocksDB();
        this.name = name;
        this.columnFamilyHandle = rocksDBConfig.getColumnFamilyHandle(columnFamily);
        this.keySerializer = rocksDBConfig.getKeySerializer();
        this.valueSerializer = rocksDBConfig.getValueSerializer();
    }

    @SneakyThrows
    public void add(String value) {
        db.put(columnFamilyHandle, keySerializer.serialize(name + PREFIX + value), new byte[0]);
    }

    @SneakyThrows
    public void add(WriteBatch writeBatch, String value) {
        writeBatch.put(columnFamilyHandle, keySerializer.serialize(name + PREFIX + value), new byte[0]);
    }

    @SneakyThrows
    public boolean contains(String value) {
        byte[] val = db.get(columnFamilyHandle, keySerializer.serialize(name + PREFIX + value));
        return val != null;
    }

    @SneakyThrows
    public void remove(String value) {
        db.delete(columnFamilyHandle, keySerializer.serialize(name + PREFIX + value));
    }

    @SneakyThrows
    public void remove(WriteBatch writeBatch, String value) {
        writeBatch.delete(columnFamilyHandle, keySerializer.serialize(name + PREFIX + value));
    }

    @SneakyThrows
    public Set<String> members() {
        Set<String> members = new HashSet<>();
        String prefix = name + PREFIX;
        try (RocksIterator iterator = db.newIterator(columnFamilyHandle)) {
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

