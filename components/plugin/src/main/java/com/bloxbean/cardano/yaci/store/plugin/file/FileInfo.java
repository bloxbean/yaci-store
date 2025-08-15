package com.bloxbean.cardano.yaci.store.plugin.file;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class FileInfo {
    private final String path;
    private final String name;
    private final long size;
    private final long lastModified;
    private final boolean isDirectory;
    private final boolean isRegularFile;
    private final boolean exists;
    private final boolean readable;
    private final boolean writable;

    public FileInfo(Path path, long size, long lastModified, boolean isDirectory, 
                   boolean isRegularFile, boolean exists, boolean readable, boolean writable) {
        this.path = path.toString();
        this.name = path.getFileName() != null ? path.getFileName().toString() : "";
        this.size = size;
        this.lastModified = lastModified;
        this.isDirectory = isDirectory;
        this.isRegularFile = isRegularFile;
        this.exists = exists;
        this.readable = readable;
        this.writable = writable;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public long getLastModified() {
        return lastModified;
    }

    public LocalDateTime getLastModifiedDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModified), ZoneId.systemDefault());
    }

    public String getLastModifiedString() {
        return getLastModifiedDateTime().toString();
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public boolean isFile() {
        return isRegularFile;
    }

    public boolean exists() {
        return exists;
    }

    public boolean isReadable() {
        return readable;
    }

    public boolean isWritable() {
        return writable;
    }

    public String getExtension() {
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            return name.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }

    public String getBaseName() {
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0) {
            return name.substring(0, lastDot);
        }
        return name;
    }

    @Override
    public String toString() {
        return String.format("FileInfo{path='%s', name='%s', size=%d, isDirectory=%s, exists=%s}", 
                            path, name, size, isDirectory, exists);
    }
}