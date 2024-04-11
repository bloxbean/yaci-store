package com.bloxbean.cardano.yaci.store.utxo.cache;

import com.bloxbean.cardano.yaci.store.common.cache.Cache;

public class NoCache<K, V> implements Cache<K, V> {
    @Override
    public void put(K key, V value) {
        //do nothing
    }

    @Override
    public void putIfAbsent(K key, V value) {
        //do nothing
    }

    @Override
    public V get(K key) {
        return null;
    }

    @Override
    public boolean containsKey(K key) {
        return false;
    }

    @Override
    public void remove(K key) {
        //do nothing
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public void clear() {
        //do nothing
    }
}
