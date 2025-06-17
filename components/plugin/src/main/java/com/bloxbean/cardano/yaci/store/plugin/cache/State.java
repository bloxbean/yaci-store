package com.bloxbean.cardano.yaci.store.plugin.cache;

import java.util.function.Function;

public interface State<K, V> {

    void put(K key, V value);

    void putIfAbsent(K key, V value);

    V computeIfAbsent(K key, Function<K, V> mappingFunction);

    V get(K key);

    boolean containsKey(K key);

    void remove(K key);

    long size();

    void clear();
}
