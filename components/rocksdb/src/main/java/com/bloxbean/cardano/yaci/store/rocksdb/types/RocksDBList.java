package com.bloxbean.cardano.yaci.store.rocksdb.types;

import com.bloxbean.cardano.yaci.store.rocksdb.config.RocksDBConfig;
import com.bloxbean.cardano.yaci.store.rocksdb.serializer.Serializer;
import lombok.SneakyThrows;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;

public class RocksDBList<T> {
    private final static String PREFIX = ":";
    private final RocksDB db;
    private final String name;
    private final byte[] metadataKeyName;
    private final ColumnFamilyHandle columnFamilyHandle;
    private final Serializer keySerializer;
    private final Serializer valueSerializer;

    public RocksDBList(RocksDBConfig rocksDBConfig, String columnFamily, String name) {
        this.db = rocksDBConfig.getRocksDB();
        this.name = name;
        this.columnFamilyHandle = rocksDBConfig.getColumnFamilyHandle(columnFamily);
        this.keySerializer = rocksDBConfig.getKeySerializer();
        this.valueSerializer = rocksDBConfig.getValueSerializer();
        this.metadataKeyName = keySerializer.serialize(name + "__metadata__");
    }

    @SneakyThrows
    public void add(T value) {
        long index = size(); // Get the current length of the list
        byte[] key = keySerializer.serialize(name + PREFIX + index);
        db.put(columnFamilyHandle, key, valueSerializer.serialize(value));
        updateMetadata(key); // Increment the length
    }

    @SneakyThrows
    public String get(long index) {
        byte[] value = db.get(columnFamilyHandle, (name + PREFIX + index).getBytes());
        return value != null ? new String(value) : null;
    }

    @SneakyThrows
    public long size() {
        var metadataValueBytes = db.get(columnFamilyHandle, metadataKeyName);
        if (metadataValueBytes == null || metadataValueBytes.length == 0) {
            return 0;
        } else {
            var metadata = valueSerializer.deserialize(metadataValueBytes, ListMetadata.class);
            return metadata.getSize();
        }
    }

    @SneakyThrows
    private void updateMetadata(byte[] currentKey) {
        var metadataValueBytes = db.get(columnFamilyHandle, metadataKeyName);

        if (metadataValueBytes == null || metadataValueBytes.length == 0) {
            var metadata = new ListMetadata();
            metadata.setSize(1);
            metadata.setHead(currentKey);
            metadata.setTail(currentKey);
            db.put(columnFamilyHandle, metadataKeyName, valueSerializer.serialize(metadata));
        } else {
            var metadata = valueSerializer.deserialize(metadataValueBytes, ListMetadata.class);
            metadata.setSize(metadata.getSize() + 1);
            metadata.setTail(currentKey);
            db.put(columnFamilyHandle, metadataKeyName, valueSerializer.serialize(metadata));
        }
    }
}

