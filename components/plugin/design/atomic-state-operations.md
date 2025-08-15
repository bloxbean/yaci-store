# Atomic State Operations for Plugin Framework

## Overview

The enhanced State API provides thread-safe atomic operations for plugin developers using MVEL, JavaScript, or Python. This design addresses the need for safe concurrent access to shared state when multiple plugins run in parallel.

## API Reference

### Counter Operations

```java
// Increment by 1
long increment(K key)

// Increment by delta
long increment(K key, long delta)

// Decrement by 1
long decrement(K key)

// Decrement by delta
long decrement(K key, long delta)

// Add delta and return new value
long addAndGet(K key, long delta)

// Return old value and add delta
long getAndAdd(K key, long delta)
```

### Set Operations

```java
// Add element to set
boolean addToSet(K key, Object element)

// Remove element from set
boolean removeFromSet(K key, Object element)

// Check if element exists in set
boolean containsInSet(K key, Object element)

// Get immutable view of set
Set<Object> getSet(K key)

// Get size of set
long setSize(K key)
```

### Compute Operations

```java
// Update value based on current value
V compute(K key, BiFunction<K, V, V> remappingFunction)

// Merge new value with existing value
V merge(K key, V value, BiFunction<V, V, V> remappingFunction)
```

### Compare and Set Operations

```java
// Update only if current value matches expected
boolean compareAndSet(K key, V expectedValue, V newValue)

// Get old value and set new value atomically
V getAndSet(K key, V newValue)
```

## Usage Examples

### Example 1: Request Counter

```javascript
// MVEL plugin
count = global_state.increment("request_count")
if (count % 1000 == 0) {
    log.info("Processed " + count + " requests")
}
```

### Example 2: Unique User Tracking

```javascript
// JavaScript plugin
function trackUser(userId) {
    if (global_state.addToSet("daily_users", userId)) {
        // New user today
        global_state.increment("new_users_today")
    }
    return global_state.setSize("daily_users")
}
```

### Example 3: Rate Limiting

```python
# Python plugin
def check_rate_limit(client_id):
    key = f"requests_{client_id}"
    requests = global_state.increment(key)
    
    if requests > 100:
        raise Exception(f"Rate limit exceeded for {client_id}")
    
    return 100 - requests  # remaining requests
```

### Example 4: Distributed Lock

```javascript
// MVEL - Simple lock implementation
lockKey = "process_lock"
lockValue = Thread.currentThread().getName()

// Try to acquire lock
if (global_state.compareAndSet(lockKey, null, lockValue)) {
    try {
        // Critical section
        performCriticalWork()
    } finally {
        // Release lock
        global_state.compareAndSet(lockKey, lockValue, null)
    }
} else {
    log.warn("Could not acquire lock")
}
```

### Example 5: Accumulating Metrics

```javascript
// JavaScript - Accumulate values
function recordTransaction(amount) {
    // Add to total
    global_state.merge("total_volume", amount, (old, new) => {
        return (old || 0) + new
    })
    
    // Update maximum
    global_state.compute("max_transaction", (key, current) => {
        return Math.max(current || 0, amount)
    })
    
    // Count transactions
    return global_state.increment("transaction_count")
}
```

