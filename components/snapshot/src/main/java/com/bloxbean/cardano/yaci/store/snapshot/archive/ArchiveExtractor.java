package com.bloxbean.cardano.yaci.store.snapshot.archive;

import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Extracts snapshot parts into a work directory.
 *
 * <p>A snapshot arrives from outside the operator's control, so extraction is hostile-input
 * handling: absolute paths, {@code ..} traversal, entries resolving outside the work directory,
 * duplicate entries, symbolic links and entries larger than the manifest declares are all rejected
 * before a single byte is written.
 */
public class ArchiveExtractor {

    private static final Logger log = LoggerFactory.getLogger(ArchiveExtractor.class);

    private final Path workDir;
    private final long maxEntries;
    private final long maxTotalBytes;

    public ArchiveExtractor(Path workDir, long maxEntries, long maxTotalBytes) {
        this.workDir = workDir.toAbsolutePath().normalize();
        this.maxEntries = maxEntries;
        this.maxTotalBytes = maxTotalBytes;
    }

    /**
     * @param expectedSizes declared size per entry path from the manifest; an entry not present here,
     *                      or whose real size differs, is rejected
     * @return number of entries extracted
     */
    public int extract(Path zipFile, Map<String, Long> expectedSizes, Consumer<String> progress) throws IOException {
        Files.createDirectories(workDir);
        Set<String> seen = new HashSet<>();
        long totalBytes = 0;
        int count = 0;

        try (InputStream fin = Files.newInputStream(zipFile);
             ZipInputStream zin = new ZipInputStream(fin)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                String name = entry.getName();
                validateName(name);
                if (entry.isDirectory()) {
                    throw new IOException("Snapshot part contains a directory entry: " + name);
                }
                if (!seen.add(name)) {
                    throw new IOException("Duplicate entry in snapshot part: " + name);
                }
                if (++count > maxEntries) {
                    throw new IOException("Snapshot part exceeds the configured entry limit of " + maxEntries);
                }
                Long expected = expectedSizes.get(name);
                if (expected == null) {
                    throw new IOException("Entry '" + name + "' is not declared in the manifest");
                }

                Path target = workDir.resolve(name).normalize();
                if (!target.startsWith(workDir)) {
                    throw new IOException("Entry resolves outside the work directory: " + name);
                }
                Files.createDirectories(target.getParent());
                if (Files.isSymbolicLink(target)) {
                    throw new IOException("Refusing to write through an existing symbolic link: " + target);
                }

                long written = Files.copy(zin, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                totalBytes += written;
                if (written != expected) {
                    throw new IOException("Entry '" + name + "' is " + written
                            + " bytes but the manifest declares " + expected);
                }
                if (totalBytes > maxTotalBytes) {
                    throw new IOException("Extraction exceeds the configured size limit of " + maxTotalBytes + " bytes");
                }
                if (progress != null) {
                    progress.accept(name);
                }
            }
        }
        log.debug("Extracted {} entries ({} bytes) from {}", count, totalBytes, zipFile.getFileName());
        return count;
    }

    /** Entry names present in a part, without extracting anything. */
    public static Set<String> listEntries(Path zipFile) throws IOException {
        Set<String> names = new HashSet<>();
        try (ZipFile zf = new ZipFile(zipFile.toFile())) {
            zf.stream().forEach(e -> names.add(e.getName()));
        }
        return names;
    }

    static void validateName(String name) throws IOException {
        if (name == null || name.isBlank()) {
            throw new IOException("Snapshot part contains an entry with an empty name");
        }
        if (name.startsWith("/") || name.startsWith("\\") || name.contains(":")) {
            throw new IOException("Absolute or drive-qualified entry path rejected: " + name);
        }
        if (name.contains("\0")) {
            throw new IOException("Entry name contains a NUL byte");
        }
        for (String segment : name.split("/")) {
            if (segment.equals("..")) {
                throw new IOException("Path traversal entry rejected: " + name);
            }
        }
    }

    /** Verify an extracted file against its manifest digest. */
    public boolean verifyFile(String relativePath, String expectedSha256) throws IOException {
        Path p = workDir.resolve(relativePath);
        if (!Files.isRegularFile(p)) {
            return false;
        }
        return Digests.sha256Hex(p).equals(expectedSha256);
    }

    public Path workDir() {
        return workDir;
    }
}
