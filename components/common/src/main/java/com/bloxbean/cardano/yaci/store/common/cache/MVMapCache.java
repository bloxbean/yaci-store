package com.bloxbean.cardano.yaci.store.common.cache;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

public class MVMapCache<K, V> implements Cache<K, V> {

    private MVMap<K, V> map;
    public MVMapCache(MVStore mvStore, String name) {
        map = mvStore.openMap(name);
    }

    @Override
    public void put(K key, V value) {
        map.put(key, value);
    }

    @Override
    public void putIfAbsent(K key, V value) {
        map.putIfAbsent(key, value);
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    public void remove(Object key) {
        map.remove(key);
    }

    public long size() {
        return map.size();
    }

    public void clear() {
       map.clear();
    }
}
