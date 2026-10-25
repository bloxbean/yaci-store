package com.bloxbean.cardano.yaci.store.common.cache;

import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * A {@link Cache} in a column family of the process's {@link RocksDBCacheStore}, for data too large for memory.
 * Made with {@link RocksDBCacheStore#cache}. Entries do not survive a restart.
 */
public class RocksDBCache<K, V> implements Cache<K, V> {

    /** How keys and values are stored as bytes. */
    public interface Codec<T> {
        byte[] encode(T value);

        T decode(byte[] bytes);
    }

    public static final Codec<String> STRING = new Codec<>() {
        @Override
        public byte[] encode(String value) {
            return value.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String decode(byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    };

    private final RocksDBCacheStore store;
    private final ColumnFamilyHandle family;
    private final Codec<K> keyCodec;
    private final Codec<V> valueCodec;

    RocksDBCache(RocksDBCacheStore store, ColumnFamilyHandle family, Codec<K> keyCodec, Codec<V> valueCodec) {
        this.store = store;
        this.family = family;
        this.keyCodec = keyCodec;
        this.valueCodec = valueCodec;
    }

    @Override
    public void put(K key, V value) {
        try {
            store.db().put(family, store.writeOptions(), keyCodec.encode(key), valueCodec.encode(value));
        } catch (RocksDBException e) {
            throw new IllegalStateException("RocksDB cache put failed", e);
        }
    }

    /** Not atomic: a concurrent put of the same key can be overwritten. */
    @Override
    public void putIfAbsent(K key, V value) {
        if (get(key) == null)
            put(key, value);
    }

    /** Not atomic: a concurrent call for the same key can compute it too. */
    @Override
    public V computeIfAbsent(K key, Function<K, V> mappingFunction) {
        V value = get(key);
        if (value == null) {
            value = mappingFunction.apply(key);
            if (value != null)
                put(key, value);
        }
        return value;
    }

    @Override
    public V get(K key) {
        try {
            byte[] bytes = store.db().get(family, keyCodec.encode(key));
            return bytes != null ? valueCodec.decode(bytes) : null;
        } catch (RocksDBException e) {
            throw new IllegalStateException("RocksDB cache get failed", e);
        }
    }

    @Override
    public boolean containsKey(K key) {
        return get(key) != null;
    }

    @Override
    public void remove(K key) {
        try {
            store.db().delete(family, store.writeOptions(), keyCodec.encode(key));
        } catch (RocksDBException e) {
            throw new IllegalStateException("RocksDB cache remove failed", e);
        }
    }

    /** An estimate: RocksDB counts deleted keys until they are compacted away. */
    @Override
    public long size() {
        try {
            return store.db().getLongProperty(family, "rocksdb.estimate-num-keys");
        } catch (RocksDBException e) {
            throw new IllegalStateException("RocksDB cache size failed", e);
        }
    }

    @Override
    public void clear() {
        try (RocksIterator iterator = store.db().newIterator(family)) {
            iterator.seekToFirst();
            if (!iterator.isValid())
                return;
            byte[] first = iterator.key();
            iterator.seekToLast();
            byte[] last = iterator.key();
            store.db().deleteRange(family, store.writeOptions(), first, last);
            store.db().delete(family, store.writeOptions(), last);
        } catch (RocksDBException e) {
            throw new IllegalStateException("RocksDB cache clear failed", e);
        }
    }
}
