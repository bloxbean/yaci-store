package com.bloxbean.cardano.yaci.store.rocksdb;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RocksDBConfig {
    private final RocksDBProperties rocksDBProperties;

    private final static String FILE_NAME = "db";

    private RocksDB db;
    private Map<String, ColumnFamilyHandle> columnFamilyHandles = new HashMap<>();

    private File baseDir;

    @PostConstruct
    public void initDB() {
        try {
            RocksDB.loadLibrary();

            List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>();
            List<ColumnFamilyHandle> cfHandles = new ArrayList<>();

            // Default column family
            cfDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));

            // Dynamic column families
            for (String cfName : rocksDBProperties.getColumnFamilyNames()) {
                cfDescriptors.add(new ColumnFamilyDescriptor(cfName.getBytes()));
            }

            baseDir = new File(rocksDBProperties.getRocksDBBaseDir(), FILE_NAME);
            Files.createDirectories(baseDir.getParentFile().toPath());
            Files.createDirectories(baseDir.getAbsoluteFile().toPath());

            try (final DBOptions options = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)) {
                db = RocksDB.open(options, baseDir.getAbsolutePath(), cfDescriptors, cfHandles);

                for (int i = 0; i < cfHandles.size(); i++) {
                    columnFamilyHandles.put(new String(cfDescriptors.get(i).getName()), cfHandles.get(i));
                }
            }
        } catch (IOException | RocksDBException e) {
            log.error("Error initializing RocksDB. Exception: '{}', message: '{}'", e.getCause(), e.getMessage(), e);
            throw new IllegalStateException("Error initializing RocksDB", e);
        }
    }

    @PreDestroy
    public void closeDB() {
        for (var columnFamilyHandle : columnFamilyHandles.values()) {
            if (columnFamilyHandle != null)
                columnFamilyHandle.close();
        }

        if (db != null) {
            db.close();
        }
    }

    public RocksDB getRocksDB() {
        return db;
    }

    public ColumnFamilyHandle getColumnFamilyHandle(String name) {
        return columnFamilyHandles.get(name);
    }
}
