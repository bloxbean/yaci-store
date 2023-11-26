package com.bloxbean.cardano.yaci.store.rocksdb;

import java.util.Optional;

public interface KVRepository<K, V> {
    boolean save(K key, V value);
    Optional<V> find(K key, Class<V> clazz);
    boolean delete(K key);
}
