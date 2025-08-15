package com.bloxbean.cardano.yaci.store.plugin.cache;

import java.util.Set;
import java.util.function.BiFunction;
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
    
    // Atomic counter operations
    long increment(K key);
    
    long increment(K key, long delta);
    
    long decrement(K key);
    
    long decrement(K key, long delta);
    
    long addAndGet(K key, long delta);
    
    long getAndAdd(K key, long delta);
    
    // Atomic set operations
    boolean addToSet(K key, Object element);
    
    boolean removeFromSet(K key, Object element);
    
    boolean containsInSet(K key, Object element);
    
    Set<Object> getSet(K key);
    
    long setSize(K key);
    
    // Atomic compute operations
    V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);
    
    V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction);
    
    // Compare and set
    boolean compareAndSet(K key, V expectedValue, V newValue);
    
    V getAndSet(K key, V newValue);
}
