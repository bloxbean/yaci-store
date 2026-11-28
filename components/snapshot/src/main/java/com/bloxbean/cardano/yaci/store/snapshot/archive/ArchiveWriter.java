package com.bloxbean.cardano.yaci.store.snapshot.archive;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes a snapshot as a series of independent ZIP parts.
 *
 * <p>Independent parts rather than a spanned archive: each can be retried, mirrored and verified on
 * its own, and losing one does not make the others unreadable.
 *
 * <p>Entries use {@link ZipEntry#STORED} because DuckLake already writes Zstandard-compressed
 * Parquet; re-deflating costs hours of CPU for a fraction of a percent. A Parquet file is never
 * split, so a file larger than the target part size simply gets a part to itself.
 */
public class ArchiveWriter {

    private static final Logger log = LoggerFactory.getLogger(ArchiveWriter.class);
    public static final long DEFAULT_PART_SIZE = 8L * 1024 * 1024 * 1024;

    private final java.util.function.Function<String, Path> source;
    private final Path outputDir;
    private final String baseName;
    private final long partSizeBytes;

    public ArchiveWriter(Path dataDir, Path outputDir, String baseName, long partSizeBytes) {
        this(dataDir::resolve, outputDir, baseName, partSizeBytes);
    }

    /** @param source where each file is, by its path in the snapshot */
    public ArchiveWriter(java.util.function.Function<String, Path> source, Path outputDir, String baseName,
                         long partSizeBytes) {
        this.source = source;
        this.outputDir = outputDir;
        this.baseName = baseName;
        this.partSizeBytes = partSizeBytes > 0 ? partSizeBytes : DEFAULT_PART_SIZE;
    }

    /**
     * @param files    files to package, each identified by its path relative to {@code dataDir},
     *                 already digested during the pre-scan
     * @param owners   parallel list naming the spec that owns each file, recorded per part
     * @param progress called after every file
     */
    public List<SnapshotManifest.PartManifest> write(List<SnapshotManifest.FileEntry> files,
                                                     List<String> owners,
                                                     Consumer<String> progress) throws IOException {
        return write(files, owners, null, 1, progress);
    }

    /**
     * Writes the parts, several at once. Part boundaries depend only on the file sizes, so they are
     * planned first and each part is then an independent archive: the output is byte-identical
     * whatever the thread count. Each part's SHA-256 is computed while it is written.
     *
     * @param crcs    CRC-32 per file path from the pre-scan, or null to compute them here (one extra
     *                read per file)
     * @param threads parts written at once
     */
    public List<SnapshotManifest.PartManifest> write(List<SnapshotManifest.FileEntry> files,
                                                     List<String> owners, Map<String, Long> crcs,
                                                     int threads, Consumer<String> progress) throws IOException {
        return write(files, owners, null, crcs, threads, progress);
    }

    /**
     * As {@link #write(List, List, Map, int, Consumer)}, and a new part also starts wherever the group
     * changes. The exporter groups files by store module, so an import that skips a module never
     * needs a part that only carries its tables.
     *
     * @param groups parallel to {@code files}, or null for no group boundaries
     */
    public List<SnapshotManifest.PartManifest> write(List<SnapshotManifest.FileEntry> files,
                                                     List<String> owners, List<String> groups,
                                                     Map<String, Long> crcs, int threads,
                                                     Consumer<String> progress) throws IOException {
        if (files.size() != owners.size() || (groups != null && groups.size() != files.size())) {
            throw new IllegalArgumentException("files, owners and groups must be the same length");
        }
        Files.createDirectories(outputDir);

        // Never split a Parquet file: start a new part unless the current one is still empty.
        List<int[]> ranges = new ArrayList<>();
        int start = 0;
        long uncompressed = 0;
        for (int i = 0; i < files.size(); i++) {
            long size = files.get(i).size();
            boolean newGroup = groups != null && i > 0 && !groups.get(i).equals(groups.get(i - 1));
            if (i > start && (newGroup || uncompressed + size > partSizeBytes)) {
                ranges.add(new int[]{start, i});
                start = i;
                uncompressed = 0;
            }
            uncompressed += size;
        }
        if (start < files.size()) {
            ranges.add(new int[]{start, files.size()});
        }

        List<Integer> numbers = new ArrayList<>();
        for (int n = 1; n <= ranges.size(); n++) numbers.add(n);
        try {
            return Parallel.map(numbers, threads, n -> writePart(n, files.subList(ranges.get(n - 1)[0], ranges.get(n - 1)[1]),
                    owners.subList(ranges.get(n - 1)[0], ranges.get(n - 1)[1]), crcs, progress));
        } catch (Parallel.PartFailure e) {
            throw e.getCause() instanceof IOException io ? io : new IOException(e.getMessage(), e.getCause());
        }
    }

    private SnapshotManifest.PartManifest writePart(int partNumber, List<SnapshotManifest.FileEntry> files,
                                                    List<String> owners, Map<String, Long> crcs,
                                                    Consumer<String> progress) throws IOException {
        String fileName = String.format("%s.part-%05d.zip", baseName, partNumber);
        Path partPath = outputDir.resolve(fileName);
        Set<String> tables = new LinkedHashSet<>(owners);
        MessageDigest sha = Digests.sha256();
        try (OutputStream os = new DigestOutputStream(
                new BufferedOutputStream(Files.newOutputStream(partPath), 1 << 20), sha);
             ZipOutputStream zip = new ZipOutputStream(os)) {
            zip.setMethod(ZipOutputStream.STORED);
            for (SnapshotManifest.FileEntry fe : files) {
                Long crc = crcs == null ? null : crcs.get(fe.path());
                addStored(zip, fe, crc);
                if (progress != null) {
                    progress.accept(fe.path());
                }
            }
        }
        long size = Files.size(partPath);
        log.info("Wrote {} ({} entries, {} bytes)", fileName, files.size(), size);
        return new SnapshotManifest.PartManifest(partNumber, fileName, size, Digests.hex(sha.digest()),
                files.size(), new ArrayList<>(tables));
    }

    private void addStored(ZipOutputStream zip, SnapshotManifest.FileEntry fe, Long knownCrc) throws IOException {
        Path file = source.apply(fe.path());
        ZipEntry entry = new ZipEntry(fe.path());
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(fe.size());
        entry.setCompressedSize(fe.size());
        entry.setCrc(knownCrc != null ? knownCrc : crc32(file));
        // Fixed timestamp keeps repeated exports of the same file set byte-comparable.
        entry.setTime(0L);
        zip.putNextEntry(entry);
        try (InputStream in = Files.newInputStream(file)) {
            in.transferTo(zip);
        }
        // ZipOutputStream checks the stored CRC and size against the bytes written.
        zip.closeEntry();
    }

    private static long crc32(Path file) throws IOException {
        CRC32 crc = new CRC32();
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buf = new byte[1 << 16];
            int r;
            while ((r = in.read(buf)) != -1) {
                crc.update(buf, 0, r);
            }
        }
        return crc.getValue();
    }
}
