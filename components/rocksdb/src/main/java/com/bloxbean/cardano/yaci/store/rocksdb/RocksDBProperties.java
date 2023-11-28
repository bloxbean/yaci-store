package com.bloxbean.cardano.yaci.store.rocksdb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RocksDBProperties {

    @Value("${store.rocksdb.column-families}")
    private String columnFamilies;

    @Value("${store.rocksdb.basedir:_rocksdb}")
    private String rocksDBBaseDir;

    public List<String> getColumnFamilyNames() {
        return Arrays.asList(columnFamilies.split(","));
    }

    public String getRocksDBBaseDir() {
        return rocksDBBaseDir;
    }
}
