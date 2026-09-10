package com.bloxbean.cardano.yaci.store.snapshot.archive;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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

    private final Path dataDir;
    private final Path outputDir;
    private final String baseName;
    private final long partSizeBytes;

    public ArchiveWriter(Path dataDir, Path outputDir, String baseName, long partSizeBytes) {
        this.dataDir = dataDir;
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
        if (files.size() != owners.size()) {
            throw new IllegalArgumentException("files and owners must be the same length");
        }
        Files.createDirectories(outputDir);

        List<SnapshotManifest.PartManifest> parts = new ArrayList<>();
        int partNumber = 0;
        int index = 0;
        while (index < files.size()) {
            partNumber++;
            String fileName = String.format("%s.part-%05d.zip", baseName, partNumber);
            Path partPath = outputDir.resolve(fileName);
            Set<String> tables = new LinkedHashSet<>();
            int entries = 0;
            long uncompressed = 0;

            try (OutputStream os = Files.newOutputStream(partPath);
                 ZipOutputStream zip = new ZipOutputStream(os)) {
                zip.setMethod(ZipOutputStream.STORED);
                while (index < files.size()) {
                    SnapshotManifest.FileEntry fe = files.get(index);
                    // Never split a Parquet file: start a new part unless this part is still empty.
                    if (entries > 0 && uncompressed + fe.size() > partSizeBytes) {
                        break;
                    }
                    addStored(zip, fe);
                    tables.add(owners.get(index));
                    uncompressed += fe.size();
                    entries++;
                    index++;
                    if (progress != null) {
                        progress.accept(fe.path());
                    }
                }
            }

            long size = Files.size(partPath);
            String sha = Digests.sha256Hex(partPath);
            parts.add(new SnapshotManifest.PartManifest(partNumber, fileName, size, sha, entries,
                    new ArrayList<>(tables)));
            log.info("Wrote {} ({} entries, {} bytes)", fileName, entries, size);
        }
        return parts;
    }

    private void addStored(ZipOutputStream zip, SnapshotManifest.FileEntry fe) throws IOException {
        Path source = dataDir.resolve(fe.path());
        ZipEntry entry = new ZipEntry(fe.path());
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(fe.size());
        entry.setCompressedSize(fe.size());
        entry.setCrc(crc32(source));
        // Fixed timestamp keeps repeated exports of the same file set byte-comparable.
        entry.setTime(0L);
        zip.putNextEntry(entry);
        try (InputStream in = Files.newInputStream(source)) {
            in.transferTo(zip);
        }
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
