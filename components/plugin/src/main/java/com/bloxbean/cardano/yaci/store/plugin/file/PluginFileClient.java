package com.bloxbean.cardano.yaci.store.plugin.file;

import com.bloxbean.cardano.yaci.store.common.config.StoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
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
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Secure, sandboxed file API for plugin scripts (MVEL/JS/Python).
 * - All paths are confined under {@code sandboxRoot}.
 * - Non-append writes are atomic (temp + ATOMIC_MOVE) for crash safety.
 * - CSV parsing supports quoted fields and CRLF.
 * - NDJSON append for scalable event logs.
 * - Optional advisory locking to avoid interleaved writes.
 */
@Component
public class PluginFileClient {
    private static final long ATOMIC_WRITE_THRESHOLD_BYTES = 16 * 1024 * 1024; // 16 MB

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectWriter prettyWriter = objectMapper.writerWithDefaultPrettyPrinter();

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

    // ===== Basic File Operations =====

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

    public FileOperationResult readBytes(String filePath) {
        try {
            Path path = safeResolve(filePath);
            if (!Files.exists(path)) {
                return FileOperationResult.error("File does not exist: " + filePath);
            }
            byte[] content = Files.readAllBytes(path);
            return FileOperationResult.success(content);
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read file bytes (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    public FileOperationResult readText(String filePath) {
        return read(filePath);
    }

    public FileOperationResult write(String filePath, String content) {
        return write(filePath, content, false);
    }

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

    public FileOperationResult writeBytes(String filePath, byte[] content) {
        return writeBytes(filePath, content, false);
    }

    public FileOperationResult writeBytes(String filePath, byte[] content, boolean append) {
        try {
            Path path = safeResolve(filePath);
            return doWriteBytes(path, content, append);
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write file bytes (" + e.getClass().getSimpleName() + "): " + e.getMessage());
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

    public FileOperationResult writeText(String filePath, String content) {
        return write(filePath, content, false);
    }

    public FileOperationResult append(String filePath, String content) {
        return write(filePath, content, true);
    }

    public FileOperationResult appendText(String filePath, String content) {
        return write(filePath, content, true);
    }

    public FileOperationResult appendBytes(String filePath, byte[] content) {
        return writeBytes(filePath, content, true);
    }

    public FileOperationResult delete(String filePath) {
        try {
            Path path = safeResolve(filePath);
            boolean deleted = Files.deleteIfExists(path);
            if (deleted) return FileOperationResult.success();
            return FileOperationResult.error("Failed to delete file: " + filePath);
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to delete file (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    public FileOperationResult copy(String sourcePath, String targetPath) {
        try {
            Path source = safeResolve(sourcePath);
            Path target = safeResolve(targetPath);
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return FileOperationResult.success();
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to copy file (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    public FileOperationResult move(String sourcePath, String targetPath) {
        try {
            Path source = safeResolve(sourcePath);
            Path target = safeResolve(targetPath);
            Files.createDirectories(target.getParent());
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return FileOperationResult.success();
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to move file (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    public boolean exists(String filePath) {
        try {
            Path p = safeResolve(filePath);
            return Files.exists(p);
        } catch (Exception e) {
            return false;
        }
    }

    public FileOperationResult getInfo(String filePath) {
        try {
            Path path = safeResolve(filePath);
            if (!Files.exists(path)) {
                FileInfo info = new FileInfo(path, 0, 0, false, false, false, false, false);
                return FileOperationResult.success(info);
            }
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            FileInfo info = new FileInfo(
                    path,
                    attrs.size(),
                    attrs.lastModifiedTime().toMillis(),
                    attrs.isDirectory(),
                    attrs.isRegularFile(),
                    true,
                    Files.isReadable(path),
                    Files.isWritable(path)
            );
            return FileOperationResult.success(info);
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to get file info (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    // ===== Directory Operations =====

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

    public FileOperationResult createDirectory(String dirPath) {
        return createDir(dirPath);
    }

    public DirectoryListing listFiles(String dirPath) {
        return listFiles(dirPath, false);
    }

    public DirectoryListing listFiles(String dirPath, boolean recursive) {
        try {
            Path path = safeResolve(dirPath);
            if (!Files.exists(path)) {
                return DirectoryListing.error("Directory does not exist: " + dirPath);
            }
            if (!Files.isDirectory(path)) {
                return DirectoryListing.error("Path is not a directory: " + dirPath);
            }

            List<FileInfo> files;
            if (recursive) {
                try (Stream<Path> walk = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) {
                    files = walk.filter(p -> !p.equals(path))
                            .map(this::createFileInfoQuiet)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
            } else {
                try (Stream<Path> list = Files.list(path)) {
                    files = list.map(this::createFileInfoQuiet)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
            }
            return DirectoryListing.success(files);
        } catch (SecurityException se) {
            return DirectoryListing.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return DirectoryListing.error("Failed to list files (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    public DirectoryListing listFilesRecursive(String dirPath) {
        return listFiles(dirPath, true);
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

    public String joinPath(String... parts) {
        if (parts == null || parts.length == 0) return "";
        return Paths.get("", parts).normalize().toString();
    }

    public String getParent(String filePath) {
        try {
            Path path = safeResolve(filePath);
            Path parent = path.getParent();
            return parent != null ? parent.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getFileName(String filePath) {
        try {
            Path path = safeResolve(filePath);
            Path fileName = path.getFileName();
            return fileName != null ? fileName.toString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public String getExtension(String filePath) {
        String fileName = getFileName(filePath);
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    public String getBaseName(String filePath) {
        String fileName = getFileName(filePath);
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) return fileName.substring(0, lastDot);
        return fileName;
    }

    public String getAbsolutePath(String filePath) {
        try {
            return safeResolve(filePath).toString();
        } catch (Exception e) {
            return "";
        }
    }

    public String resolvePath(String basePath, String relativePath) {
        try {
            Path base = safeResolve(basePath);
            Path resolved = base.resolve(relativePath).normalize();
            // Re-check sandbox after resolve
            if (!resolved.toRealPath(LinkOption.NOFOLLOW_LINKS).startsWith(realRoot)) {
                throw new SecurityException("Resolved path escapes sandbox");
            }
            return resolved.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public String relativize(String basePath, String childPath) {
        try {
            Path base = safeResolve(basePath);
            Path child = safeResolve(childPath);
            return base.relativize(child).toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ===== JSON Operations =====

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

    public FileOperationResult readJsonMap(String filePath) {
        try {
            FileOperationResult readResult = read(filePath);
            if (!readResult.isSuccess()) return readResult;
            String content = readResult.getAsString();
            if (content == null || content.trim().isEmpty()) {
                return FileOperationResult.error("File is empty or null");
            }
            Map<String, Object> jsonData = objectMapper.readValue(content, Map.class);
            return FileOperationResult.success(jsonData);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read JSON as Map (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    public FileOperationResult readJsonList(String filePath) {
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

    public FileOperationResult writeJson(String filePath, Object data) {
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            return write(filePath, jsonString, false);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write JSON (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    public FileOperationResult writeJsonPretty(String filePath, Object data) {
        try {
            String jsonString = prettyWriter.writeValueAsString(data);
            return write(filePath, jsonString, false);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write pretty JSON (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

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

    /** Append one JSON object per line (NDJSON). Scales well for logs. */
    public FileOperationResult appendJsonLine(String filePath, Object elem) {
        try {
            String line = objectMapper.writeValueAsString(elem) + "\n";
            return write(filePath, line, true);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to append JSON line (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    // ===== CSV Operations =====
    // Robust parsing for quoted fields + CRLF; writing auto-converts value types to String

    public FileOperationResult readCsv(String filePath) {
        return readCsv(filePath, ",", true);
    }

    public FileOperationResult readCsv(String filePath, String delimiter, boolean hasHeaders) {
        try {
            Path path = safeResolve(filePath);
            if (!Files.exists(path)) return FileOperationResult.success(new ArrayList<>());
            List<Map<String, String>> records = new ArrayList<>();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
                String headerLine = null;
                String line;
                List<String> headers = null;

                if (hasHeaders && (headerLine = br.readLine()) != null) {
                    headers = parseCsvLine(headerLine, delimiter);
                }

                while ((line = br.readLine()) != null) {
                    List<String> values = parseCsvLine(line, delimiter);
                    Map<String, String> rec = new LinkedHashMap<>();
                    if (hasHeaders && headers != null) {
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

    public FileOperationResult writeCsv(String filePath, List<?> headers, List<List<?>> rows) {
        return writeCsv(filePath, headers, rows, ",", false);
    }

    public FileOperationResult writeCsv(String filePath, List<?> headers, List<List<?>> rows, String delimiter, boolean append) {
        try {
            StringBuilder csv = new StringBuilder();

            // Only write headers if not appending or file doesn't exist
            if (!append || !exists(filePath)) {
                if (headers != null && !headers.isEmpty()) {
                    List<String> headerStrings = headers.stream()
                            .map(v -> escapeCsvValue(String.valueOf(v), delimiter))
                            .collect(Collectors.toList());
                    csv.append(String.join(delimiter, headerStrings)).append("\n");
                }
            }

            if (rows != null) {
                for (List<?> row : rows) {
                    if (row != null) {
                        List<String> escapedRow = row.stream()
                                .map(v -> escapeCsvValue(String.valueOf(v), delimiter))
                                .collect(Collectors.toList());
                        csv.append(String.join(delimiter, escapedRow)).append("\n");
                    }
                }
            }

            return write(filePath, csv.toString(), append);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write CSV (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    public FileOperationResult appendCsv(String filePath, List<List<?>> rows) {
        return appendCsv(filePath, rows, ",");
    }

    public FileOperationResult appendCsv(String filePath, List<List<?>> rows, String delimiter) {
        return writeCsv(filePath, null, rows, delimiter, true);
    }

    private List<String> parseCsvLine(String line, String delimiter) {
        List<String> out = new ArrayList<>();
        if (line == null) return out;

        char delim = delimiter != null && delimiter.length() == 1 ? delimiter.charAt(0) : ',';
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
                } else if (c == delim) {
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

    private String escapeCsvValue(String value, String delimiter) {
        if (value == null) return "";
        boolean mustQuote = value.contains("\n") || value.contains("\r") || value.contains("\"")
                || (delimiter != null && delimiter.length() == 1 && value.indexOf(delimiter.charAt(0)) >= 0);
        if (mustQuote) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ===== Archive Operations =====

    public FileOperationResult zip(String sourceDir, String zipFilePath) {
        try {
            Path sourcePath = safeResolve(sourceDir);
            Path zipPath = safeResolve(zipFilePath);
            Files.createDirectories(zipPath.getParent());

            try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipPath)))) {
                final Path realSource = sourcePath.toRealPath(LinkOption.NOFOLLOW_LINKS);

                Files.walk(sourcePath) // do not follow symlinks implicitly
                        .filter(p -> !Files.isDirectory(p))
                        .forEach(p -> {
                            try {
                                // Skip symlinks or files escaping the source root
                                if (Files.isSymbolicLink(p)) return;
                                Path realP = p.toRealPath(LinkOption.NOFOLLOW_LINKS);
                                if (!realP.startsWith(realSource)) return;

                                ZipEntry zipEntry = new ZipEntry(realSource.relativize(realP).toString().replace('\\', '/'));
                                try {
                                    zipEntry.setTime(Files.getLastModifiedTime(realP).toMillis());
                                } catch (Exception ignore) { /* best effort */ }

                                zos.putNextEntry(zipEntry);
                                try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(realP))) {
                                    in.transferTo(zos);
                                }
                                zos.closeEntry();
                            } catch (Exception ignoredPerEntry) {
                                // Collecting per-entry errors would be nicer; keep silent to continue.
                            }
                        });
            }

            return FileOperationResult.success();
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to create zip archive (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    public FileOperationResult unzip(String zipFilePath, String targetDir) {
        try {
            Path zipPath = safeResolve(zipFilePath);
            Path targetPath = safeResolve(targetDir);
            Files.createDirectories(targetPath);

            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
                ZipEntry zipEntry;
                while ((zipEntry = zis.getNextEntry()) != null) {
                    Path newFile = targetPath.resolve(zipEntry.getName()).normalize();

                    // Prevent zip slip and ensure inside sandbox
                    Path realNewFileParent = newFile.getParent().toRealPath(LinkOption.NOFOLLOW_LINKS);
                    if (!realNewFileParent.startsWith(realRoot)) {
                        zis.closeEntry();
                        return FileOperationResult.error("Zip entry is outside target directory: " + zipEntry.getName());
                    }

                    if (zipEntry.isDirectory()) {
                        Files.createDirectories(newFile);
                    } else {
                        Files.createDirectories(newFile.getParent());
                        // Refuse to write through an existing symlink
                        if (Files.exists(newFile) && Files.isSymbolicLink(newFile)) {
                            zis.closeEntry();
                            return FileOperationResult.error("Refusing to overwrite symlink: " + zipEntry.getName());
                        }
                        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(newFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
                            zis.transferTo(out);
                        }
                    }
                    zis.closeEntry();
                }
            }

            return FileOperationResult.success();
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to extract zip archive (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    // ===== Utility Methods =====

    public FileOperationResult createTempFile() {
        return createTempFile("temp", ".tmp");
    }

    public FileOperationResult createTempFile(String prefix, String suffix) {
        try {
            Path tmp = Files.createTempFile(safeResolve("."), prefix, suffix);
            return FileOperationResult.success(tmp.toString());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to create temp file (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    public FileOperationResult createTempDirectory() {
        return createTempDirectory("temp");
    }

    public FileOperationResult createTempDirectory(String prefix) {
        try {
            Path tmp = Files.createTempDirectory(safeResolve("."), prefix);
            return FileOperationResult.success(tmp.toString());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to create temp directory (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    public long getFileSize(String filePath) {
        try {
            return Files.size(safeResolve(filePath));
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean isDirectory(String filePath) {
        try {
            return Files.isDirectory(safeResolve(filePath));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isFile(String filePath) {
        try {
            return Files.isRegularFile(safeResolve(filePath));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isReadable(String filePath) {
        try {
            return Files.isReadable(safeResolve(filePath));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isWritable(String filePath) {
        try {
            return Files.isWritable(safeResolve(filePath));
        } catch (Exception e) {
            return false;
        }
    }

    /** Glob helper: pattern like "*.csv" */
    public List<String> glob(String dirPath, String pattern) {
        try {
            Path dir = safeResolve(dirPath);
            if (!Files.isDirectory(dir)) return List.of();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, pattern)) {
                List<String> out = new ArrayList<>();
                for (Path p : ds) out.add(p.toString());
                return out;
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    /** Read up to {@code limit} lines to avoid huge allocations. */
    public FileOperationResult readLines(String filePath, int limit) {
        try {
            Path path = safeResolve(filePath);
            if (!Files.exists(path)) return FileOperationResult.success(List.of());
            List<String> lines = new ArrayList<>(Math.max(0, limit));
            try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8))) {
                String line;
                int count = 0;
                while ((line = br.readLine()) != null && (limit <= 0 || count++ < limit)) {
                    lines.add(line);
                }
            }
            return FileOperationResult.success(lines);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read lines (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    /** Delete a directory; if recursive=false, only empty dirs are removed. */
    public FileOperationResult deleteDir(String dirPath, boolean recursive) {
        try {
            Path dir = safeResolve(dirPath);
            if (!Files.exists(dir)) return FileOperationResult.success();
            if (!recursive) {
                Files.delete(dir);
            } else {
                // Depth-first delete
                try (Stream<Path> walk = Files.walk(dir)) {
                    List<Path> list = walk.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
                    for (Path p : list) {
                        Files.deleteIfExists(p);
                    }
                }
            }
            return FileOperationResult.success();
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to delete directory (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    // ===== Streaming helper (optional large writes) =====

    /** Stream large content into a file (non-atomic). Consumer should write bytes to the provided OutputStream. */
    public FileOperationResult writeLarge(String filePath, Consumer<BufferedOutputStream> writer) {
        try {
            Path path = safeResolve(filePath);
            Files.createDirectories(path.getParent());
            ReentrantLock localLock = acquireLocalLock(path);
            try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(path,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE))) {
                writer.accept(out);
            } finally {
                releaseLocalLock(localLock);
            }
            return FileOperationResult.success();
        } catch (SecurityException se) {
            return FileOperationResult.error("Access denied: " + se.getMessage());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write large file (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }
}
