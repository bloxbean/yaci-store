# Plugin File Client Usage Guide

## Overview

The enhanced `PluginFileClient` provides comprehensive file operations designed specifically for plugin developers using MVEL, JavaScript, or Python. It offers simple, intuitive methods for file I/O, directory operations, JSON/CSV handling, and archive operations with built-in error handling and security measures.

## Key Features

- **Simple API**: Minimal parameters for common file operations
- **Script-Friendly**: Designed for MVEL, JavaScript, and Python developers
- **Append Support**: Write and append modes for flexible file operations
- **JSON Operations**: Built-in JSON reading/writing with `readJson()`, `writeJson()`, and `appendJson()`
- **CSV Operations**: Easy CSV handling with headers and delimiters
- **Directory Operations**: Create, list, and walk directory trees
- **Path Operations**: Cross-platform path manipulation utilities
- **Archive Support**: Zip and unzip operations
- **Error Handling**: Consistent error reporting without exceptions
- **Security**: Path validation and protection against directory traversal attacks

## Basic Usage Examples

### Simple File Operations

```javascript
// MVEL - Basic file operations
content = files.read("config.txt")
if (content.isSuccess()) {
    configText = content.getAsString()
    log.info("Config loaded: " + configText.length() + " characters")
}

// Write new content
result = files.write("output.txt", "Hello, World!")
if (result.isSuccess()) {
    log.info("File written successfully")
}
```

```javascript
// JavaScript - File operations with error handling
const content = files.read("data.txt");
if (content.isSuccess()) {
    console.log("File content:", content.getAsString());
} else {
    console.error("Read failed:", content.getErrorMessage());
}

// Append to log file
const logEntry = `[${new Date()}] Block processed\n`;
files.append("process.log", logEntry);
```

```python
# Python - File operations
content = files.read("input.csv")
if content.isSuccess():
    data = content.getAsString()
    print(f"Read {len(data)} bytes from file")

# Write with append mode
files.write("debug.log", "Debug info\n", True)  # append=True
```

### File Information and Checks

```javascript
// MVEL - Check file existence and properties
if (files.exists("config.json")) {
    info = files.getInfo("config.json")
    if (info.isSuccess()) {
        fileInfo = info.getData()
        log.info("File size: " + fileInfo.getFormattedSize())
        log.info("Last modified: " + fileInfo.getLastModifiedString())
    }
}
```

```javascript
// JavaScript - File properties
if (files.exists("data.txt")) {
    const size = files.getFileSize("data.txt");
    const isReadable = files.isReadable("data.txt");
    const isWritable = files.isWritable("data.txt");
    
    console.log(`File size: ${size}, readable: ${isReadable}, writable: ${isWritable}`);
}
```

```python
# Python - File checks
if files.exists("backup.zip"):
    if files.isFile("backup.zip"):
        print("Found backup file")
    elif files.isDirectory("backup.zip"):
        print("Backup is a directory")
```

### Directory Operations

```javascript
// MVEL - Create and list directories
files.createDir("output/logs")

listing = files.listFiles("data/")
if (listing.isSuccess()) {
    fileInfos = listing.getFiles()
    log.info("Found " + fileInfos.size() + " files")
    
    // Filter by extension
    csvFiles = listing.filterByExtension("csv")
    for (fileInfo : csvFiles) {
        log.info("CSV file: " + fileInfo.getName())
    }
}
```

```javascript
// JavaScript - Recursive directory listing
const listing = files.listFilesRecursive("project/");
if (listing.isSuccess()) {
    const jsFiles = listing.filterByExtension("js");
    console.log(`Found ${jsFiles.length} JavaScript files`);
    
    jsFiles.forEach(file => {
        console.log(`- ${file.getPath()} (${file.getFormattedSize()})`);
    });
}
```

```python
# Python - Directory operations
files.createDirectory("exports/daily")

listing = files.listFiles("imports/")
if listing.isSuccess():
    files_list = listing.getRegularFiles()
    directories = listing.getDirectories()
    
    print(f"Files: {len(files_list)}, Directories: {len(directories)}")
    print(f"Total size: {listing.getFormattedTotalSize()}")
```

### Path Operations

```javascript
// MVEL - Path manipulation
basePath = "/data/exports"
filename = "report.csv"
fullPath = files.joinPath(basePath, "daily", filename)
log.info("Full path: " + fullPath)

parent = files.getParent(fullPath)
name = files.getFileName(fullPath)
extension = files.getExtension(fullPath)
baseName = files.getBaseName(fullPath)
```

```javascript
// JavaScript - Path operations
const dataPath = files.joinPath("data", "processed", "block_123.json");
const parentDir = files.getParent(dataPath);
const filename = files.getFileName(dataPath);

console.log(`File: ${filename} in directory: ${parentDir}`);

// Get absolute path
const absolutePath = files.getAbsolutePath("./relative/path.txt");
```

```python
# Python - Cross-platform paths
config_path = files.joinPath("config", "environments", "prod.yml")
log_dir = files.getParent(config_path)
file_extension = files.getExtension(config_path)

print(f"Config in {log_dir}, type: {file_extension}")
```

## JSON Operations

### Reading JSON Files

```javascript
// MVEL - Read JSON objects
configResult = files.readJson("config.json")
if (configResult.isSuccess()) {
    config = configResult.getData()
    databaseUrl = config.get("database_url")
    log.info("Database URL: " + databaseUrl)
}

// Read JSON arrays
transactionsResult = files.readJsonList("transactions.json")
if (transactionsResult.isSuccess()) {
    transactions = transactionsResult.getData()
    log.info("Loaded " + transactions.size() + " transactions")
}
```

```javascript
// JavaScript - JSON with error handling
const configResult = files.readJsonMap("settings.json");
if (configResult.isSuccess()) {
    const settings = configResult.getData();
    const apiKey = settings.api_key;
    const timeout = settings.request_timeout || 30;
    
    console.log(`API configured with ${timeout}s timeout`);
} else {
    console.error("Failed to load settings:", configResult.getErrorMessage());
}
```

```python
# Python - JSON operations
result = files.readJson("metadata.json")
if result.isSuccess():
    metadata = result.getData()
    version = metadata.get("version", "unknown")
    print(f"Application version: {version}")
```

### Writing JSON Files

```javascript
// MVEL - Write JSON data
exportData = {
    "timestamp": System.currentTimeMillis(),
    "block_number": currentBlock,
    "transaction_count": txCount
}

result = files.writeJson("exports/block_" + currentBlock + ".json", exportData)
if (result.isSuccess()) {
    log.info("Block data exported")
}
```

```javascript
// JavaScript - JSON export
const report = {
    generated_at: new Date().toISOString(),
    total_blocks: blockCount,
    total_transactions: transactionCount,
    summary: {
        avg_tx_per_block: transactionCount / blockCount,
        processing_time_ms: processingTime
    }
};

files.writeJson("reports/daily_report.json", report);
```

```python
# Python - JSON configuration
settings = {
    "database": {
        "host": "localhost",
        "port": 5432,
        "name": "yaci_store"
    },
    "logging": {
        "level": "INFO",
        "file": "application.log"
    }
}

files.writeJson("config/generated.json", settings)
```

### Appending to JSON Arrays

```javascript
// MVEL - Log events to JSON array
logEntry = {
    "timestamp": System.currentTimeMillis(),
    "event": "block_processed",
    "block": blockNumber,
    "tx_count": transactionCount
}

files.appendJson("logs/events.json", logEntry)
```

```javascript
// JavaScript - Event logging
const transactionEvent = {
    timestamp: Date.now(),
    tx_hash: transaction.getTxHash(),
    amount: transaction.getTotalOutput(),
    fee: transaction.getFee(),
    block_number: blockNumber
};

files.appendJson("audit/transactions.json", transactionEvent);
```

```python
# Python - Audit trail
audit_entry = {
    "user": "plugin_system",
    "action": "data_export", 
    "resource": f"block_{block_number}",
    "timestamp": System.currentTimeMillis(),
    "success": True
}

files.appendJson("audit/actions.json", audit_entry)
```

## CSV Operations

### Reading CSV Files

```javascript
// MVEL - Read CSV with headers
csvResult = files.readCsv("data.csv")
if (csvResult.isSuccess()) {
    records = csvResult.getData()
    for (record : records) {
        name = record.get("name")
        amount = record.get("amount")
        log.info("Record: " + name + " = " + amount)
    }
}
```

```javascript
// JavaScript - CSV with custom delimiter
const csvResult = files.readCsv("data.tsv", "\t", true);
if (csvResult.isSuccess()) {
    const records = csvResult.getData();
    console.log(`Loaded ${records.length} records`);
    
    records.forEach(record => {
        console.log(`ID: ${record.id}, Value: ${record.value}`);
    });
}
```

```python
# Python - CSV processing
result = files.readCsv("transactions.csv")
if result.isSuccess():
    records = result.getData()
    total_amount = sum(float(r.get("amount", 0)) for r in records)
    print(f"Total amount: {total_amount}")
```

### Writing CSV Files

```javascript
// MVEL - Write CSV with headers
headers = ["block_number", "timestamp", "tx_count", "total_fees"]
rows = []

for (i = 1; i <= 10; i++) {
    row = [String.valueOf(i), String.valueOf(System.currentTimeMillis()), 
           String.valueOf(i * 5), String.valueOf(i * 1000)]
    rows.add(row)
}

files.writeCsv("reports/block_summary.csv", headers, rows)
```

```javascript
// JavaScript - CSV export
const headers = ["address", "balance", "tx_count"];
const rows = addresses.map(addr => [
    addr.address,
    addr.balance.toString(),
    addr.transactionCount.toString()
]);

files.writeCsv("exports/addresses.csv", headers, rows);
```

```python
# Python - CSV data export
headers = ["epoch", "blocks", "transactions", "rewards"]
rows = []

for epoch_data in epoch_stats:
    row = [
        str(epoch_data.epoch),
        str(epoch_data.block_count), 
        str(epoch_data.tx_count),
        str(epoch_data.total_rewards)
    ]
    rows.append(row)

files.writeCsv("analytics/epoch_stats.csv", headers, rows)
```

### Appending to CSV Files

```javascript
// MVEL - Continuous data logging
newRows = []
newRows.add([String.valueOf(blockNumber), String.valueOf(System.currentTimeMillis()), 
             String.valueOf(transactionCount)])

files.appendCsv("logs/block_log.csv", newRows)
```

```javascript
// JavaScript - Real-time data collection
const newRows = [[
    blockNumber.toString(),
    new Date().toISOString(),
    transactionCount.toString(),
    totalFees.toString()
]];

files.appendCsv("monitoring/real_time.csv", newRows);
```

```python
# Python - Incremental logging
new_rows = [[
    str(transaction.getTxHash()),
    str(transaction.getTotalOutput()),
    str(System.currentTimeMillis())
]]

files.appendCsv("data/transaction_log.csv", new_rows)
```

## Archive Operations

### Creating ZIP Archives

```javascript
// MVEL - Backup directory
result = files.zip("data/exports", "backups/export_" + System.currentTimeMillis() + ".zip")
if (result.isSuccess()) {
    log.info("Backup created successfully")
}
```

```javascript
// JavaScript - Archive logs
const archiveName = `logs_${new Date().toISOString().split('T')[0]}.zip`;
const result = files.zip("logs/", `archives/${archiveName}`);

if (result.isSuccess()) {
    console.log(`Logs archived to ${archiveName}`);
    // Optionally clean up old logs
}
```

```python
# Python - Data archival
backup_file = f"backup_{System.currentTimeMillis()}.zip"
result = files.zip("data/processed", f"backups/{backup_file}")

if result.isSuccess():
    print(f"Created backup: {backup_file}")
```

### Extracting ZIP Archives

```javascript
// MVEL - Extract backup
result = files.unzip("backups/data_backup.zip", "restored/")
if (result.isSuccess()) {
    log.info("Backup restored successfully")
} else {
    log.error("Restore failed: " + result.getErrorMessage())
}
```

```javascript
// JavaScript - Extract and process
const extractResult = files.unzip("imports/data.zip", "temp/extracted/");
if (extractResult.isSuccess()) {
    // Process extracted files
    const listing = files.listFiles("temp/extracted/");
    console.log(`Extracted ${listing.size()} files`);
}
```

```python
# Python - Automated extraction
result = files.unzip("uploads/package.zip", "processing/")
if result.isSuccess():
    print("Package extracted for processing")
    # Continue with file processing
else:
    print(f"Extraction failed: {result.getErrorMessage()}")
```

## Temporary Files and Directories

```javascript
// MVEL - Create temp files for processing
tempFile = files.createTempFile("process", ".tmp")
if (tempFile.isSuccess()) {
    tempPath = tempFile.getAsString()
    files.write(tempPath, processedData)
    // Process temp file
    files.delete(tempPath)  // Clean up
}
```

```javascript
// JavaScript - Temp directory for batch processing
const tempDirResult = files.createTempDirectory("batch_process");
if (tempDirResult.isSuccess()) {
    const tempDir = tempDirResult.getAsString();
    
    // Process files in temp directory
    processFiles(tempDir);
    
    // Clean up temp directory
    files.listFiles(tempDir).getFiles().forEach(file => {
        files.delete(file.getPath());
    });
    files.delete(tempDir);
}
```

```python
# Python - Temporary file operations
temp_file = files.createTempFile("export", ".json")
if temp_file.isSuccess():
    temp_path = temp_file.getAsString()
    
    # Write data to temp file
    files.writeJson(temp_path, export_data)
    
    # Process and move to final location
    files.move(temp_path, "exports/final.json")
```

## Best Practices

### 1. Error Handling
Always check operation results before processing data:

```javascript
const result = files.read("config.json");
if (!result.isSuccess()) {
    console.error(`Failed to read config: ${result.getErrorMessage()}`);
    return;
}

const content = result.getAsString();
```

### 2. Path Operations
Use path operations for cross-platform compatibility:

```javascript
// Good - platform-independent
const logPath = files.joinPath("logs", "application.log");

// Avoid - platform-specific
// const logPath = "logs/application.log";  // Unix only
// const logPath = "logs\\application.log"; // Windows only
```

### 3. Append vs Write
Choose the appropriate write mode for your use case:

```javascript
// For logs and continuous data - use append
files.append("transaction.log", newLogEntry);

// For configuration and complete data - use write
files.write("config.json", configData);
```

### 4. Directory Creation
Create parent directories when needed:

```javascript
// File operations automatically create parent directories
files.write("reports/daily/summary.txt", reportData);  // Creates reports/daily/

// Or explicitly create directories
files.createDir("exports/archive");
```

### 5. Resource Cleanup
Clean up temporary resources:

```javascript
const tempFile = files.createTempFile("process", ".tmp");
if (tempFile.isSuccess()) {
    const path = tempFile.getAsString();
    try {
        // Use temp file
        processData(path);
    } finally {
        // Always clean up
        files.delete(path);
    }
}
```

### 6. Security Considerations
The file client includes built-in security measures:

- Path validation to prevent directory traversal attacks
- ZIP slip protection during extraction
- Automatic parent directory creation within allowed paths

```javascript
// Safe - paths are validated
files.write("data/export.json", jsonData);

// Protected - directory traversal attempts are blocked
// files.write("../../../etc/passwd", maliciousData);  // Would fail
```

## Migration from Manual File Operations

### Before (Manual file handling)
```javascript
// Old way - complex and error-prone
try {
    const path = java.nio.file.Paths.get("data.json");
    const content = java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
    const data = JSON.parse(content);
    // Process data
} catch (e) {
    console.error("File operation failed:", e.message);
}
```

### After (File client)
```javascript
// New way - simple and robust
const result = files.readJsonMap("data.json");
if (result.isSuccess()) {
    const data = result.getData();
    // Process data
} else {
    console.error("File operation failed:", result.getErrorMessage());
}
```

## Common Use Cases

### 1. Configuration Management
```javascript
function loadConfig(configFile) {
    const result = files.readJsonMap(configFile);
    if (result.isSuccess()) {
        return result.getData();
    } else {
        console.warn(`Config not found, using defaults: ${result.getErrorMessage()}`);
        return getDefaultConfig();
    }
}
```

### 2. Data Export
```javascript
function exportBlockData(blockNumber, transactions) {
    const exportData = {
        block: blockNumber,
        timestamp: new Date().toISOString(),
        transactions: transactions.map(tx => ({
            hash: tx.getTxHash(),
            amount: tx.getTotalOutput()
        }))
    };
    
    const filename = `block_${blockNumber}.json`;
    return files.writeJson(`exports/${filename}`, exportData);
}
```

### 3. Audit Logging
```javascript
function auditLog(action, details) {
    const logEntry = {
        timestamp: new Date().toISOString(),
        action: action,
        details: details,
        user: "plugin_system"
    };
    
    files.appendJson("audit/actions.json", logEntry);
}
```

### 4. Batch Processing
```javascript
function processCsvBatch(inputDir, outputDir) {
    const listing = files.listFiles(inputDir);
    if (!listing.isSuccess()) return;
    
    const csvFiles = listing.filterByExtension("csv");
    const results = [];
    
    csvFiles.forEach(fileInfo => {
        const csvResult = files.readCsv(fileInfo.getPath());
        if (csvResult.isSuccess()) {
            const processed = processRecords(csvResult.getData());
            const outputPath = files.joinPath(outputDir, `processed_${fileInfo.getName()}`);
            files.writeJson(outputPath, processed);
            results.push(outputPath);
        }
    });
    
    return results;
}
```
