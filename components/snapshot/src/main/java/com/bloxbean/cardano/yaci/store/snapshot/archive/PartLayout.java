package com.bloxbean.cardano.yaci.store.snapshot.archive;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Which files each part carries, worked out from the manifest alone.
 *
 * <p>The exporter writes the tables' files in manifest order and cuts that sequence into parts, so
 * part N holds the next {@code entryCount} files. The result is checked against each part's recorded
 * table list. When the two disagree, every part that lists one of a file's tables may hold it.
 */
public final class PartLayout {

    private final Map<Integer, Set<String>> filesByPart;
    private final boolean exact;

    private PartLayout(Map<Integer, Set<String>> filesByPart, boolean exact) {
        this.filesByPart = filesByPart;
        this.exact = exact;
    }

    public static PartLayout of(SnapshotManifest manifest) {
        List<String> files = new ArrayList<>();
        List<String> owners = new ArrayList<>();
        for (SnapshotManifest.TableManifest t : manifest.tables()) {
            if (t.files() != null) {
                t.files().forEach(f -> {
                    files.add(f.path());
                    owners.add(t.specId());
                });
            }
        }
        List<SnapshotManifest.PartManifest> parts = manifest.parts().stream()
                .sorted(Comparator.comparingInt(SnapshotManifest.PartManifest::partNumber)).toList();

        Map<Integer, Set<String>> byPart = new LinkedHashMap<>();
        int at = 0;
        boolean exact = parts.stream().mapToInt(SnapshotManifest.PartManifest::entryCount).sum() == files.size();
        for (SnapshotManifest.PartManifest part : parts) {
            if (!exact) {
                break;
            }
            Set<String> partFiles = new LinkedHashSet<>(files.subList(at, at + part.entryCount()));
            Set<String> partOwners = new TreeSet<>(owners.subList(at, at + part.entryCount()));
            exact = partOwners.equals(new TreeSet<>(part.tables()));
            byPart.put(part.partNumber(), partFiles);
            at += part.entryCount();
        }
        if (exact) {
            return new PartLayout(byPart, true);
        }

        Map<Integer, Set<String>> byTable = new LinkedHashMap<>();
        for (SnapshotManifest.PartManifest part : parts) {
            Set<String> partFiles = new LinkedHashSet<>();
            for (int i = 0; i < files.size(); i++) {
                if (part.tables().contains(owners.get(i))) {
                    partFiles.add(files.get(i));
                }
            }
            byTable.put(part.partNumber(), partFiles);
        }
        return new PartLayout(byTable, false);
    }

    /** True when each file is known to be in exactly one part. */
    public boolean exact() {
        return exact;
    }

    /** The parts that hold (or, when not {@link #exact}, may hold) any of {@code files}. */
    public List<SnapshotManifest.PartManifest> partsFor(SnapshotManifest manifest, Set<String> files) {
        return manifest.parts().stream()
                .filter(p -> filesByPart.getOrDefault(p.partNumber(), Set.of()).stream().anyMatch(files::contains))
                .sorted(Comparator.comparingInt(SnapshotManifest.PartManifest::partNumber))
                .toList();
    }

    public Set<String> files(int partNumber) {
        return filesByPart.getOrDefault(partNumber, Set.of());
    }
}
