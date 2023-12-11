package com.bloxbean.cardano.yaci.store.rocksdb;

import com.bloxbean.cardano.yaci.store.rocksdb.common.IndexDef;
import com.bloxbean.cardano.yaci.store.rocksdb.common.IndexRecord;
import com.bloxbean.cardano.yaci.store.rocksdb.common.KeyValue;
import com.bloxbean.cardano.yaci.store.rocksdb.config.RocksDBConfig;
import com.bloxbean.cardano.yaci.store.rocksdb.serializer.Serializer;
import com.bloxbean.cardano.yaci.store.rocksdb.types.RocksDBMultiSet;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Slf4j
public class RocksDBRepository<V> implements KVRepository<String, V> {

    private RocksDB db;
    private ColumnFamilyHandle columnFamilyHandle;
    private ColumnFamilyHandle slotIndexColumnFamilyHandle;
    private List<IndexDef<V>> indexDefs;
    private RocksDBConfig dbConfig;
    private Serializer keySerializer;
    private Serializer valueSerializer;

    private static Map<String, ColumnFamilyHandle> indexColumnFamilyHandles = new ConcurrentHashMap<>();

    private Map<String, RocksDBMultiSet> indexMap = new ConcurrentHashMap<>();

    public RocksDBRepository(RocksDBConfig dbConfig, String columnFamily) {
        this(dbConfig, columnFamily, false);
    }

    @SneakyThrows
    public RocksDBRepository(RocksDBConfig dbConfig, String columnFamily, boolean enableSlotIndex) {
        String indexColHandler = columnFamily + "_slot";
        this.db = dbConfig.getRocksDB();
        this.columnFamilyHandle = dbConfig.getColumnFamilyHandle(columnFamily);
        this.slotIndexColumnFamilyHandle = dbConfig.getColumnFamilyHandle(indexColHandler);
        this.dbConfig = dbConfig;
        this.keySerializer = dbConfig.getKeySerializer();
        this.valueSerializer = dbConfig.getValueSerializer();

        if (this.columnFamilyHandle == null)
            this.db.createColumnFamily(new ColumnFamilyDescriptor(columnFamily.getBytes(StandardCharsets.UTF_8)));

        if (enableSlotIndex && this.slotIndexColumnFamilyHandle == null)
            this.db.createColumnFamily(new ColumnFamilyDescriptor(indexColHandler.getBytes(StandardCharsets.UTF_8)));

        if (this.slotIndexColumnFamilyHandle != null)
            indexColumnFamilyHandles.put(indexColHandler, this.slotIndexColumnFamilyHandle);
    }

    @SneakyThrows
    public RocksDBRepository<V> withIndex(String indexColumnFamily,
                                          Function<V, List<IndexRecord>> keyMapper) {
        var indexDef = new IndexDef<V>(indexColumnFamily,keyMapper);
        if (indexDefs == null)
            indexDefs = new ArrayList<>();

        indexDefs.add(indexDef);

        var indexColumnFamilyHandle = dbConfig.getColumnFamilyHandle(indexColumnFamily);
        if (indexColumnFamilyHandle != null) {
            indexColumnFamilyHandles.put(indexColumnFamily, indexColumnFamilyHandle);
        } else {
            indexColumnFamilyHandle = db.createColumnFamily(new ColumnFamilyDescriptor(indexColumnFamily.getBytes(StandardCharsets.UTF_8)));
            indexColumnFamilyHandles.put(indexColumnFamily, indexColumnFamilyHandle);
        }

        RocksDBMultiSet rocksDBMultiSet = new RocksDBMultiSet(dbConfig, indexDef.getIndexName());
        indexMap.put(indexDef.getIndexName(), rocksDBMultiSet);

        return this;
    }

    @SneakyThrows
    @Override
    public boolean save(String key, V value) {
        if (log.isDebugEnabled())
            log.debug("saving value '{}' with key '{}'", value, key);
        try (WriteBatch writeBatch = new WriteBatch()) {
            writeBatch.put(columnFamilyHandle, keySerializer.serialize(key), valueSerializer.serialize(value));
            createBatchIndexes(writeBatch, key, value);

            WriteOptions writeOptions = new WriteOptions();
            db.write(writeOptions, writeBatch);
        } catch (RocksDBException e) {
            log.error("Error saving entry. Cause: '{}', message: '{}'", e.getCause(), e.getMessage());
            return false;
        }
        return true;
    }

    @SneakyThrows
    @Override
    public Optional<V> find(String key, Class<V> clazz) {
        V value = null;
        try {
            byte[] bytes = db.get(columnFamilyHandle, keySerializer.serialize(key));
            if (bytes != null)
                value = valueSerializer.deserialize(bytes, clazz);
        } catch (RocksDBException e) {
            log.error(
                    "Error retrieving the entry with key: {}, cause: {}, message: {}",
                    key,
                    e.getCause(),
                    e.getMessage()
            );
        }

        if (log.isDebugEnabled())
            log.debug("finding key '{}' returns '{}'", key, value);
        return value != null ? Optional.of(value) : Optional.empty();
    }

    @SneakyThrows
    @Override
    public List<V> findMulti(List<String> keys, Class<V> clazz) {
        try {
            List<byte[]> keyBytes = keys.stream()
                    .map(key -> keySerializer.serialize(key))
                    .toList();

            var columnFamilies = keyBytes.stream()
                    .map(key -> columnFamilyHandle)
                    .toList();

            var values = db.multiGetAsList(columnFamilies, keyBytes);
            if (values == null || values.size() == 0)
                return Collections.emptyList();

            return values.stream()
                    .map(value -> {
                        if (value == null)
                            return null;
                        else
                            return valueSerializer.deserialize(value, clazz);
                    })
                    .toList();
        } catch (RocksDBException e) {
            log.error(
                    "Error retrieving the entries for multiple keys: {}, cause: {}, message: {}",
                    keys,
                    e.getCause(),
                    e.getMessage()
            );
        }

        return Collections.emptyList();
    }

    @SneakyThrows
    @Override
    public boolean saveBatch(List<KeyValue<String, V>> elements) {
        try (WriteBatch writeBatch = new WriteBatch()) {
            for (var element : elements) {
                writeBatch.put(columnFamilyHandle, keySerializer.serialize(element.getKey()), valueSerializer.serialize(element.getValue()));
            }

            createBatchIndexes(writeBatch, elements);

            var writeOption = new WriteOptions();
            db.write(writeOption, writeBatch);
            writeBatch.close();

            return true;
        } catch (RocksDBException e) {
            log.error(
                    "Error saving batch. cause: {}, message: {}",
                    e.getCause(),
                    e.getMessage()
            );
        }

        return false;
    }

    @Override
    public boolean delete(String key) {
        if (log.isDebugEnabled())
            log.debug("deleting key '{}'", key);
        try {
            db.delete(columnFamilyHandle, keySerializer.serialize(key));
        } catch (RocksDBException e) {
            log.error("Error deleting entry, cause: '{}', message: '{}'", e.getCause(), e.getMessage());
            return false;
        }
        return true;
    }

    @SneakyThrows
    @Override
    public boolean deleteMulti(List<String> keys) {
        if (log.isDebugEnabled())
            log.debug("deleting key '{}'", keys);
        try (var writeBatch = new WriteBatch()) {

            List<byte[]> keyBytes = keys.stream()
                    .map(key -> keySerializer.serialize(key)).toList();

            for (var key : keyBytes) {
                writeBatch.delete(columnFamilyHandle, key);
            }

            deleteBatchIndexes(writeBatch, keys);

            WriteOptions writeOptions = new WriteOptions();
            db.write(writeOptions, writeBatch);
        } catch (RocksDBException e) {
            log.error("Error deleting entry, cause: '{}', message: '{}'", e.getCause(), e.getMessage());
            return false;
        }
        return true;
    }


    @SneakyThrows
    public void createSlotIndex(long slot, List<String> pKeys) {
        String slotStr = String.valueOf(slot);
        try (var writeBatch = new WriteBatch()) {
            writeBatch.put(slotIndexColumnFamilyHandle, keySerializer.serialize(slotStr), valueSerializer.serialize(pKeys));
            WriteOptions writeOptions = new WriteOptions();
            db.write(writeOptions, writeBatch);
        } catch (RocksDBException e) {
            log.error("Error creating index for key: {}, value: {}, cause: {}, message: {}", pKeys, slotStr, e.getCause(), e.getMessage());
        }
    }

    @SneakyThrows
    public List<KeyValue> findElements(String keyPrefix, int count, Class<V> clazz) {
        try (var iterator = db.newIterator(columnFamilyHandle)) {
            iterator.seek(keySerializer.serialize(keyPrefix));

            List<KeyValue> result = new ArrayList<>();
            int counter = 0;
            while (iterator.isValid()) {
                if (counter >= count)
                    break;

                iterator.next();

                String key = keySerializer.deserialize(iterator.key(), String.class);
                if (!key.startsWith(keyPrefix))
                    break;

                if (iterator.value() == null || iterator.value().length == 0)
                    result.add(new KeyValue(key, null));
                else {
                    var value = valueSerializer.deserialize(iterator.value(), clazz);
                    result.add(new KeyValue(key, value));
                }
                counter++;
            }

            return result;
        }
    }

    @SneakyThrows
    public <T> List<KeyValue> findByIndex(String indexName, String indexKeyPrefix,
                                          String cursorKey, int count, Class<T> clazz) { //TODO -- Add count
        var columnHandler = indexColumnFamilyHandles.get(indexName);

        var indexMultiSet = indexMap.get(indexName);
        if (indexMultiSet == null)
            throw new IllegalStateException("Index not found for name: " + indexName);

        Set<String> secondaryKeys = indexMultiSet.members(indexKeyPrefix);
        if (secondaryKeys == null || secondaryKeys.size() == 0)
            return Collections.emptyList();

        return secondaryKeys.stream()
                .map(key -> new KeyValue(key, null ))
                .toList();
    }

    public <T> List<KeyValue> findAllByIndex(String indexName, String indexKeyPrefix, Class<T> clazz) {
        return findByIndex(indexName, indexKeyPrefix, null, -1, clazz);
    }

    public void deleteIndex1(String indexName, List<String> indexKeys) {
        var columnHandler = indexColumnFamilyHandles.get(indexName);
        try (var writeBatch = new WriteBatch()) {
            for (var indexKey : indexKeys) {
                writeBatch.delete(columnHandler, keySerializer.serialize(indexKey));
            }

            WriteOptions writeOptions = new WriteOptions();
            db.write(writeOptions, writeBatch);
        } catch (RocksDBException e) {
            log.error("Error deleting index for key: {}, cause: {}, message: {}", indexKeys, e.getCause(), e.getMessage());
        }
    }

    public void deleteIndex(String indexName, List<IndexRecord> indexRecords) {
        var indexSet = indexMap.get(indexName);

        try (var writeBatch = new WriteBatch()) {
            for (var indexRecord : indexRecords) {
                indexSet.remove(writeBatch, indexRecord.getPartKey(), indexRecord.getSecondaryKey());
                //writeBatch.delete(columnHandler, keySerializer.serialize(indexKey));
            }

            WriteOptions writeOptions = new WriteOptions();
            db.write(writeOptions, writeBatch);
        } catch (RocksDBException e) {
            log.error("Error", e);
            log.error("Error deleting index {}", indexName);
        }
    }

    @SneakyThrows
    private void createBatchIndexes(WriteBatch writeBatch, String key, V value) {
        if (indexDefs == null || indexDefs.size() == 0)
            return;

        for (var indexDef : indexDefs) {
            List<IndexRecord> indexRecords = indexDef.getKeyMapper().apply(value);
            var indexSet = indexMap.get(indexDef.getIndexName());

            try {
                for (var indexRecord: indexRecords) {
                    indexSet.add(writeBatch, indexRecord.getPartKey(), indexRecord.getSecondaryKey());
                }
            } catch (Exception e) {
                log.error("Error creating index : {}", indexRecords);
            }
        }
    }

    @SneakyThrows
    private void createBatchIndexes(WriteBatch writeBatch, List<KeyValue<String, V>> elements) {
        if (indexDefs == null || indexDefs.size() == 0)
            return;

        for (var indexDef : indexDefs) {

            var indexSet = indexMap.get(indexDef.getIndexName());
            for (var element : elements) {
                List<IndexRecord> indexRecords = indexDef.getKeyMapper().apply(element.getValue());

                try {
                    for (var indexRecord: indexRecords) {
                        indexSet.add(writeBatch, indexRecord.getPartKey(), indexRecord.getSecondaryKey());
                    }

                } catch (Exception e) {
                    log.error("Error creating index : {}", indexRecords);
                }
            }
        }
    }

    @SneakyThrows
    private void deleteBatchIndexes(WriteBatch writeBatch, List<String> keys) {
        if (indexDefs == null || indexDefs.size() == 0)
            return;

        for (var indexDef : indexDefs) {
            var indexColFamilyHandle = indexColumnFamilyHandles.get(indexDef.getIndexName());

            for (var key : keys) {
                writeBatch.delete(indexColFamilyHandle, keySerializer.serialize(key));
            }
        }
    }
}
