# Plugin HTTP Client Usage Guide

## Overview

The enhanced `PluginHttpClient` provides a comprehensive HTTP client designed specifically for plugin developers using MVEL, JavaScript, or Python. It offers simple, intuitive methods for common HTTP operations with built-in support for authentication, JSON handling, and error management.

## Key Features

- **Simple API**: Minimal parameters for common use cases
- **Script-Friendly**: Designed for MVEL, JavaScript, and Python developers
- **JSON-first**: Built-in JSON serialization and parsing with `asJson()` and `asJsonList()`
- **Authentication**: Easy basic auth, bearer tokens, and API keys
- **Error Handling**: Status checking and timeout support
- **Response Headers**: Access to response headers and content types
- **Query Parameters**: Easy URL building with parameters
- **Timeout Support**: Integer-based timeouts (milliseconds or seconds) instead of Java Duration objects
- **Generic Retry Wrapper**: Retry any HTTP operation using closures/functions with exponential backoff

## Basic Usage Examples

### Simple GET Requests

```javascript
// MVEL - Basic GET request
response = http.get("https://api.example.com/users")
if (response.isSuccess()) {
    users = response.asJsonMap()
    log.info("Found " + users.size() + " users")
}
```

```javascript
// JavaScript - GET with query parameters
const response = http.getWithParams("https://api.example.com/search", {
    "q": "cardano",
    "limit": "10"
});

if (response.isSuccess()) {
    const data = response.asJsonMap();
    console.log("Search results:", data.results.length);
}
```

```python
# Python - GET with headers
response = http.get("https://api.example.com/data", {
    "User-Agent": "YaciPlugin/1.0",
    "Accept": "application/json"
})

if response.isSuccess():
    data = response.asJsonMap()
    print(f"Data received: {len(data)} items")
```

### JSON POST Requests

```javascript
// MVEL - POST JSON data
user = {
    "name": "Alice",
    "email": "alice@example.com",
    "age": 30
}

response = http.postJson("https://api.example.com/users", user)
if (response.isSuccess()) {
    createdUser = response.asJsonMap()
    log.info("Created user with ID: " + createdUser.id)
} else {
    log.error("Failed to create user: " + response.getStatus())
}
```

```javascript
// JavaScript - POST with custom headers
const newRecord = {
    transaction_id: "tx123",
    amount: 1000000,
    address: "addr1..."
};

const response = http.postJson("https://webhook.example.com/notify", newRecord, {
    "Content-Type": "application/json",
    "X-Source": "yaci-store"
});

if (response.isError()) {
    console.error("Webhook failed:", response.getStatus(), response.getBody());
}
```

```python
# Python - POST with object serialization
transaction_data = {
    "block_number": event.getMetadata().getBlock(),
    "tx_hash": transaction.getTxHash(),
    "outputs": [output.getLovelaceAmount() for output in transaction.getOutputs()]
}

response = http.postJson("https://analytics.example.com/transactions", transaction_data)
if response.isSuccess():
    print("Transaction data sent successfully")
```

## Authentication Examples

### Basic Authentication

```javascript
// MVEL - Basic auth
response = http.getWithBasicAuth("https://api.example.com/protected", "username", "password")
if (response.isSuccess()) {
    data = response.asJsonMap()
}
```

```javascript
// JavaScript - Basic auth with POST
const payload = { "action": "sync", "block": 12345 };
const response = http.postJsonWithBasicAuth(
    "https://api.example.com/sync", 
    payload, 
    "api_user", 
    "secret_key"
);
```

### Bearer Token Authentication

```javascript
// MVEL - Bearer token
token = "eyJhbGciOiJIUzI1NiIs..."
response = http.getWithBearerToken("https://api.example.com/me", token)
```

```python
# Python - Bearer token from environment or config
bearer_token = os.getenv("API_TOKEN")
response = http.getWithBearerToken("https://api.example.com/data", bearer_token)
```

### API Key Authentication

```javascript
// JavaScript - API key in header
const response = http.getWithApiKey("https://api.example.com/stats", "X-API-Key", "abc123");
```

```javascript
// MVEL - Custom API key header
response = http.getWithApiKey("https://api.example.com/data", "Authorization", "ApiKey " + apiKey)
```

## Form Data Submission

```javascript
// MVEL - Form submission
formData = {
    "name": "Transaction Alert",
    "email": "admin@example.com",
    "message": "Large transaction detected: " + amount
}

response = http.postForm("https://contact.example.com/submit", formData)
```

```python
# Python - Form with custom headers
form_fields = {
    "event_type": "block_commit",
    "block_number": str(block_number),
    "timestamp": str(System.currentTimeMillis())
}

response = http.postForm("https://webhook.site/unique-id", form_fields, {
    "X-Source": "yaci-store-plugin"
})
```

## Response Handling

### Status Code Checking

```javascript
// JavaScript - Comprehensive error handling
const response = http.get("https://api.example.com/status");

if (response.isSuccess()) {
    console.log("API is healthy");
} else if (response.isClientError()) {
    console.warn("Client error:", response.getStatus(), response.getBody());
} else if (response.isServerError()) {
    console.error("Server error:", response.getStatus());
    // Maybe retry logic here
} else {
    console.info("Unexpected status:", response.getStatus());
}
```

### JSON Response Parsing (Script-Friendly)

```javascript
// MVEL - Parse JSON objects (most common case)
response = http.get("https://api.example.com/blocks/latest")

if (response.isJson()) {
    blockData = response.asJson()  // Simple and intuitive
    blockNumber = blockData.number
    blockHash = blockData.hash
    
    log.info("Latest block: " + blockNumber + " (" + blockHash + ")")
}
```

```javascript
// JavaScript - Parse JSON arrays
const response = http.get("https://api.example.com/transactions");
if (response.isJson()) {
    const transactions = response.asJsonList();  // For JSON arrays
    console.log(`Found ${transactions.length} transactions`);
    
    transactions.forEach(tx => {
        console.log(`TX: ${tx.hash}, Amount: ${tx.amount}`);
    });
}
```

```python
# Python - Handle both objects and arrays
response = http.get("https://api.example.com/data")

if response.isJson():
    try:
        data = response.asJson()  # For JSON objects
        process_data(data)
    except Exception as e:
        # Try as array if object parsing fails
        try:
            data = response.asJsonList()  # For JSON arrays
            process_array_data(data)
        except Exception as e2:
            print(f"JSON parsing failed: {e2}")
else:
    print(f"Unexpected content type: {response.getContentType()}")
```

### Response Headers

```javascript
// JavaScript - Access response headers
const response = http.get("https://api.example.com/data");

const contentType = response.getContentType();
const rateLimit = response.getHeader("X-RateLimit-Remaining");
const etag = response.getHeader("ETag");

console.log(`Content-Type: ${contentType}, Rate Limit: ${rateLimit}`);
```

## Advanced Usage

### Custom Timeouts (Script-Friendly)

```javascript
// MVEL - Timeout in milliseconds
response = http.getWithTimeout("https://slow-api.example.com/process", 60000)
if (response.isSuccess()) {
    data = response.asJson()
}
```

```javascript
// JavaScript - Timeout in seconds (more readable)
const response = http.getWithTimeoutSeconds("https://api.example.com/data", 30);
if (response.isSuccess()) {
    const data = response.asJson();
    console.log("Data received:", data);
}
```

```python
# Python - POST with timeout in milliseconds
response = http.postJsonWithTimeout(
    "https://api.example.com/urgent", 
    {"alert": "High priority"},
    5000  # 5 seconds in milliseconds
)

# Python - POST with timeout in seconds (cleaner)
response = http.postJsonWithTimeoutSeconds(
    "https://api.example.com/urgent",
    {"alert": "High priority"}, 
    5  # 5 seconds
)
```

### Error Recovery and Retries (Generic Wrapper)

The HTTP client now includes a generic retry wrapper that works idiomatically across all script languages using closures/functions.

#### Basic Retry Usage

```javascript
// MVEL - Simple retry with default conditions (5xx errors and timeouts)
response = http.retry(() -> http.get("https://unreliable-api.example.com/data"), 3, 1)
if (response.isSuccess()) {
    data = response.asJsonMap()
    log.info("Success after retries")
}
```

```javascript
// JavaScript - Retry POST request
const response = http.retry(
    () => http.postJson("https://api.example.com/submit", formData),
    5, 2  // 5 retries, 2 second initial delay
);

if (response.isSuccess()) {
    console.log("POST succeeded after retries");
} else {
    console.error("POST failed after all retries:", response.getStatus());
}
```

```python
# Python - Retry with lambda
response = http.retry(
    lambda: http.get("https://api.example.com/data"),
    3, 1  # 3 retries, 1 second initial delay
)

if response.isSuccess():
    data = response.asJsonMap()
    print("Request succeeded")
```

#### Advanced Retry with Custom Conditions

```javascript
// MVEL - Retry only on specific status codes
response = http.retryWithCondition(
    () -> http.get("https://api.example.com/rate-limited"),
    5, 1,
    (resp) -> resp.getStatus() == 429 || resp.getStatus() == 503
)
```

```javascript
// JavaScript - Custom retry logic for authentication
const response = http.retryWithCondition(
    () => http.getWithBearerToken("https://api.example.com/protected", token),
    3, 2,
    (resp) => resp.getStatus() !== 401  // Don't retry auth failures
);
```

```python
# Python - Retry on server errors only
response = http.retryWithCondition(
    lambda: http.postJson("https://api.example.com/process", data),
    4, 1,
    lambda resp: resp.isServerError()  # Only retry 5xx errors
)
```

#### Convenience Retry Methods

```javascript
// MVEL - Retry only on server errors (5xx)
response = http.retryOnServerError(
    () -> http.get("https://backend-api.example.com/data"),
    3, 2
)
```

```javascript
// JavaScript - Retry on any error (4xx or 5xx)
const response = http.retryOnAnyError(
    () => http.get("https://external-api.example.com/data"),
    2, 1
);
```

```python
# Python - Retry on specific status codes
retry_condition = http.retryOnStatusCodes(503, 429, 502)
response = http.retryWithCondition(
    lambda: http.get("https://api.example.com/data"),
    5, 1,
    retry_condition
)
```

#### Complex Operations with Retry

```javascript
// MVEL - Complex operation with authentication and retry
response = http.retry(() -> {
    headers = {"Authorization": "Bearer " + getAuthToken(), "X-Client": "yaci-plugin"}
    return http.postJson("https://api.example.com/complex-operation", {
        "block": currentBlock,
        "transactions": txList,
        "timestamp": System.currentTimeMillis()
    }, headers)
}, 3, 2)
```

```javascript
// JavaScript - Multi-step operation with retry
const response = http.retry(() => {
    // Complex operation that might fail
    const authHeaders = {"Authorization": `Bearer ${getToken()}`};
    return http.postJsonWithTimeout(
        "https://api.example.com/batch-process",
        {
            blockData: processedBlock,
            metadata: blockMetadata
        },
        authHeaders,
        30000  // 30 second timeout
    );
}, 3, 5);  // 3 retries with 5 second delays
```

```python
# Python - Retry wrapper around complex API interaction
def complex_api_call():
    # Prepare complex payload
    payload = {
        "event_data": prepare_event_data(),
        "signatures": calculate_signatures(),
        "metadata": get_metadata()
    }
    
    # Make authenticated request with timeout
    headers = {"Authorization": f"Bearer {api_token}"}
    return http.postJsonWithTimeoutSeconds(
        "https://api.example.com/events",
        payload,
        headers,
        15  # 15 second timeout
    )

# Retry the complex operation
response = http.retry(complex_api_call, 3, 2)
if response.isSuccess():
    result = response.asJsonMap()
    print(f"Event processed with ID: {result['id']}")
```

#### Retry Behavior

- **Default Condition**: Retries on server errors (5xx) and request timeouts (408)
- **Exponential Backoff**: Delays increase exponentially (1s, 2s, 4s, 8s, etc.)
- **Script-Friendly**: Uses simple integers for retry counts and delays
- **Flexible**: Any HTTP operation can be wrapped with retry logic
- **Exception Handling**: Network errors and exceptions are automatically retried

#### Best Practices for Retries

1. **Use appropriate retry counts**: 3-5 retries for most cases
2. **Don't retry client errors**: 4xx errors usually won't succeed on retry
3. **Be mindful of delays**: Exponential backoff prevents overwhelming servers
4. **Use custom conditions**: Tailor retry behavior to your specific API
5. **Combine with timeouts**: Set reasonable timeouts to prevent hanging

### Webhook Integration

```python
# Python - Send block events to external webhook
def send_block_webhook(block_event):
    webhook_url = "https://your-app.com/webhooks/yaci"
    
    payload = {
        "event_type": "block_committed",
        "block_number": block_event.getMetadata().getBlock(),
        "block_hash": block_event.getMetadata().getBlockHash(),
        "timestamp": System.currentTimeMillis(),
        "slot": block_event.getMetadata().getSlot()
    }
    
    response = http.postJsonWithBearerToken(webhook_url, payload, webhook_token)
    
    if not response.isSuccess():
        print(f"Webhook failed: {response.getStatus()} - {response.getBody()}")
        return False
    
    return True

# Usage in event handler
def block_event(event):
    success = send_block_webhook(event)
    if success:
        print(f"Webhook sent for block {event.getMetadata().getBlock()}")
```

### API Integration Patterns

```javascript
// JavaScript - External API integration with caching
function fetchExchangeRate(currency) {
    const cacheKey = `exchange_rate_${currency}`;
    const cachedRate = global_state.get(cacheKey);
    
    if (cachedRate && isCacheValid(cachedRate.timestamp)) {
        return cachedRate.rate;
    }
    
    const response = http.getWithApiKey(
        `https://api.exchangerate.com/v1/rates/${currency}`,
        "X-API-Key",
        exchange_api_key
    );
    
    if (response.isSuccess()) {
        const data = response.asJsonMap();
        const rate = data.rates.USD;
        
        // Cache for 5 minutes
        global_state.put(cacheKey, {
            rate: rate,
            timestamp: Date.now()
        });
        
        return rate;
    }
    
    return null;
}

function isCacheValid(timestamp) {
    const fiveMinutes = 5 * 60 * 1000;
    return (Date.now() - timestamp) < fiveMinutes;
}
```

## Best Practices

### 1. Error Handling
Always check response status before processing data:

```javascript
const response = http.get(url);
if (!response.isSuccess()) {
    console.error(`HTTP ${response.getStatus()}: ${response.getBody()}`);
    return;
}
```

### 2. Use Appropriate Methods
Choose the right HTTP method for your use case:

```javascript
// Use specific methods for clarity
http.get(url)           // For retrieving data
http.postJson(url, data) // For creating resources
http.putJson(url, data)  // For updating resources
http.delete(url)         // For deleting resources
```

### 3. Handle Authentication Securely
Store sensitive credentials safely:

```javascript
// Good - use environment variables or secure config
const token = System.getenv("API_TOKEN");
const response = http.getWithBearerToken(url, token);

// Avoid - hardcoding credentials in scripts
// const response = http.getWithBearerToken(url, "hardcoded-token");
```

### 4. Use Timeouts for External Calls
Set appropriate timeouts for external APIs using simple integers:

```javascript
// For critical real-time operations (5 seconds in milliseconds)
const response = http.getWithTimeout(url, 5000);

// For batch processing (2 minutes in seconds - more readable)
const response = http.getWithTimeoutSeconds(url, 120);

// MVEL - Quick timeout for real-time data
response = http.getWithTimeoutSeconds("https://live-api.example.com/data", 3)

// Python - Long timeout for slow APIs
response = http.getWithTimeout("https://slow-api.example.com/batch", 300000)  # 5 minutes
```

### 5. Log HTTP Operations
Add logging for debugging and monitoring:

```javascript
console.log(`Making request to: ${url}`);
const response = http.get(url);
console.log(`Response: ${response.getStatus()}`);

if (response.isError()) {
    console.error(`Error response: ${response.getBody()}`);
}
```

## Migration from Simple HTTP Usage

If you're currently using basic HTTP operations, here's how to migrate:

### Before (Manual HTTP handling)
```javascript
// Old way - complex and error-prone
const url = "https://api.example.com/data?param1=value1&param2=value2";
const response = http.get(url, {"Authorization": "Bearer " + token});
const data = JSON.parse(response.getBody());
```

### After (Enhanced HTTP client)
```javascript
// New way - simple and robust
const response = http.getWithParams("https://api.example.com/data", {
    "param1": "value1",
    "param2": "value2"
}, {"Authorization": "Bearer " + token});

const data = response.asJsonMap();
```

## Common Use Cases

### 1. External Webhook Notifications
```javascript
function notifyExternalSystem(transaction) {
    const payload = {
        txHash: transaction.getTxHash(),
        amount: transaction.getTotalOutput(),
        timestamp: new Date().toISOString()
    };
    
    const response = http.postJsonWithApiKey(
        "https://your-system.com/webhooks/cardano",
        payload,
        "X-API-Key",
        webhook_api_key
    );
    
    return response.isSuccess();
}
```

### 2. External Data Enrichment
```javascript
function enrichAddressData(address) {
    const response = http.get(`https://pool-info-api.com/address/${address}`);
    
    if (response.isSuccess() && response.isJson()) {
        return response.asJsonMap();
    }
    
    return null;
}
```

### 3. Health Check Integration
```javascript
function reportHealth() {
    const healthData = {
        status: "healthy",
        timestamp: Date.now(),
        lastBlock: global_state.get("last_processed_block")
    };
    
    http.postJson("https://monitoring.example.com/health", healthData);
}
```

## Quick Reference - Retry Methods

### Core Retry Methods
- `retry(operation, maxRetries, delaySeconds)` - Retry with default conditions (5xx errors, timeouts)
- `retryWithCondition(operation, maxRetries, delaySeconds, condition)` - Retry with custom conditions

### Convenience Methods  
- `retryOnServerError(operation, maxRetries, delaySeconds)` - Retry only on 5xx errors
- `retryOnAnyError(operation, maxRetries, delaySeconds)` - Retry on any 4xx/5xx errors
- `retryOnStatusCodes(statusCode1, statusCode2, ...)` - Create condition for specific status codes

### Parameters
- **operation**: Supplier function `() -> HttpResponseWrapper` (closure/lambda/arrow function)
- **maxRetries**: Integer (0 = no retries, 3-5 recommended for most cases)
- **delaySeconds**: Integer initial delay (exponential backoff: 1s, 2s, 4s, 8s...)
- **condition**: Predicate function `(response) -> boolean` returning true to retry

### Language Examples
```javascript
// MVEL
http.retry(() -> http.get(url), 3, 1)

// JavaScript  
http.retry(() => http.get(url), 3, 1)

// Python
http.retry(lambda: http.get(url), 3, 1)
```

This enhanced HTTP client makes it significantly easier for plugin developers to integrate with external APIs and services while maintaining robust error handling and security practices.