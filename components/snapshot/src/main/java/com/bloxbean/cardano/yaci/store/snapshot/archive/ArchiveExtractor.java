package com.bloxbean.cardano.yaci.store.snapshot.archive;

import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
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

    /** One part's extraction: entries written, bytes written and the SHA-256 of the part file. */
    public record Extracted(int entries, long bytes, String partSha256) {}

    /**
     * @param expectedSizes declared size per entry path from the manifest; an entry not present here,
     *                      or whose real size differs, is rejected
     * @return number of entries extracted
     */
    public int extract(Path zipFile, Map<String, Long> expectedSizes, Consumer<String> progress) throws IOException {
        return extract(zipFile, expectedSizes, null, progress).entries();
    }

    /**
     * Extracts a part in one sequential read. The part's SHA-256 is computed over the bytes as they
     * are read, and each entry's SHA-256 while it is written, so verifying both costs no extra pass.
     * An entry whose digest differs from {@code expectedDigests} fails the part. Safe to run for
     * different parts at the same time.
     *
     * @param expectedDigests declared SHA-256 per entry path, or null to skip entry digests
     */
    public Extracted extract(Path zipFile, Map<String, Long> expectedSizes, Map<String, String> expectedDigests,
                             Consumer<String> progress) throws IOException {
        Files.createDirectories(workDir);
        Set<String> seen = new HashSet<>();
        long totalBytes = 0;
        int count = 0;
        MessageDigest partDigest = Digests.sha256();
        byte[] buf = new byte[1 << 16];

        try (InputStream fin = new DigestInputStream(
                new BufferedInputStream(Files.newInputStream(zipFile), 1 << 20), partDigest);
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

                MessageDigest entryDigest = Digests.sha256();
                long written = 0;
                try (OutputStream out = Files.newOutputStream(target)) {
                    int r;
                    while ((r = zin.read(buf)) != -1) {
                        written += r;
                        // Stop as soon as an entry exceeds its declared size, before writing more.
                        if (written > expected) {
                            throw new IOException("Entry '" + name + "' is larger than the "
                                    + expected + " bytes the manifest declares");
                        }
                        entryDigest.update(buf, 0, r);
                        out.write(buf, 0, r);
                    }
                }
                totalBytes += written;
                if (written != expected) {
                    throw new IOException("Entry '" + name + "' is " + written
                            + " bytes but the manifest declares " + expected);
                }
                if (totalBytes > maxTotalBytes) {
                    throw new IOException("Extraction exceeds the configured size limit of " + maxTotalBytes + " bytes");
                }
                String expectedSha = expectedDigests == null ? null : expectedDigests.get(name);
                if (expectedSha != null && !Digests.hex(entryDigest.digest()).equals(expectedSha)) {
                    throw new IOException("Extracted file failed its digest check: " + name);
                }
                if (progress != null) {
                    progress.accept(name);
                }
            }
            // The central directory follows the last entry; read it so the part digest covers the file.
            while (fin.read(buf) != -1) {
                // digest only
            }
        }
        log.debug("Extracted {} entries ({} bytes) from {}", count, totalBytes, zipFile.getFileName());
        return new Extracted(count, totalBytes, Digests.hex(partDigest.digest()));
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
