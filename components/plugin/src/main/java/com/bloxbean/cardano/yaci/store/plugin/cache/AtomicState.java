package com.bloxbean.cardano.yaci.store.plugin.cache;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Thread-safe implementation of State with atomic operations support.
 * Uses a single ConcurrentHashMap with intelligent value handling for different types.
 * Automatically wraps numeric values in AtomicLong when atomic operations are used.
 */
public class AtomicState<K, V> implements State<K, V> {
    
    private final ConcurrentHashMap<K, Object> state;
    
    public AtomicState() {
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
        Object result = state.computeIfAbsent(key, k -> mappingFunction.apply(k));
        return unwrapValue(result);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public V get(K key) {
        Object value = state.get(key);
        return unwrapValue(value);
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
    
    // Atomic counter operations
    
    @Override
    public long increment(K key) {
        return addAndGet(key, 1);
    }
    
    @Override
    public long increment(K key, long delta) {
        return addAndGet(key, delta);
    }
    
    @Override
    public long decrement(K key) {
        return addAndGet(key, -1);
    }
    
    @Override
    public long decrement(K key, long delta) {
        return addAndGet(key, -delta);
    }
    
    @Override
    public long addAndGet(K key, long delta) {
        Object result = state.compute(key, (k, currentValue) -> {
            if (currentValue == null) {
                return new AtomicLong(delta);
            } else if (currentValue instanceof AtomicLong) {
                ((AtomicLong) currentValue).addAndGet(delta);
                return currentValue;
            } else if (currentValue instanceof Number) {
                // Convert regular number to AtomicLong for thread safety
                long val = ((Number) currentValue).longValue() + delta;
                return new AtomicLong(val);
            } else {
                // Try to parse as number, default to delta if not possible
                try {
                    long val = Long.parseLong(currentValue.toString()) + delta;
                    return new AtomicLong(val);
                } catch (NumberFormatException e) {
                    return new AtomicLong(delta);
                }
            }
        });
        
        return ((AtomicLong) result).get();
    }
    
    @Override
    public long getAndAdd(K key, long delta) {
        AtomicLong oldValueHolder = new AtomicLong(0);
        
        state.compute(key, (k, currentValue) -> {
            if (currentValue == null) {
                return new AtomicLong(delta);
            } else if (currentValue instanceof AtomicLong) {
                oldValueHolder.set(((AtomicLong) currentValue).getAndAdd(delta));
                return currentValue;
            } else if (currentValue instanceof Number) {
                long val = ((Number) currentValue).longValue();
                oldValueHolder.set(val);
                return new AtomicLong(val + delta);
            } else {
                try {
                    long val = Long.parseLong(currentValue.toString());
                    oldValueHolder.set(val);
                    return new AtomicLong(val + delta);
                } catch (NumberFormatException e) {
                    return new AtomicLong(delta);
                }
            }
        });
        
        return oldValueHolder.get();
    }
    
    // Atomic set operations
    
    @Override
    public boolean addToSet(K key, Object element) {
        AtomicReference<Boolean> wasAdded = new AtomicReference<>(false);
        
        state.compute(key, (k, currentValue) -> {
            Set<Object> set;
            if (currentValue == null) {
                set = ConcurrentHashMap.newKeySet();
                wasAdded.set(set.add(element));
            } else if (currentValue instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<Object> existing = (Set<Object>) currentValue;
                set = existing;
                wasAdded.set(set.add(element));
            } else {
                // Convert non-set value to a set containing that value
                set = ConcurrentHashMap.newKeySet();
                set.add(currentValue);
                wasAdded.set(set.add(element));
            }
            return set;
        });
        
        return wasAdded.get();
    }
    
    @Override
    public boolean removeFromSet(K key, Object element) {
        AtomicReference<Boolean> removed = new AtomicReference<>(false);
        
        state.computeIfPresent(key, (k, currentValue) -> {
            if (currentValue instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<Object> set = (Set<Object>) currentValue;
                removed.set(set.remove(element));
                return set.isEmpty() ? null : set;
            }
            return currentValue;
        });
        
        return removed.get();
    }
    
    @Override
    public boolean containsInSet(K key, Object element) {
        Object value = state.get(key);
        if (value instanceof Set) {
            return ((Set<?>) value).contains(element);
        }
        return false;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Set<Object> getSet(K key) {
        Object value = state.get(key);
        if (value instanceof Set) {
            return Collections.unmodifiableSet((Set<Object>) value);
        }
        return Collections.emptySet();
    }
    
    @Override
    public long setSize(K key) {
        Object value = state.get(key);
        if (value instanceof Set) {
            return ((Set<?>) value).size();
        }
        return 0;
    }
    
    // Atomic compute operations
    
    @Override
    @SuppressWarnings("unchecked")
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Object result = state.compute(key, (k, v) -> {
            V unwrapped = unwrapValue(v);
            V computed = remappingFunction.apply(k, unwrapped);
            return wrapValueIfNeeded(computed);
        });
        return unwrapValue(result);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Object result = state.merge(key, wrapValueIfNeeded(value), (oldVal, newVal) -> {
            V oldUnwrapped = unwrapValue(oldVal);
            V newUnwrapped = unwrapValue(newVal);
            V merged = remappingFunction.apply(oldUnwrapped, newUnwrapped);
            return wrapValueIfNeeded(merged);
        });
        return unwrapValue(result);
    }
    
    // Compare and set operations
    
    @Override
    public boolean compareAndSet(K key, V expectedValue, V newValue) {
        Object expected = wrapValueIfNeeded(expectedValue);
        Object newVal = wrapValueIfNeeded(newValue);
        
        if (expectedValue == null) {
            return state.putIfAbsent(key, newVal) == null;
        }
        
        // Handle atomic values specially
        if (expected instanceof Number && !(expected instanceof AtomicLong)) {
            // Need to handle numeric comparison carefully
            AtomicReference<Boolean> success = new AtomicReference<>(false);
            state.compute(key, (k, currentValue) -> {
                if (currentValue instanceof AtomicLong) {
                    if (((AtomicLong) currentValue).get() == ((Number) expected).longValue()) {
                        success.set(true);
                        return newVal;
                    }
                } else if (currentValue instanceof Number) {
                    if (((Number) currentValue).longValue() == ((Number) expected).longValue()) {
                        success.set(true);
                        return newVal;
                    }
                } else if (expected.equals(currentValue)) {
                    success.set(true);
                    return newVal;
                }
                return currentValue;
            });
            return success.get();
        }
        
        return state.replace(key, expected, newVal);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public V getAndSet(K key, V newValue) {
        Object oldValue = state.put(key, wrapValueIfNeeded(newValue));
        return unwrapValue(oldValue);
    }
    
    // Helper methods
    
    @SuppressWarnings("unchecked")
    private V unwrapValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof AtomicLong) {
            return (V) Long.valueOf(((AtomicLong) value).get());
        }
        if (value instanceof AtomicReference) {
            return (V) ((AtomicReference<?>) value).get();
        }
        return (V) value;
    }
    
    private Object wrapValueIfNeeded(V value) {
        // For now, we don't auto-wrap on put, only on atomic operations
        // This preserves the original behavior for non-atomic usage
        return value;
    }
}