package com.bloxbean.cardano.yaci.store.common.cache;

public interface Cache<K, V> {

    void put(K key, V value);

    void putIfAbsent(K key, V value);

    V get(K key);

    boolean containsKey(K key);

    void remove(K key);

    long size();

    void clear();
}
