package com.bloxbean.cardano.yaci.store.plugin.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Component
public class PluginFileClient {
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ===== Basic File Operations =====

    public FileOperationResult read(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return FileOperationResult.error("File does not exist: " + filePath);
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            return FileOperationResult.success(content);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read file: " + e.getMessage());
        }
    }

    public FileOperationResult readBytes(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return FileOperationResult.error("File does not exist: " + filePath);
            }

            byte[] content = Files.readAllBytes(path);
            return FileOperationResult.success(content);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read file bytes: " + e.getMessage());
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
            Path path = Paths.get(filePath);

            // Create parent directories if they don't exist
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            if (append) {
                Files.writeString(path, content, StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, content, StandardCharsets.UTF_8,
                                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            return FileOperationResult.success();
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write file: " + e.getMessage());
        }
    }

    public FileOperationResult writeBytes(String filePath, byte[] content) {
        return writeBytes(filePath, content, false);
    }

    public FileOperationResult writeBytes(String filePath, byte[] content, boolean append) {
        try {
            Path path = Paths.get(filePath);

            // Create parent directories if they don't exist
            Path parent = path.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            if (append) {
                Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.write(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }

            return FileOperationResult.success();
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write file bytes: " + e.getMessage());
        }
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
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return FileOperationResult.error("File does not exist: " + filePath);
            }

            boolean deleted = Files.deleteIfExists(path);
            if (deleted) {
                return FileOperationResult.success();
            } else {
                return FileOperationResult.error("Failed to delete file: " + filePath);
            }
        } catch (Exception e) {
            return FileOperationResult.error("Failed to delete file: " + e.getMessage());
        }
    }

    public FileOperationResult copy(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);

            if (!Files.exists(source)) {
                return FileOperationResult.error("Source file does not exist: " + sourcePath);
            }

            // Create parent directories if they don't exist
            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return FileOperationResult.success();
        } catch (Exception e) {
            return FileOperationResult.error("Failed to copy file: " + e.getMessage());
        }
    }

    public FileOperationResult move(String sourcePath, String targetPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetPath);

            if (!Files.exists(source)) {
                return FileOperationResult.error("Source file does not exist: " + sourcePath);
            }

            // Create parent directories if they don't exist
            Path parent = target.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            return FileOperationResult.success();
        } catch (Exception e) {
            return FileOperationResult.error("Failed to move file: " + e.getMessage());
        }
    }

    public boolean exists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }

    public FileOperationResult getInfo(String filePath) {
        try {
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                FileInfo info = new FileInfo(path, 0, 0, false, false, false, false, false);
                return FileOperationResult.success(info);
            }

            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);

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
        } catch (Exception e) {
            return FileOperationResult.error("Failed to get file info: " + e.getMessage());
        }
    }

    // ===== Directory Operations =====

    public FileOperationResult createDir(String dirPath) {
        try {
            Path path = Paths.get(dirPath);
            Files.createDirectories(path);
            return FileOperationResult.success();
        } catch (Exception e) {
            return FileOperationResult.error("Failed to create directory: " + e.getMessage());
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
            Path path = Paths.get(dirPath);

            if (!Files.exists(path)) {
                return DirectoryListing.error("Directory does not exist: " + dirPath);
            }

            if (!Files.isDirectory(path)) {
                return DirectoryListing.error("Path is not a directory: " + dirPath);
            }

            List<FileInfo> files = new ArrayList<>();

            if (recursive) {
                try (Stream<Path> walk = Files.walk(path)) {
                    files = walk.filter(p -> !p.equals(path))
                              .map(this::createFileInfo)
                              .filter(Objects::nonNull)
                              .collect(Collectors.toList());
                }
            } else {
                try (Stream<Path> list = Files.list(path)) {
                    files = list.map(this::createFileInfo)
                              .filter(Objects::nonNull)
                              .collect(Collectors.toList());
                }
            }

            return DirectoryListing.success(files);
        } catch (Exception e) {
            return DirectoryListing.error("Failed to list files: " + e.getMessage());
        }
    }

    public DirectoryListing listFilesRecursive(String dirPath) {
        return listFiles(dirPath, true);
    }

    private FileInfo createFileInfo(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
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
        if (parts.length == 0) {
            return "";
        }

        Path result = Paths.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            result = result.resolve(parts[i]);
        }

        return result.toString();
    }

    public String getParent(String filePath) {
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        return parent != null ? parent.toString() : null;
    }

    public String getFileName(String filePath) {
        Path path = Paths.get(filePath);
        Path fileName = path.getFileName();
        return fileName != null ? fileName.toString() : "";
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
        if (lastDot > 0) {
            return fileName.substring(0, lastDot);
        }
        return fileName;
    }

    public String getAbsolutePath(String filePath) {
        return Paths.get(filePath).toAbsolutePath().toString();
    }

    public String resolvePath(String basePath, String relativePath) {
        return Paths.get(basePath).resolve(relativePath).toString();
    }

    // ===== JSON Operations =====

    public FileOperationResult readJson(String filePath) {
        try {
            FileOperationResult readResult = read(filePath);
            if (!readResult.isSuccess()) {
                return readResult;
            }

            String content = readResult.getAsString();
            if (content == null || content.trim().isEmpty()) {
                return FileOperationResult.error("File is empty or null");
            }

            Object jsonData = objectMapper.readValue(content, Object.class);
            return FileOperationResult.success(jsonData);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read JSON: " + e.getMessage());
        }
    }

    public FileOperationResult readJsonMap(String filePath) {
        try {
            FileOperationResult readResult = read(filePath);
            if (!readResult.isSuccess()) {
                return readResult;
            }

            String content = readResult.getAsString();
            if (content == null || content.trim().isEmpty()) {
                return FileOperationResult.error("File is empty or null");
            }

            Map<String, Object> jsonData = objectMapper.readValue(content, Map.class);
            return FileOperationResult.success(jsonData);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read JSON as Map: " + e.getMessage());
        }
    }

    public FileOperationResult readJsonList(String filePath) {
        try {
            FileOperationResult readResult = read(filePath);
            if (!readResult.isSuccess()) {
                return readResult;
            }

            String content = readResult.getAsString();
            if (content == null || content.trim().isEmpty()) {
                return FileOperationResult.error("File is empty or null");
            }

            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, Object.class);
            List<Object> jsonData = objectMapper.readValue(content, listType);
            return FileOperationResult.success(jsonData);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read JSON as List: " + e.getMessage());
        }
    }

    public FileOperationResult writeJson(String filePath, Object data) {
        try {
            String jsonString = objectMapper.writeValueAsString(data);
            return write(filePath, jsonString, false);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write JSON: " + e.getMessage());
        }
    }

    public FileOperationResult appendJson(String filePath, Object newElement) {
        try {
            // Read existing JSON array or create new one
            List<Object> jsonList;

            if (exists(filePath)) {
                FileOperationResult readResult = readJsonList(filePath);
                if (readResult.isSuccess()) {
                    jsonList = readResult.getAs(List.class);
                } else {
                    // If file exists but not valid JSON array, try empty array
                    jsonList = new ArrayList<>();
                }
            } else {
                jsonList = new ArrayList<>();
            }

            // Add new element
            jsonList.add(newElement);

            // Write back to file
            return writeJson(filePath, jsonList);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to append to JSON array: " + e.getMessage());
        }
    }

    // ===== CSV Operations =====

    public FileOperationResult readCsv(String filePath) {
        return readCsv(filePath, ",", true);
    }

    public FileOperationResult readCsv(String filePath, String delimiter, boolean hasHeaders) {
        try {
            FileOperationResult readResult = read(filePath);
            if (!readResult.isSuccess()) {
                return readResult;
            }

            String content = readResult.getAsString();
            if (content == null || content.trim().isEmpty()) {
                return FileOperationResult.success(new ArrayList<>());
            }

            String[] lines = content.split("\n");
            List<Map<String, String>> records = new ArrayList<>();

            if (lines.length == 0) {
                return FileOperationResult.success(records);
            }

            String[] headers = null;
            int startRow = 0;

            if (hasHeaders && lines.length > 0) {
                headers = lines[0].split(delimiter);
                startRow = 1;
            }

            for (int i = startRow; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                String[] values = line.split(delimiter);
                Map<String, String> record = new LinkedHashMap<>();

                for (int j = 0; j < values.length; j++) {
                    String key = hasHeaders && headers != null && j < headers.length
                               ? headers[j].trim()
                               : "column_" + j;
                    String value = values[j].trim();

                    // Remove quotes if present
                    if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                        value = value.substring(1, value.length() - 1);
                    }

                    record.put(key, value);
                }

                records.add(record);
            }

            return FileOperationResult.success(records);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to read CSV: " + e.getMessage());
        }
    }

    public FileOperationResult writeCsv(String filePath, List<String> headers, List<List<String>> rows) {
        return writeCsv(filePath, headers, rows, ",", false);
    }

    public FileOperationResult writeCsv(String filePath, List<String> headers, List<List<String>> rows, String delimiter, boolean append) {
        try {
            StringBuilder csv = new StringBuilder();

            // Only write headers if not appending or file doesn't exist
            if (!append || !exists(filePath)) {
                if (headers != null && !headers.isEmpty()) {
                    csv.append(String.join(delimiter, headers)).append("\n");
                }
            }

            // Write rows
            if (rows != null) {
                for (List<String> row : rows) {
                    if (row != null) {
                        List<String> escapedRow = row.stream()
                                                     .map(this::escapeCsvValue)
                                                     .collect(Collectors.toList());
                        csv.append(String.join(delimiter, escapedRow)).append("\n");
                    }
                }
            }

            return write(filePath, csv.toString(), append);
        } catch (Exception e) {
            return FileOperationResult.error("Failed to write CSV: " + e.getMessage());
        }
    }

    public FileOperationResult appendCsv(String filePath, List<List<String>> rows) {
        return appendCsv(filePath, rows, ",");
    }

    public FileOperationResult appendCsv(String filePath, List<List<String>> rows, String delimiter) {
        return writeCsv(filePath, null, rows, delimiter, true);
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }

        // If value contains delimiter, newline, or quotes, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }

    // ===== Archive Operations =====

    public FileOperationResult zip(String sourceDir, String zipFilePath) {
        try {
            Path sourcePath = Paths.get(sourceDir);
            Path zipPath = Paths.get(zipFilePath);

            if (!Files.exists(sourcePath)) {
                return FileOperationResult.error("Source directory does not exist: " + sourceDir);
            }

            // Create parent directories if they don't exist
            Path parent = zipPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                Files.walk(sourcePath)
                     .filter(path -> !Files.isDirectory(path))
                     .forEach(path -> {
                         ZipEntry zipEntry = new ZipEntry(sourcePath.relativize(path).toString());
                         try {
                             zos.putNextEntry(zipEntry);
                             Files.copy(path, zos);
                             zos.closeEntry();
                         } catch (Exception e) {
                             // Log error but continue with other files
                         }
                     });
            }

            return FileOperationResult.success();
        } catch (Exception e) {
            return FileOperationResult.error("Failed to create zip archive: " + e.getMessage());
        }
    }

    public FileOperationResult unzip(String zipFilePath, String targetDir) {
        try {
            Path zipPath = Paths.get(zipFilePath);
            Path targetPath = Paths.get(targetDir);

            if (!Files.exists(zipPath)) {
                return FileOperationResult.error("Zip file does not exist: " + zipFilePath);
            }

            Files.createDirectories(targetPath);

            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
                ZipEntry zipEntry = zis.getNextEntry();

                while (zipEntry != null) {
                    Path newFile = targetPath.resolve(zipEntry.getName());

                    // Security check to prevent zip slip
                    if (!newFile.normalize().startsWith(targetPath.normalize())) {
                        return FileOperationResult.error("Zip entry is outside target directory: " + zipEntry.getName());
                    }

                    if (zipEntry.isDirectory()) {
                        Files.createDirectories(newFile);
                    } else {
                        // Create parent directories
                        Files.createDirectories(newFile.getParent());
                        Files.copy(zis, newFile, StandardCopyOption.REPLACE_EXISTING);
                    }

                    zipEntry = zis.getNextEntry();
                }

                zis.closeEntry();
            }

            return FileOperationResult.success();
        } catch (Exception e) {
            return FileOperationResult.error("Failed to extract zip archive: " + e.getMessage());
        }
    }

    // ===== Utility Methods =====

    public FileOperationResult createTempFile() {
        return createTempFile("temp", ".tmp");
    }

    public FileOperationResult createTempFile(String prefix, String suffix) {
        try {
            Path tempFile = Files.createTempFile(prefix, suffix);
            return FileOperationResult.success(tempFile.toString());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to create temp file: " + e.getMessage());
        }
    }

    public FileOperationResult createTempDirectory() {
        return createTempDirectory("temp");
    }

    public FileOperationResult createTempDirectory(String prefix) {
        try {
            Path tempDir = Files.createTempDirectory(prefix);
            return FileOperationResult.success(tempDir.toString());
        } catch (Exception e) {
            return FileOperationResult.error("Failed to create temp directory: " + e.getMessage());
        }
    }

    public long getFileSize(String filePath) {
        try {
            return Files.size(Paths.get(filePath));
        } catch (Exception e) {
            return -1;
        }
    }

    public boolean isDirectory(String filePath) {
        return Files.isDirectory(Paths.get(filePath));
    }

    public boolean isFile(String filePath) {
        return Files.isRegularFile(Paths.get(filePath));
    }

    public boolean isReadable(String filePath) {
        return Files.isReadable(Paths.get(filePath));
    }

    public boolean isWritable(String filePath) {
        return Files.isWritable(Paths.get(filePath));
    }
}
