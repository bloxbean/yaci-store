package com.bloxbean.cardano.yaci.store.snapshot.archive;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.LongConsumer;

/**
 * Level-1 (artifact) validation: the exact required part set is present, each part's SHA-256 matches
 * the manifest, and every declared file is carried by exactly one part.
 *
 * <p>Integrity verification is mandatory even when signature verification is disabled.
 */
public class ArchiveVerifier {

    /** Parallel part hashing: the default thread count, so a slow disk sees at most this many streams. */
    public static final int DEFAULT_THREADS = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors()));

    public record Result(boolean ok, List<String> problems, long bytesVerified) {}

    private record PartCheck(List<String> problems, long bytes, Set<String> entries) {}

    private final int threads;

    public ArchiveVerifier() {
        this(DEFAULT_THREADS);
    }

    /** @param threads parts hashed at once; parts are independent archives */
    public ArchiveVerifier(int threads) {
        this.threads = Math.max(1, threads);
    }

    public Result verify(SnapshotManifest manifest, Path archiveDir, boolean deepEntryScan) {
        return verify(manifest, archiveDir, deepEntryScan, null);
    }

    /** @param hashed told the bytes hashed as they are read, for progress; may be null */
    public Result verify(SnapshotManifest manifest, Path archiveDir, boolean deepEntryScan, LongConsumer hashed) {
        return check(manifest, archiveDir, deepEntryScan, true, hashed);
    }

    /** Cheap preflight: existence and sizes only. Import verifies every digest before extraction. */
    public Result inspect(SnapshotManifest manifest, Path archiveDir) {
        return check(manifest, archiveDir, false, false, null);
    }

    /**
     * Existence, sizes and the entry set of every part, without hashing. For a caller that hashes
     * each part and each entry while it extracts them.
     */
    public Result structure(SnapshotManifest manifest, Path archiveDir) {
        return check(manifest, archiveDir, true, false, null);
    }

    /**
     * The parts a (possibly scoped) import reads, and the files it needs from them. A part outside
     * {@code parts} is never read, so it is not checked; its digest is still pinned by the manifest.
     */
    public record Selection(List<SnapshotManifest.PartManifest> parts, Set<String> files) {
        public static Selection all(SnapshotManifest manifest) {
            Set<String> files = new TreeSet<>();
            manifest.tables().forEach(t -> t.files().forEach(f -> files.add(f.path())));
            return new Selection(manifest.parts(), files);
        }
    }

    public Result verify(SnapshotManifest manifest, Path archiveDir, boolean deepEntryScan, LongConsumer hashed,
                         Selection selection) {
        return check(manifest, archiveDir, deepEntryScan, true, hashed, selection);
    }

    public Result inspect(SnapshotManifest manifest, Path archiveDir, Selection selection) {
        return check(manifest, archiveDir, false, false, null, selection);
    }

    public Result structure(SnapshotManifest manifest, Path archiveDir, Selection selection) {
        return check(manifest, archiveDir, true, false, null, selection);
    }

    private Result check(SnapshotManifest manifest, Path archiveDir, boolean deepEntryScan,
                         boolean hashParts, LongConsumer hashed) {
        return check(manifest, archiveDir, deepEntryScan, hashParts, hashed, Selection.all(manifest));
    }

    private Result check(SnapshotManifest manifest, Path archiveDir, boolean deepEntryScan,
                         boolean hashParts, LongConsumer hashed, Selection selection) {
        List<String> problems = new ArrayList<>();
        long bytes = 0;

        Set<String> declaredFiles = new TreeSet<>();
        manifest.tables().forEach(t -> t.files().forEach(f -> declaredFiles.add(f.path())));
        boolean everyPart = selection.parts().size() == manifest.parts().size();

        List<PartCheck> checks = Parallel.map(selection.parts(), hashParts ? threads : 1,
                part -> checkPart(part, archiveDir.resolve(part.fileName()), deepEntryScan, hashParts, hashed));

        // Merged in manifest order, so duplicate reports are deterministic.
        Set<String> coveredFiles = new TreeSet<>();
        for (int i = 0; i < checks.size(); i++) {
            PartCheck c = checks.get(i);
            problems.addAll(c.problems());
            bytes += c.bytes();
            for (String e : c.entries()) {
                if (!coveredFiles.add(e)) {
                    problems.add("File '" + e + "' appears in more than one part");
                }
            }
        }

        if (deepEntryScan) {
            Set<String> missing = new TreeSet<>(everyPart ? declaredFiles : selection.files());
            missing.removeAll(coveredFiles);
            if (!missing.isEmpty()) {
                problems.add("Files declared in the manifest but absent from every part: "
                        + limit(missing));
            }
            Set<String> extra = new HashSet<>(coveredFiles);
            extra.removeAll(declaredFiles);
            if (!extra.isEmpty()) {
                problems.add("Files present in parts but not declared in the manifest: "
                        + limit(new TreeSet<>(extra)));
            }
        }

        return new Result(problems.isEmpty(), problems, bytes);
    }

    private static PartCheck checkPart(SnapshotManifest.PartManifest part, Path p, boolean deepEntryScan,
                                       boolean hashParts, LongConsumer hashed) {
        List<String> problems = new ArrayList<>();
        Set<String> entries = new TreeSet<>();
        long bytes = 0;
        if (!Files.isRegularFile(p)) {
            problems.add("Missing archive part: " + part.fileName());
            return new PartCheck(problems, 0, entries);
        }
        try {
            long size = Files.size(p);
            if (size != part.size()) {
                problems.add(part.fileName() + ": size " + size + " but manifest declares " + part.size());
            }
            if (hashParts) {
                String sha = Digests.sha256Hex(p, hashed);
                if (!sha.equals(part.sha256())) {
                    problems.add(part.fileName() + ": SHA-256 mismatch");
                }
                bytes += size;
            }

            if (deepEntryScan) {
                Set<String> listed = ArchiveExtractor.listEntries(p);
                if (listed.size() != part.entryCount()) {
                    problems.add(part.fileName() + ": " + listed.size()
                            + " entries but manifest declares " + part.entryCount());
                }
                for (String e : listed) {
                    try {
                        ArchiveExtractor.validateName(e);
                    } catch (IOException ex) {
                        problems.add(part.fileName() + ": " + ex.getMessage());
                    }
                }
                entries.addAll(listed);
            }
        } catch (IOException e) {
            problems.add(part.fileName() + ": " + e.getMessage());
        }
        return new PartCheck(problems, bytes, entries);
    }

    private static String limit(Set<String> values) {
        List<String> l = new ArrayList<>(values);
        if (l.size() <= 5) {
            return l.toString();
        }
        return l.subList(0, 5) + " (+" + (l.size() - 5) + " more)";
    }
}
