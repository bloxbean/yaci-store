package com.bloxbean.cardano.yaci.store.plugin.file;

import java.util.List;
import java.util.stream.Collectors;

public class DirectoryListing {
    private final List<FileInfo> files;
    private final boolean success;
    private final String errorMessage;

    public DirectoryListing(List<FileInfo> files) {
        this.files = files;
        this.success = true;
        this.errorMessage = null;
    }

    public DirectoryListing(String errorMessage) {
        this.files = List.of();
        this.success = false;
        this.errorMessage = errorMessage;
    }

    public static DirectoryListing success(List<FileInfo> files) {
        return new DirectoryListing(files);
    }

    public static DirectoryListing error(String errorMessage) {
        return new DirectoryListing(errorMessage);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isError() {
        return !success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<FileInfo> getFiles() {
        return files;
    }

    public List<FileInfo> getDirectories() {
        return files.stream()
                   .filter(FileInfo::isDirectory)
                   .collect(Collectors.toList());
    }

    public List<FileInfo> getRegularFiles() {
        return files.stream()
                   .filter(FileInfo::isFile)
                   .collect(Collectors.toList());
    }

    public List<FileInfo> filterByExtension(String extension) {
        String ext = extension.toLowerCase();
        if (!ext.startsWith(".")) {
            ext = "." + ext;
        }
        final String finalExt = ext;
        return files.stream()
                   .filter(f -> f.getName().toLowerCase().endsWith(finalExt))
                   .collect(Collectors.toList());
    }

    public List<FileInfo> filterByName(String namePattern) {
        return files.stream()
                   .filter(f -> f.getName().matches(namePattern))
                   .collect(Collectors.toList());
    }

    public List<FileInfo> filterBySize(long minSize, long maxSize) {
        return files.stream()
                   .filter(f -> f.getSize() >= minSize && f.getSize() <= maxSize)
                   .collect(Collectors.toList());
    }

    public int size() {
        return files.size();
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    public long getTotalSize() {
        return files.stream()
                   .filter(FileInfo::isFile)
                   .mapToLong(FileInfo::getSize)
                   .sum();
    }

    public String getFormattedTotalSize() {
        long totalSize = getTotalSize();
        if (totalSize < 1024) {
            return totalSize + " B";
        } else if (totalSize < 1024 * 1024) {
            return String.format("%.1f KB", totalSize / 1024.0);
        } else if (totalSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", totalSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", totalSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    @Override
    public String toString() {
        if (!success) {
            return "DirectoryListing{error='" + errorMessage + "'}";
        }
        return String.format("DirectoryListing{files=%d, directories=%d, totalSize=%s}", 
                           getRegularFiles().size(), getDirectories().size(), getFormattedTotalSize());
    }
}