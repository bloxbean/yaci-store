package com.bloxbean.cardano.yaci.store.common.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RocksDBCacheTest {

    @TempDir
    Path tmp;

    @Test
    void putGetRemoveAndClear() {
        try (RocksDBCacheStore store = new RocksDBCacheStore(tmp.resolve("cache"))) {
            RocksDBCache<String, String> cache = store.cache("utxo_owner_addr", RocksDBCache.STRING, RocksDBCache.STRING);

            cache.put("tx1#0", "addr1");
            cache.putIfAbsent("tx1#0", "other");
            cache.put("tx2#1", "addr2");
            assertThat(cache.get("tx1#0")).isEqualTo("addr1");
            assertThat(cache.containsKey("tx2#1")).isTrue();
            assertThat(cache.computeIfAbsent("tx3#0", key -> "addr3")).isEqualTo("addr3");

            cache.remove("tx1#0");
            assertThat(cache.get("tx1#0")).isNull();

            cache.clear();
            assertThat(cache.get("tx2#1")).isNull();
            assertThat(cache.get("tx3#0")).isNull();
        }
    }

    @Test
    void eachCacheIsItsOwnColumnFamily() {
        try (RocksDBCacheStore store = new RocksDBCacheStore(tmp.resolve("cache"))) {
            RocksDBCache<String, String> one = store.cache("one", RocksDBCache.STRING, RocksDBCache.STRING);
            RocksDBCache<String, String> two = store.cache("two", RocksDBCache.STRING, RocksDBCache.STRING);

            one.put("key", "1");
            two.put("key", "2");
            one.clear();

            assertThat(one.get("key")).isNull();
            assertThat(two.get("key")).isEqualTo("2");
        }
    }

    @Test
    void aStoreOpensEmptyEvenWhenTheFolderHasAnOldCache() throws Exception {
        Path folder = tmp.resolve("cache");
        try (RocksDBCacheStore store = new RocksDBCacheStore(folder)) {
            store.cache("utxo_owner_addr", RocksDBCache.STRING, RocksDBCache.STRING).put("tx1#0", "addr1");
        }
        Files.writeString(folder.resolve("stray-file"), "left from an earlier run");

        try (RocksDBCacheStore store = new RocksDBCacheStore(folder)) {
            assertThat(store.cache("utxo_owner_addr", RocksDBCache.STRING, RocksDBCache.STRING).get("tx1#0")).isNull();
            assertThat(folder.resolve("stray-file")).doesNotExist();
        }
    }

    @Test
    void aCacheFailsInJavaOnceItsStoreIsClosed() {
        RocksDBCacheStore store = new RocksDBCacheStore(tmp.resolve("cache"));
        RocksDBCache<String, String> cache = store.cache("utxo_owner_addr", RocksDBCache.STRING, RocksDBCache.STRING);
        cache.put("tx1#0", "addr1");
        store.close();
        store.close();

        //A RocksDB call on the closed handles would crash the JVM, as the sync thread did at a shutdown
        assertThatThrownBy(() -> cache.get("tx1#0")).isInstanceOf(IllegalStateException.class).hasMessageContaining("closed");
        assertThatThrownBy(() -> cache.put("tx2#0", "addr2")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(cache::clear).isInstanceOf(IllegalStateException.class);
    }
}
