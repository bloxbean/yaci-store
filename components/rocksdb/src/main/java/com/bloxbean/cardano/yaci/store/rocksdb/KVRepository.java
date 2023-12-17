package com.bloxbean.cardano.yaci.store.rocksdb;

import com.bloxbean.rocks.types.common.KeyValue;

import java.util.List;
import java.util.Optional;

public interface KVRepository<K, V> {
    boolean save(K key, V value);
    Optional<V> find(K key, Class<V> clazz);
    List<V> findMulti(List<K> keys, Class<V> clazz);
    boolean saveBatch(List<KeyValue<K, V>> elements);
    boolean delete(K key);
    boolean deleteMulti(List<String> keys);

}
