package com.bloxbean.cardano.yaci.store.cache;

import com.bloxbean.cardano.yaci.store.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

public class GuavaCache<K, V> implements Cache<K, V> {
    private com.google.common.cache.Cache<K, V> cache;

    public GuavaCache(int maxSize, long expiryAfterAccessInMinutes) {
        com.google.common.cache.Cache<K, V> _cache = CacheBuilder.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(expiryAfterAccessInMinutes, TimeUnit.MINUTES)
                .build();
        this.cache = _cache;
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void putIfAbsent(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public boolean containsKey(K key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public void remove(K key) {
        cache.invalidate(key);
    }

    @Override
    public long size() {
        return cache.size();
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }
}
