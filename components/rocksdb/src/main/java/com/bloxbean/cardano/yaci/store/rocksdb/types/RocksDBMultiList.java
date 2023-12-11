package com.bloxbean.cardano.yaci.store.rocksdb.types;

import com.bloxbean.cardano.yaci.store.rocksdb.config.RocksDBConfig;
import com.bloxbean.cardano.yaci.store.rocksdb.serializer.Serializer;
import lombok.SneakyThrows;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;

public class RocksDBMultiList<T> {
    private final static String PREFIX = ":";
    private final RocksDB db;
    private final ColumnFamilyHandle columnFamilyHandle;
    private final Serializer keySerializer;
    private final Serializer valueSerializer;

    public RocksDBMultiList(RocksDBConfig rocksDBConfig, String columnFamily) {
        this.db = rocksDBConfig.getRocksDB();
        this.columnFamilyHandle = rocksDBConfig.getColumnFamilyHandle(columnFamily);
        this.keySerializer = rocksDBConfig.getKeySerializer();
        this.valueSerializer = rocksDBConfig.getValueSerializer();
    }

    @SneakyThrows
    public void add(String listName, T value) {
        long index = size(listName); // Get the current length of the list
        byte[] key = keySerializer.serialize(listName + PREFIX + index);
        db.put(columnFamilyHandle, key, valueSerializer.serialize(value));
        updateMetadata(listName, key); // Increment the length
    }

    @SneakyThrows
    public String get(String listName, long index) {
        byte[] value = db.get(columnFamilyHandle, (listName + PREFIX + index).getBytes());
        return value != null ? new String(value) : null;
    }

    @SneakyThrows
    public long size(String listName) {
        var metadataValueBytes = db.get(columnFamilyHandle, keySerializer.serialize(getMetadataKey(listName)));
        if (metadataValueBytes == null || metadataValueBytes.length == 0) {
            return 0;
        } else {
            var metadata = valueSerializer.deserialize(metadataValueBytes, ListMetadata.class);
            return metadata.getSize();
        }
    }

    @SneakyThrows
    private void updateMetadata(String listName, byte[] currentKey) {
        var metadataValueBytes = db.get(columnFamilyHandle, keySerializer.serialize(getMetadataKey(listName)));

        if (metadataValueBytes == null || metadataValueBytes.length == 0) {
            var metadata = new ListMetadata();
            metadata.setSize(1);
            metadata.setHead(currentKey);
            metadata.setTail(currentKey);
            byte[] metadataKeyName = keySerializer.serialize(getMetadataKey(listName));
            db.put(columnFamilyHandle, metadataKeyName, valueSerializer.serialize(metadata));
        } else {
            var metadata = valueSerializer.deserialize(metadataValueBytes, ListMetadata.class);
            metadata.setSize(metadata.getSize() + 1);
            metadata.setTail(currentKey);
            byte[] metadataKeyName = keySerializer.serialize(getMetadataKey(listName));
            db.put(columnFamilyHandle, metadataKeyName, valueSerializer.serialize(metadata));
        }
    }

    private String getMetadataKey(String name) {
        return name + "__metadata__";
    }
}

