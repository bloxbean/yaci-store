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

/**
 * Level-1 (artifact) validation: the exact required part set is present, each part's SHA-256 matches
 * the manifest, and every declared file is carried by exactly one part.
 *
 * <p>Integrity verification is mandatory even when signature verification is disabled.
 */
public class ArchiveVerifier {

    public record Result(boolean ok, List<String> problems, long bytesVerified) {}

    public Result verify(SnapshotManifest manifest, Path archiveDir, boolean deepEntryScan) {
        List<String> problems = new ArrayList<>();
        long bytes = 0;

        Set<String> declaredFiles = new TreeSet<>();
        manifest.tables().forEach(t -> t.files().forEach(f -> declaredFiles.add(f.path())));

        Set<String> coveredFiles = new TreeSet<>();
        for (SnapshotManifest.PartManifest part : manifest.parts()) {
            Path p = archiveDir.resolve(part.fileName());
            if (!Files.isRegularFile(p)) {
                problems.add("Missing archive part: " + part.fileName());
                continue;
            }
            try {
                long size = Files.size(p);
                if (size != part.size()) {
                    problems.add(part.fileName() + ": size " + size + " but manifest declares " + part.size());
                }
                String sha = Digests.sha256Hex(p);
                if (!sha.equals(part.sha256())) {
                    problems.add(part.fileName() + ": SHA-256 mismatch");
                }
                bytes += size;

                if (deepEntryScan) {
                    Set<String> entries = ArchiveExtractor.listEntries(p);
                    if (entries.size() != part.entryCount()) {
                        problems.add(part.fileName() + ": " + entries.size()
                                + " entries but manifest declares " + part.entryCount());
                    }
                    for (String e : entries) {
                        try {
                            ArchiveExtractor.validateName(e);
                        } catch (IOException ex) {
                            problems.add(part.fileName() + ": " + ex.getMessage());
                        }
                        if (!coveredFiles.add(e)) {
                            problems.add("File '" + e + "' appears in more than one part");
                        }
                    }
                }
            } catch (IOException e) {
                problems.add(part.fileName() + ": " + e.getMessage());
            }
        }

        if (deepEntryScan) {
            Set<String> missing = new TreeSet<>(declaredFiles);
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

    private static String limit(Set<String> values) {
        List<String> l = new ArrayList<>(values);
        if (l.size() <= 5) {
            return l.toString();
        }
        return l.subList(0, 5) + " (+" + (l.size() - 5) + " more)";
    }
}
