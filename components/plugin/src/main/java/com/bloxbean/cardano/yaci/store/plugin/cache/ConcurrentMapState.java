package com.bloxbean.cardano.yaci.store.plugin.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class ConcurrentMapState<K, V> implements State<K, V> {
    private final ConcurrentHashMap<K, V> state;

    public ConcurrentMapState() {
        this.state = new ConcurrentHashMap<>();
    }

    @Override
    public void put(K key, V value) {
        state.put(key, value);
    }

    @Override
    public void putIfAbsent(K key, V value) {
        state.putIfAbsent(key, value);
    }

    @Override
    public V computeIfAbsent(K key, Function<K, V> mappingFunction) {
        return state.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V get(K key) {
        return state.get(key);
    }

    @Override
    public boolean containsKey(K key) {
        return state.containsKey(key);
    }

    @Override
    public void remove(K key) {
        state.remove(key);
    }

    @Override
    public long size() {
        return state.size();
    }

    @Override
    public void clear() {
        state.clear();
    }
}
