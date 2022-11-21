package com.bloxbean.cardano.yaci.indexer.repository;

import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Repository;
import org.springframework.util.SerializationUtils;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

@Slf4j
@Repository
public class RocksDBRepository implements KVRepository<String, Object> {
    private final static String FILE_NAME = "spring-boot-db";
    File baseDir;
    RocksDB db;  @PostConstruct // execute after the application starts.

    void initialize() {
        RocksDB.loadLibrary();
        final Options options = new Options();
        options.setCreateIfMissing(true);
        baseDir = new File("/tmp/rocks", FILE_NAME);    try {
            Files.createDirectories(baseDir.getParentFile().toPath());
            Files.createDirectories(baseDir.getAbsoluteFile().toPath());
            db = RocksDB.open(options, baseDir.getAbsolutePath());      log.info("RocksDB initialized");
        } catch(IOException | RocksDBException e) {
            log.error("Error initializng RocksDB. Exception: '{}', message: '{}'", e.getCause(), e.getMessage(), e);
        }
    }  @Override
    public synchronized boolean save(String key, Object value) {
//        log.info("saving value '{}' with key '{}'", value, key);
        try {
            db.put(key.getBytes(), SerializationUtils.serialize(value));
        } catch (RocksDBException e) {
            log.error("Error saving entry. Cause: '{}', message: '{}'", e.getCause(), e.getMessage());      return false;
        }    return true;
    }  @Override
    public synchronized Optional<Object> find(String key) {
        Object value = null;    try {
            byte[] bytes = db.get(key.getBytes());
            if (bytes != null) value = SerializationUtils.deserialize(bytes);
        } catch (RocksDBException e) {
            log.error(
                    "Error retrieving the entry with key: {}, cause: {}, message: {}",
                    key,
                    e.getCause(),
                    e.getMessage()
            );
        }    log.info("finding key '{}' returns '{}'", key, value);    return value != null ? Optional.of(value) : Optional.empty();
    }  @Override
    public synchronized boolean delete(String key) {
//        log.info("deleting key '{}'", key);
        try {
            db.delete(key.getBytes());
        } catch (RocksDBException e) {
            log.error("Error deleting entry, cause: '{}', message: '{}'", e.getCause(), e.getMessage());      return false;
        }    return true;
    }
}
