package com.bloxbean.cardano.yaci.store.plugin.cache;

import com.bloxbean.cardano.yaci.store.common.cache.Cache;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ConcurrentMapCache<K, V> implements Cache<K, V> {
    private final ConcurrentHashMap<K, V> cache;

    public ConcurrentMapCache() {
        this.cache = new ConcurrentHashMap<>();
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void putIfAbsent(K key, V value) {
        cache.putIfAbsent(key, value);
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> mappingFunction) {
        return cache.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V get(K key) {
        return cache.get(key);
    }

    @Override
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    @Override
    public void remove(K key) {
        cache.remove(key);
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
