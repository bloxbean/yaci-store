# Plugin Locker Usage Guide

## Overview

The enhanced `Locker` provides thread-safe synchronization utilities designed specifically for plugin developers using MVEL, JavaScript, or Python. It offers simple, intuitive methods for coordinating access to shared resources and ensuring thread safety in concurrent plugin environments.

## Key Features

- **Script-Friendly API**: Designed for MVEL, JavaScript, and Python developers
- **Named Locks**: Easy coordination using string-based lock names
- **Timeout Support**: Prevent deadlocks with configurable timeouts
- **Try-Lock Operations**: Non-blocking lock attempts
- **Lock State Queries**: Debugging and monitoring capabilities

## Basic Usage Examples

### Simple Synchronization

```javascript
// MVEL - Global lock for simple synchronization
result = locker.withGlobalLock(() -> {
    // Only one plugin can execute this code at a time
    return updateGlobalCounter()
})
```

```javascript
// JavaScript - Global lock
const result = locker.withGlobalLock(() => {
    // Thread-safe operation
    return processSharedResource();
});
```

```python
# Python - Global lock
result = locker.withGlobalLock(lambda: process_shared_data())
```

### Named Locks for Coordination

```javascript
// MVEL - Named locks for specific resource coordination
result = locker.withLock("database_cleanup", () -> {
    // Only one plugin can perform database cleanup at a time
    log.info("Starting database cleanup")
    cleanupOldRecords()
    return "cleanup completed"
})
```

```javascript
// JavaScript - Named locks
const result = locker.withLock("transaction_processing", () => {
    // Coordinate transaction processing between plugins
    return processTransactions(blockData);
});
```

```python
# Python - Named locks for resource coordination
result = locker.withLock("state_update", lambda: {
    # Ensure atomic state updates
    update_plugin_state()
    return "state updated"
})
```

## Advanced Usage

### Timeout Operations

```javascript
// MVEL - Try lock with timeout (returns null if timeout)
result = locker.tryWithLock("external_api_calls", () -> {
    return callExternalAPI()
}, 30)  // 30 second timeout

if (result == null) {
    log.warn("Failed to acquire lock within timeout")
} else {
    log.info("API call result: " + result)
}
```

```javascript
// JavaScript - Timeout with exception handling
try {
    const result = locker.withLockTimeout("critical_section", () => {
        return performCriticalOperation();
    }, 10); // 10 second timeout, throws exception if timeout
    
    console.log("Operation completed:", result);
} catch (error) {
    console.error("Lock timeout:", error.message);
}
```

```python
# Python - Non-blocking try lock
result = locker.tryWithLock("background_processing", lambda: {
    return process_background_tasks()
}, 5)  # 5 second timeout

if result is None:
    print("Could not acquire lock, skipping background processing")
else:
    print(f"Background processing result: {result}")
```

### Operations Without Return Values

```javascript
// MVEL - Void operations (no return value)
locker.withLockVoid("log_rotation", () -> {
    rotateLogFiles()
    cleanupOldLogs()
    log.info("Log rotation completed")
})
```

```javascript
// JavaScript - Void operations with timeout
const success = locker.tryWithLockVoid("system_maintenance", () => {
    performSystemMaintenance();
    updateSystemStatus();
}, 60); // 60 second timeout

if (!success) {
    console.warn("Maintenance skipped due to lock timeout");
}
```

```python
# Python - Simple void operations
locker.withGlobalLockVoid(lambda: {
    update_metrics()
    send_heartbeat()
})
```

## Common Use Cases

### 1. Database Operations Coordination

```javascript
// MVEL - Ensure only one plugin performs database migrations
migrationResult = locker.withLock("database_migration", () -> {
    if (needsMigration()) {
        log.info("Running database migration")
        runMigration()
        return "migration completed"
    } else {
        return "no migration needed"
    }
})
```

```javascript
// JavaScript - Coordinate database cleanup
const cleaned = locker.withLock("database_cleanup", () => {
    const oldRecords = findOldRecords();
    if (oldRecords.length > 0) {
        deleteRecords(oldRecords);
        return oldRecords.length;
    }
    return 0;
});

console.log(`Cleaned up ${cleaned} old records`);
```

### 2. External API Rate Limiting

```javascript
// MVEL - Rate limit external API calls
response = locker.tryWithLock("external_api_rate_limit", () -> {
    return http.get("https://api.example.com/data")
}, 1)  // 1 second timeout for rate limiting

if (response == null) {
    log.warn("API rate limit exceeded, skipping call")
} else if (response.isSuccess()) {
    data = response.asJson()
    processAPIData(data)
}
```

```python
# Python - Coordinate API calls across plugins
def safe_api_call():
    return locker.withLock("api_coordination", lambda: {
        response = http.get("https://api.example.com/critical-data")
        if response.isSuccess():
            return response.asJson()
        else:
            raise Exception(f"API call failed: {response.getStatus()}")
    })

try:
    data = safe_api_call()
    process_critical_data(data)
except Exception as e:
    print(f"API call failed: {e}")
```

### 3. State Management Coordination

```javascript
// JavaScript - Coordinate global state updates
const updateResult = locker.withLock("global_state_update", () => {
    const currentState = global_state.get("processing_status");
    if (currentState !== "processing") {
        global_state.put("processing_status", "processing");
        global_state.put("last_updated", Date.now());
        return "state_updated";
    }
    return "already_processing";
});

if (updateResult === "state_updated") {
    // Proceed with processing
    processData();
    locker.withLock("global_state_update", () => {
        global_state.put("processing_status", "completed");
        return null;
    });
}
```

### 4. File System Operations

```javascript
// MVEL - Coordinate file operations
fileResult = locker.withLock("file_processing", () -> {
    if (fileExists("input.txt")) {
        content = readFile("input.txt")
        processedContent = processContent(content)
        writeFile("output.txt", processedContent)
        deleteFile("input.txt")
        return "file processed"
    } else {
        return "no file to process"
    }
})
```

### 5. Resource Pool Management

```python
# Python - Manage shared resource pools
def acquire_database_connection():
    return locker.tryWithLock("db_connection_pool", lambda: {
        if get_available_connections() > 0:
            return allocate_connection()
        else:
            return None
    }, 2)  # 2 second timeout

connection = acquire_database_connection()
if connection:
    try:
        result = execute_query(connection, "SELECT * FROM transactions")
        process_results(result)
    finally:
        release_connection(connection)
else:
    print("No database connections available")
```

## Lock State Monitoring

### Debugging and Monitoring

```javascript
// MVEL - Check lock state for debugging
if (locker.isLocked("slow_operation")) {
    waitingCount = locker.getWaitingCount("slow_operation")
    log.info("Operation in progress, " + waitingCount + " plugins waiting")
} else {
    log.info("No operation in progress")
}
```

```javascript
// JavaScript - Monitor overall lock usage
const totalLocks = locker.getNamedLockCount();
console.log(`Currently managing ${totalLocks} named locks`);

// Check specific locks
const criticalLocks = ["database_migration", "external_api", "state_update"];
criticalLocks.forEach(lockName => {
    const isLocked = locker.isLocked(lockName);
    const waiting = locker.getWaitingCount(lockName);
    console.log(`Lock ${lockName}: locked=${isLocked}, waiting=${waiting}`);
});
```

### Cleanup Operations

```python
# Python - Cleanup for maintenance scenarios
def cleanup_locks():
    lock_count = locker.getNamedLockCount()
    print(f"Clearing {lock_count} named locks")
    locker.clearNamedLocks()
    print("Lock cleanup completed")

# Use with caution - only during maintenance
cleanup_locks()
```

## Error Handling and Best Practices

### 1. Handle Timeouts Gracefully

```javascript
// JavaScript - Graceful timeout handling
const processWithFallback = (data) => {
    const result = locker.tryWithLock("primary_processor", () => {
        return expensiveProcessing(data);
    }, 30);
    
    if (result === null) {
        console.warn("Primary processing timed out, using fallback");
        return fallbackProcessing(data);
    }
    
    return result;
};
```

### 2. Use Appropriate Lock Granularity

```javascript
// MVEL - Fine-grained locks for better concurrency
// Good - specific locks for different operations
userResult = locker.withLock("user_processing", () -> processUsers())
orderResult = locker.withLock("order_processing", () -> processOrders())

// Avoid - single lock for everything
// allResult = locker.withLock("everything", () -> {
//     processUsers()
//     processOrders()
//     return "all done"
// })
```

### 3. Handle Exceptions Properly

```python
# Python - Exception handling in locked operations
def safe_operation():
    try:
        return locker.withLock("critical_operation", lambda: {
            # Operation that might throw exception
            result = risky_operation()
            validate_result(result)
            return result
        })
    except Exception as e:
        print(f"Operation failed: {e}")
        return None

result = safe_operation()
if result:
    print(f"Operation succeeded: {result}")
```

### 4. Avoid Nested Locks (Prevent Deadlocks)

```javascript
// JavaScript - Avoid nested locks to prevent deadlocks
// Good - single level locking
const processA = () => locker.withLock("resource_a", () => doWorkA());
const processB = () => locker.withLock("resource_b", () => doWorkB());

// Avoid - nested locks can cause deadlocks
// const badNested = () => {
//     return locker.withLock("resource_a", () => {
//         return locker.withLock("resource_b", () => {
//             return doWork(); // Potential deadlock
//         });
//     });
// };
```

## Performance Considerations

1. **Lock Naming**: Use descriptive, consistent lock names
2. **Timeout Values**: Set reasonable timeouts to prevent hanging
3. **Lock Scope**: Keep locked operations as short as possible
4. **Resource Cleanup**: Named locks are automatically managed
5. **Monitoring**: Use state query methods for debugging
