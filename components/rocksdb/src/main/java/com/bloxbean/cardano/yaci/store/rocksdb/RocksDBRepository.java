package com.bloxbean.cardano.yaci.store.rocksdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.msgpack.jackson.dataformat.MessagePackMapper;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.Optional;

@Slf4j
public class RocksDBRepository<V> implements KVRepository<String, V> {

    private RocksDB db;
    private ColumnFamilyHandle columnFamilyHandle;
    private ObjectMapper objectMapper;

    public RocksDBRepository(RocksDBConfig dbConfig, String columnFamily) {
        this.db = dbConfig.getRocksDB();
        this.columnFamilyHandle = dbConfig.getColumnFamilyHandle(columnFamily);

        this.objectMapper = new MessagePackMapper().handleBigDecimalAsString();

        if (this.columnFamilyHandle == null)
            throw new IllegalStateException("No ColumnFamily found : " + columnFamily);
    }

    @SneakyThrows
    @Override
    public boolean save(String key, V value) {
        if (log.isDebugEnabled())
            log.debug("saving value '{}' with key '{}'", value, key);
        try {
            db.put(columnFamilyHandle, key.getBytes(), objectMapper.writeValueAsBytes(value));
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
            byte[] bytes = db.get(columnFamilyHandle, key.getBytes());
            if (bytes != null)
                value = objectMapper.readValue(bytes, clazz);
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

    @Override
    public boolean delete(String key) {
        if (log.isDebugEnabled())
            log.debug("deleting key '{}'", key);
        try {
            db.delete(columnFamilyHandle, key.getBytes());
        } catch (RocksDBException e) {
            log.error("Error deleting entry, cause: '{}', message: '{}'", e.getCause(), e.getMessage());
            return false;
        }
        return true;
    }
}
