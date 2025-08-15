package com.bloxbean.cardano.yaci.store.plugin.file;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simplified, secure file API for plugin scripts (MVEL/JS/Python).
 * Provides essential file operations for blockchain data processing:
 * - Basic file I/O (read, write, append)
 * - JSON operations for configuration and data export
 * - CSV operations for tabular data export
 * - Simple directory operations
 *
 * Security features:
 * - All paths are confined under sandbox root
 * - Atomic writes for data integrity
 * - Path validation against directory traversal
 * - Optional file locking for concurrent access
 */
@Component
public class PluginFileClient {
    private static final long ATOMIC_WRITE_THRESHOLD_BYTES = 16 * 1024 * 1024; // 16 MB

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Path pluginFilesRoot;
    private final Path realRoot; // cached real path for fast checks
    private final boolean enableLocks;

    private final ConcurrentHashMap<Path, ReentrantLock> pathLocks = new ConcurrentHashMap<>();

    public PluginFileClient(StoreProperties storeProperties) {
        try {
            this.pluginFilesRoot = Paths.get(storeProperties.getPluginFilesRootPath()).toAbsolutePath().normalize();

            // Create sandbox root directory only if plugins are enabled
            if (storeProperties.isPluginsEnabled() && !Files.exists(this.pluginFilesRoot)) {
                Files.createDirectories(this.pluginFilesRoot);
            }

            // Get real path if directory exists, otherwise use normalized path
            if (Files.exists(this.pluginFilesRoot)) {
                this.realRoot = this.pluginFilesRoot.toRealPath(LinkOption.NOFOLLOW_LINKS);
            } else {
                // Directory doesn't exist (plugins disabled), use normalized path
                this.realRoot = this.pluginFilesRoot;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to initialize sandbox root: " + storeProperties.getPluginFilesRootPath(), e);
        }
        this.enableLocks = storeProperties.isPluginFilesEnableLocks();
    }

    // ===== Path Safety =====

    private Path safeResolve(String first, String... more) throws IOException {
        Path candidate = pluginFilesRoot.resolve(Paths.get(first, more != null ? more : new String[0]))
                .normalize();

        // For existing paths, check they don't escape sandbox
        if (Files.exists(candidate, LinkOption.NOFOLLOW_LINKS)) {
            Path realCandidate = candidate.toRealPath(LinkOption.NOFOLLOW_LINKS);
            if (!realCandidate.startsWith(realRoot)) {
                throw new SecurityException("Path escapes sandbox: " + candidate);
            }
            return realCandidate;
        } else {
            // For non-existent paths, check the parent directory
            Path parent = candidate.getParent();
            if (parent != null && Files.exists(parent, LinkOption.NOFOLLOW_LINKS)) {
                Path realParent = parent.toRealPath(LinkOption.NOFOLLOW_LINKS);
                if (!realParent.startsWith(realRoot)) {
                    throw new SecurityException("Path escapes sandbox: " + candidate);
                }
            } else if (parent != null) {
                // Recursively check parent directories until we find one that exists
                Path existingAncestor = parent;
                while (existingAncestor != null && !Files.exists(existingAncestor, LinkOption.NOFOLLOW_LINKS)) {
                    existingAncestor = existingAncestor.getParent();
                }
                if (existingAncestor != null) {
                    Path realAncestor = existingAncestor.toRealPath(LinkOption.NOFOLLOW_LINKS);
                    if (!realAncestor.startsWith(realRoot)) {
                        throw new SecurityException("Path escapes sandbox: " + candidate);
                    }
                } else {
                    // No existing ancestor found, check if the path is under sandbox root
                    if (!candidate.startsWith(pluginFilesRoot)) {
                        throw new SecurityException("Path escapes sandbox: " + candidate);
                    }
                }
            } else {
                // Path has no parent (root-level file in sandbox)
                if (!candidate.startsWith(pluginFilesRoot)) {
                    throw new SecurityException("Path escapes sandbox: " + candidate);
                }
            }
            return candidate;
        }
    }

    private ReentrantLock acquireLocalLock(Path p) {
        if (!enableLocks) return null;
        ReentrantLock lock = pathLocks.computeIfAbsent(p, k -> new ReentrantLock());
        lock.lock();
        return lock;
    }

    private void releaseLocalLock(ReentrantLock lock) {
        if (lock != null) lock.unlock();
    }

    // ===== Core File Operations =====

    /**
     * Read the contents of a text file.
     * @param filePath Path to the file relative to sandbox root
     * @return FileOperationResult containing the file content as string
     */
    public FileOperationResult read(String filePath) {
        try {
            Path path = safeResolve(filePath);
            if (!Files.exists(path)) {
                return FileOperationResult.error("File does not exist: " + filePath);
            }
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return FileOperationResult.success(content);
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read file (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    /**
     * Write content to a file. Creates parent directories if needed.
     * Uses atomic write for files &lt;= 16MB for crash safety.
     * @param filePath Path to the file relative to sandbox root
     * @param content Content to write
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult write(String filePath, String content) {
        return write(filePath, content, false);
    }

    /**
     * Write or append content to a file. Creates parent directories if needed.
     * @param filePath Path to the file relative to sandbox root
     * @param content Content to write
     * @param append If true, append to existing file; if false, overwrite
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult write(String filePath, String content, boolean append) {
        try {
            Path path = safeResolve(filePath);
            byte[] data = content.getBytes(StandardCharsets.UTF_8);
            return doWriteBytes(path, data, append);
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write file (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    private FileOperationResult doWriteBytes(Path path, byte[] content, boolean append) throws IOException {
        Files.createDirectories(path.getParent());

        ReentrantLock localLock = acquireLocalLock(path);
        try {
            if (append) {
                // Append path (no atomicity)
                try (FileChannel ch = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                    try (FileLock ignored = enableLocks ? ch.lock() : null) {
                        ch.write(java.nio.ByteBuffer.wrap(content));
                    }
                }
            } else {
                // Atomic write path for <= threshold, else direct (streaming) write.
                if (content.length <= ATOMIC_WRITE_THRESHOLD_BYTES) {
                    atomicWrite(path, content);
                } else {
                    try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(path,
                            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
                        out.write(content);
                    }
                }
            }
            return FileOperationResult.success();
        } finally {
            releaseLocalLock(localLock);
        }
    }

    private void atomicWrite(Path path, byte[] bytes) throws IOException {
        Path dir = path.getParent();
        Files.createDirectories(dir);
        Path tmp = Files.createTempFile(dir, path.getFileName().toString(), ".tmp");
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(tmp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
            out.write(bytes);
        }
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * Append content to a file. Creates the file if it doesn't exist.
     * @param filePath Path to the file relative to sandbox root
     * @param content Content to append
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult append(String filePath, String content) {
        return write(filePath, content, true);
    }

    /**
     * Delete a file.
     * @param filePath Path to the file relative to sandbox root
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult delete(String filePath) {
        try {
            Path path = safeResolve(filePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) return FileOperationResult.success();
            return FileOperationResult.error("File does not exist: " + filePath);
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to delete file (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    /**
     * Check if a file or directory exists.
     * @param filePath Path to check relative to sandbox root
     * @return true if the file exists, false otherwise
     */
    public boolean exists(String filePath) {
        try {
            Path p = safeResolve(filePath);
            return Files.exists(p);
        } catch (Exception e) {
            return false;
        }
    }

    // ===== Directory Operations =====

    /**
     * Create a directory and all necessary parent directories.
     * @param dirPath Path to the directory relative to sandbox root
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult createDir(String dirPath) {
        try {
            Path path = safeResolve(dirPath);
            Files.createDirectories(path);
            return FileOperationResult.success();
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to create directory (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    /**
     * List files and directories in a directory (non-recursive).
     * @param dirPath Path to the directory relative to sandbox root
     * @return DirectoryListing containing file information
     */
    public DirectoryListing listFiles(String dirPath) {
        try {
            Path path = safeResolve(dirPath);
            if (!Files.exists(path)) {
                return DirectoryListing.error("Directory does not exist: " + dirPath);
            }
            if (!Files.isDirectory(path)) {
                return DirectoryListing.error("Path is not a directory: " + dirPath);
            }

            List<FileInfo> files;
            try (Stream<Path> list = Files.list(path)) {
                files = list.map(this::createFileInfoQuiet)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
            }
            return DirectoryListing.success(files);
        } catch (SecurityException se) {
            return DirectoryListing.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return DirectoryListing.error("Failed to list files (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    private FileInfo createFileInfoQuiet(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            return new FileInfo(
                    path,
                    attrs.size(),
                    attrs.lastModifiedTime().toMillis(),
                    attrs.isDirectory(),
                    attrs.isRegularFile(),
                    true,
                    Files.isReadable(path),
                    Files.isWritable(path)
            );
        } catch (Exception e) {
            return null;
        }
    }

    // ===== Path Operations =====

    /**
     * Join path components into a single path string.
     * @param parts Path components to join
     * @return Joined path string
     */
    public String joinPath(String... parts) {
        if (parts == null || parts.length == 0) return "";
        return Paths.get("", parts).normalize().toString();
    }

    /**
     * Get the filename from a path.
     * @param filePath Path to extract filename from
     * @return Filename or empty string if extraction fails
     */
    public String getFileName(String filePath) {
        try {
            Path path = safeResolve(filePath);
            Path fileName = path.getFileName();
            return fileName != null ? fileName.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ===== JSON Operations =====

    /**
     * Read a JSON file and parse it into an Object.
     * @param filePath Path to the JSON file relative to sandbox root
     * @return FileOperationResult containing the parsed JSON object
     */
    public FileOperationResult readJson(String filePath) {
        try {
            FileOperationResult readResult = read(filePath);
            if (!readResult.isSuccess()) return readResult;
            String content = readResult.getAsString();
            if (content == null || content.trim().isEmpty()) {
                return FileOperationResult.error("File is empty or null");
            }
            Object jsonData = objectMapper.readValue(content, Object.class);
            return FileOperationResult.success(jsonData);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read JSON (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    /**
     * Write an object as JSON to a file.
     * @param filePath Path to the JSON file relative to sandbox root
     * @param data Object to serialize as JSON
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult writeJson(String filePath, Object data) {
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            return write(filePath, jsonString, false);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write JSON (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    /**
     * Append an element to a JSON array file. If file doesn't exist or is not an array,
     * creates a new array with the element.
     * @param filePath Path to the JSON file relative to sandbox root
     * @param newElement Element to append to the array
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult appendJson(String filePath, Object newElement) {
        try {
            List<Object> jsonList;
            if (exists(filePath)) {
                FileOperationResult readResult = readJsonList(filePath);
                jsonList = readResult.isSuccess() ? readResult.getAs(List.class) : new ArrayList<>();
            } else {
                jsonList = new ArrayList<>();
            }
            jsonList.add(newElement);
            return writeJson(filePath, jsonList);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to append to JSON array (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    // Helper method for appendJson - not exposed to plugins
    private FileOperationResult readJsonList(String filePath) {
        try {
            FileOperationResult readResult = read(filePath);
            if (!readResult.isSuccess()) return readResult;
            String content = readResult.getAsString();
            if (content == null || content.trim().isEmpty()) {
                return FileOperationResult.error("File is empty or null");
            }
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, Object.class);
            List<Object> jsonData = objectMapper.readValue(content, listType);
            return FileOperationResult.success(jsonData);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read JSON as List (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    // ===== CSV Operations =====

    /**
     * Read a CSV file with headers. Returns a list of maps where keys are column headers.
     * @param filePath Path to the CSV file relative to sandbox root
     * @return FileOperationResult containing List&lt;Map&lt;String, String&gt;&gt; of CSV records
     */
    public FileOperationResult readCsv(String filePath) {
        try {
            Path path = safeResolve(filePath);
            if (!Files.exists(path)) return FileOperationResult.success(new ArrayList<>());
            List<Map<String, String>> records = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
                String headerLine = null;
                String line;
                List<String> headers = null;

                if ((headerLine = br.readLine()) != null) {
                    headers = parseCsvLine(headerLine);
                }

                while ((line = br.readLine()) != null) {
                    List<String> values = parseCsvLine(line);
                    Map<String, String> rec = new LinkedHashMap<>();
                    if (headers != null) {
                        for (int i = 0; i < values.size(); i++) {
                            String key = i < headers.size() ? headers.get(i).trim() : "column_" + i;
                            rec.put(key, values.get(i));
                        }
                    } else {
                        for (int i = 0; i < values.size(); i++) {
                            rec.put("column_" + i, values.get(i));
                        }
                    }
                    records.add(rec);
                }
            }

            return FileOperationResult.success(records);
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read CSV (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    /**
     * Write data to a CSV file with headers.
     * @param filePath Path to the CSV file relative to sandbox root
     * @param headers List of column headers
     * @param rows List of data rows (each row is a list of values)
     * @param append If true, append rows to existing file; if false, overwrite
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult writeCsv(String filePath, List<?> headers, List<List<?>> rows, boolean append) {
        try {
            StringBuilder csv = new StringBuilder();

            // Only write headers if not appending or file doesn't exist
            if (!append || !exists(filePath)) {
                if (headers != null && !headers.isEmpty()) {
                    List<String> headerStrings = headers.stream()
                            .map(v -> escapeCsvValue(String.valueOf(v)))
                            .collect(Collectors.toList());
                    csv.append(String.join(",", headerStrings)).append("\n");
                }
            }

            if (rows != null) {
                for (List<?> row : rows) {
                    if (row != null) {
                        List<String> escapedRow = row.stream()
                                .map(v -> escapeCsvValue(String.valueOf(v)))
                                .collect(Collectors.toList());
                        csv.append(String.join(",", escapedRow)).append("\n");
                    }
                }
            }

            return write(filePath, csv.toString(), append);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write CSV (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    /**
     * Append rows to a CSV file. Assumes file already has headers if it exists.
     * @param filePath Path to the CSV file relative to sandbox root
     * @param rows List of data rows to append
     * @return FileOperationResult indicating success or failure
     */
    public FileOperationResult appendCsv(String filePath, List<List<?>> rows) {
        return writeCsv(filePath, null, rows, true);
    }

    private List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;

        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    // escaped double quote
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    out.add(cur.toString());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        out.add(cur.toString());
        return out;
    }

    private String escapeCsvValue(String value) {
        if (value == null) return "";
        boolean mustQuote = value.contains("\n") || value.contains("\r") || value.contains("\"") || value.contains(",");
        if (mustQuote) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
