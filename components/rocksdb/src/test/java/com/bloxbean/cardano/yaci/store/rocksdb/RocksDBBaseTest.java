package com.bloxbean.cardano.yaci.store.rocksdb;

import com.bloxbean.cardano.yaci.store.rocksdb.config.RocksDBConfig;
import com.bloxbean.cardano.yaci.store.rocksdb.config.RocksDBProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;

public class RocksDBBaseTest {
    public static String tmpdir = System.getProperty("java.io.tmpdir");
    public RocksDBConfig rocksDBConfig;
    public RocksDBProperties rocksDBProperties;
    public String dbname = "rocksdb-testdb";

    @BeforeEach
    public void setup() {
        rocksDBProperties = new RocksDBProperties();
        rocksDBProperties.setRocksDBBaseDir(getDbDir());
        rocksDBProperties.setColumnFamilies(getColumnFamilies());

        rocksDBConfig = new RocksDBConfig(rocksDBProperties);
        rocksDBConfig.initDB();
    }

    @AfterEach
    public  void tearDown() {
        if (rocksDBConfig != null)
            rocksDBConfig.closeDB();

        File rocksDBFolder = new File(getDbDir());
        deleteDirectory(rocksDBFolder);
    }

    public String getDbDir() {
        return tmpdir + File.separator + dbname;
    }

    public String getColumnFamilies() {
        return "test1,test2";
    }

    public boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
