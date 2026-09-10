package com.bloxbean.cardano.yaci.store.snapshot.archive;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchiveTest {

    private record Fixture(Path dataDir, List<SnapshotManifest.FileEntry> entries, List<String> owners) {}

    private Fixture fixture(Path dataDir, int count, int sizeBytes) throws IOException {
        List<SnapshotManifest.FileEntry> entries = new ArrayList<>();
        List<String> owners = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Path p = dataDir.resolve("main/widget/date=2026-01-%02d/data.parquet".formatted(i + 1));
            Files.createDirectories(p.getParent());
            byte[] payload = new byte[sizeBytes];
            java.util.Arrays.fill(payload, (byte) ('a' + (i % 26)));
            Files.write(p, payload);
            String rel = dataDir.relativize(p).toString();
            entries.add(new SnapshotManifest.FileEntry(rel, sizeBytes, Digests.sha256Hex(p), 10,
                    rel.substring(0, rel.lastIndexOf('/'))));
            owners.add("widget");
        }
        return new Fixture(dataDir, entries, owners);
    }

    @Test
    void splitsIntoPartsAtTheConfiguredBoundary(@TempDir Path tmp) throws IOException {
        Path dataDir = tmp.resolve("data");
        Path out = tmp.resolve("out");
        Fixture f = fixture(dataDir, 5, 1000);

        // 2500 bytes per part means 2 files per part, so 3 parts for 5 files.
        List<SnapshotManifest.PartManifest> parts =
                new ArchiveWriter(dataDir, out, "snap", 2500).write(f.entries(), f.owners(), null);

        assertThat(parts).hasSize(3);
        assertThat(parts.get(0).entryCount()).isEqualTo(2);
        assertThat(parts.get(2).entryCount()).isEqualTo(1);
        assertThat(parts).allSatisfy(p -> assertThat(p.sha256()).hasSize(64));
        assertThat(parts).extracting(SnapshotManifest.PartManifest::fileName)
                .containsExactly("snap.part-00001.zip", "snap.part-00002.zip", "snap.part-00003.zip");
    }

    @Test
    void neverSplitsAFileEvenWhenItExceedsThePartSize(@TempDir Path tmp) throws IOException {
        Path dataDir = tmp.resolve("data");
        Path out = tmp.resolve("out");
        Fixture f = fixture(dataDir, 3, 5000);

        List<SnapshotManifest.PartManifest> parts =
                new ArchiveWriter(dataDir, out, "snap", 1000).write(f.entries(), f.owners(), null);

        assertThat(parts).hasSize(3);
        assertThat(parts).allSatisfy(p -> assertThat(p.entryCount()).isEqualTo(1));
    }

    @Test
    void entriesAreStoredNotDeflated(@TempDir Path tmp) throws IOException {
        Path dataDir = tmp.resolve("data");
        Path out = tmp.resolve("out");
        // Highly compressible content: a deflated part would be far smaller than the payload.
        Fixture f = fixture(dataDir, 1, 100_000);
        new ArchiveWriter(dataDir, out, "snap", 1 << 30).write(f.entries(), f.owners(), null);

        long partSize = Files.size(out.resolve("snap.part-00001.zip"));
        assertThat(partSize).isGreaterThanOrEqualTo(100_000);
    }

    @Test
    void repeatedExportsOfTheSameFileSetProduceIdenticalParts(@TempDir Path tmp) throws IOException {
        Path dataDir = tmp.resolve("data");
        Fixture f = fixture(dataDir, 4, 500);

        var first = new ArchiveWriter(dataDir, tmp.resolve("a"), "snap", 1 << 30)
                .write(f.entries(), f.owners(), null);
        var second = new ArchiveWriter(dataDir, tmp.resolve("b"), "snap", 1 << 30)
                .write(f.entries(), f.owners(), null);

        assertThat(first.get(0).sha256()).isEqualTo(second.get(0).sha256());
    }

    @Test
    void extractionRestoresFilesAndVerifiesDigests(@TempDir Path tmp) throws IOException {
        Path dataDir = tmp.resolve("data");
        Path out = tmp.resolve("out");
        Fixture f = fixture(dataDir, 3, 800);
        var parts = new ArchiveWriter(dataDir, out, "snap", 1 << 30).write(f.entries(), f.owners(), null);

        Path work = tmp.resolve("work");
        Map<String, Long> expected = new java.util.TreeMap<>();
        f.entries().forEach(e -> expected.put(e.path(), e.size()));
        ArchiveExtractor extractor = new ArchiveExtractor(work, 100, 1 << 20);
        int n = extractor.extract(out.resolve(parts.get(0).fileName()), expected, null);

        assertThat(n).isEqualTo(3);
        for (SnapshotManifest.FileEntry e : f.entries()) {
            assertThat(extractor.verifyFile(e.path(), e.sha256())).isTrue();
        }
    }

    @Test
    void rejectsPathTraversalEntries(@TempDir Path tmp) throws IOException {
        Path zip = tmp.resolve("evil.zip");
        writeZip(zip, Map.of("../../etc/passwd", "pwned"));

        ArchiveExtractor extractor = new ArchiveExtractor(tmp.resolve("work"), 100, 1 << 20);
        assertThatThrownBy(() -> extractor.extract(zip, Map.of("../../etc/passwd", 5L), null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Path traversal");
    }

    @Test
    void rejectsAbsolutePathEntries(@TempDir Path tmp) throws IOException {
        Path zip = tmp.resolve("evil.zip");
        writeZip(zip, Map.of("/etc/passwd", "pwned"));

        ArchiveExtractor extractor = new ArchiveExtractor(tmp.resolve("work"), 100, 1 << 20);
        assertThatThrownBy(() -> extractor.extract(zip, Map.of("/etc/passwd", 5L), null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Absolute or drive-qualified");
    }

    @Test
    void rejectsDuplicateEntries(@TempDir Path tmp) throws IOException {
        // ZipOutputStream refuses to write a duplicate name, so the malformed archive is assembled
        // from the local-header region of two single-entry archives, which is exactly what a
        // hand-crafted hostile part would look like to a sequential reader.
        Path a = tmp.resolve("a.zip");
        Path b = tmp.resolve("b.zip");
        writeZip(a, Map.of("a/b.parquet", "data"));
        writeZip(b, Map.of("a/b.parquet", "data"));

        Path zip = tmp.resolve("dupe.zip");
        byte[] first = Files.readAllBytes(a);
        byte[] second = Files.readAllBytes(b);
        int firstCd = centralDirectoryOffset(first);
        int secondCd = centralDirectoryOffset(second);
        try (OutputStream os = Files.newOutputStream(zip)) {
            os.write(first, 0, firstCd);
            os.write(second, 0, secondCd);
            os.write(first, firstCd, first.length - firstCd);
        }

        ArchiveExtractor extractor = new ArchiveExtractor(tmp.resolve("work"), 100, 1 << 20);
        assertThatThrownBy(() -> extractor.extract(zip, Map.of("a/b.parquet", 4L), null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Duplicate entry");
    }

    /** Offset of the first central-directory header (PK\x01\x02). */
    private static int centralDirectoryOffset(byte[] zip) {
        for (int i = 0; i < zip.length - 3; i++) {
            if (zip[i] == 0x50 && zip[i + 1] == 0x4B && zip[i + 2] == 0x01 && zip[i + 3] == 0x02) {
                return i;
            }
        }
        throw new IllegalStateException("No central directory found in test fixture");
    }

    @Test
    void rejectsEntriesNotDeclaredInTheManifest(@TempDir Path tmp) throws IOException {
        Path zip = tmp.resolve("extra.zip");
        writeZip(zip, Map.of("a/unexpected.parquet", "data"));

        ArchiveExtractor extractor = new ArchiveExtractor(tmp.resolve("work"), 100, 1 << 20);
        assertThatThrownBy(() -> extractor.extract(zip, Map.of("a/declared.parquet", 4L), null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("not declared in the manifest");
    }

    @Test
    void rejectsAnEntryLargerThanTheManifestDeclares(@TempDir Path tmp) throws IOException {
        Path zip = tmp.resolve("big.zip");
        writeZip(zip, Map.of("a/b.parquet", "much more data than declared"));

        ArchiveExtractor extractor = new ArchiveExtractor(tmp.resolve("work"), 100, 1 << 20);
        assertThatThrownBy(() -> extractor.extract(zip, Map.of("a/b.parquet", 4L), null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("the manifest declares");
    }

    @Test
    void enforcesTheEntryCountLimit(@TempDir Path tmp) throws IOException {
        Path zip = tmp.resolve("many.zip");
        Map<String, String> entries = new java.util.LinkedHashMap<>();
        Map<String, Long> expected = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 5; i++) {
            entries.put("a/" + i + ".parquet", "data");
            expected.put("a/" + i + ".parquet", 4L);
        }
        writeZip(zip, entries);

        ArchiveExtractor extractor = new ArchiveExtractor(tmp.resolve("work"), 2, 1 << 20);
        assertThatThrownBy(() -> extractor.extract(zip, expected, null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("entry limit");
    }

    @Test
    void verifierDetectsATamperedPart(@TempDir Path tmp) throws IOException {
        Path dataDir = tmp.resolve("data");
        Path out = tmp.resolve("out");
        Fixture f = fixture(dataDir, 2, 400);
        var parts = new ArchiveWriter(dataDir, out, "snap", 1 << 30).write(f.entries(), f.owners(), null);

        SnapshotManifest manifest = manifestOf(f.entries(), parts);
        assertThat(new ArchiveVerifier().verify(manifest, out, true).ok()).isTrue();

        Path part = out.resolve(parts.get(0).fileName());
        byte[] bytes = Files.readAllBytes(part);
        bytes[bytes.length / 2] ^= 0x5A;
        Files.write(part, bytes);

        ArchiveVerifier.Result result = new ArchiveVerifier().verify(manifest, out, false);
        assertThat(result.ok()).isFalse();
        assertThat(result.problems()).anySatisfy(p -> assertThat(p).contains("SHA-256 mismatch"));
    }

    @Test
    void verifierReportsAMissingPart(@TempDir Path tmp) throws IOException {
        Path dataDir = tmp.resolve("data");
        Path out = tmp.resolve("out");
        Fixture f = fixture(dataDir, 2, 400);
        var parts = new ArchiveWriter(dataDir, out, "snap", 1 << 30).write(f.entries(), f.owners(), null);
        Files.delete(out.resolve(parts.get(0).fileName()));

        ArchiveVerifier.Result result =
                new ArchiveVerifier().verify(manifestOf(f.entries(), parts), out, true);
        assertThat(result.ok()).isFalse();
        assertThat(result.problems()).anySatisfy(p -> assertThat(p).contains("Missing archive part"));
    }

    private static SnapshotManifest manifestOf(List<SnapshotManifest.FileEntry> entries,
                                               List<SnapshotManifest.PartManifest> parts) {
        SnapshotManifest.TableManifest table = new SnapshotManifest.TableManifest(
                "widget", 1, "d", "test", "CHAIN_DATA", "IMPORT", null, "widget", "widget", "widget",
                "DIRECT", "NONE", 1, List.of(), entries.size(), List.of("id"), "fp", Map.of(),
                Map.of(), entries);
        return new SnapshotManifest(1, "sid", "now", "test", "0", "1", "duck", "1", null, null,
                List.of(), Map.of(), null, null, List.of(table), parts, List.of());
    }

    private static void writeZip(Path zip, Map<String, String> entries) throws IOException {
        try (OutputStream os = Files.newOutputStream(zip); ZipOutputStream zo = new ZipOutputStream(os)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                byte[] payload = e.getValue().getBytes(StandardCharsets.UTF_8);
                ZipEntry entry = new ZipEntry(e.getKey());
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(payload.length);
                entry.setCompressedSize(payload.length);
                CRC32 crc = new CRC32();
                crc.update(payload);
                entry.setCrc(crc.getValue());
                zo.putNextEntry(entry);
                zo.write(payload);
                zo.closeEntry();
            }
        }
    }
}
