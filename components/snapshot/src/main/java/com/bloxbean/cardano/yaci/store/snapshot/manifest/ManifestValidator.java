package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** The initial restore format requires the complete, locally installed specification set. */
public final class ManifestValidator {
    private final SnapshotSpecRegistry registry;

    public ManifestValidator(SnapshotSpecRegistry registry) {
        this.registry = registry;
    }

    public List<String> validate(SnapshotManifest manifest) {
        List<String> problems = new ArrayList<>();
        if (manifest.formatVersion() != ManifestCodec.FORMAT_VERSION
                || !ManifestCodec.SPEC_FORMAT_VERSION.equals(manifest.specFormatVersion())) {
            problems.add("Unsupported snapshot or specification format version");
        }
        if (manifest.point() == null || manifest.parts() == null
                || manifest.snapshotId() == null || manifest.snapshotId().isBlank()) {
            problems.add("Manifest must declare its snapshot ID, consistency point, and archive parts");
        }
        if (manifest.tables() == null) {
            return List.of("Manifest is missing its table specifications");
        }
        Set<String> seen = new HashSet<>();
        Set<String> files = new HashSet<>();
        for (SnapshotManifest.TableManifest t : manifest.tables()) {
            if (!seen.add(t.specId())) {
                problems.add("Duplicate manifest specification '" + t.specId() + "'");
            }
            SnapshotTableSpec spec = registry.byId(t.specId()).orElse(null);
            if (spec == null) {
                problems.add("Snapshot references specification '" + t.specId() + "' not installed locally");
                continue;
            }
            if (spec.specVersion() != t.specVersion()) {
                problems.add("Specification '" + t.specId() + "' version does not match the local specification");
            } else if (!Objects.equals(spec.digest(), t.specDigest())) {
                problems.add("Specification '" + t.specId() + "' has digest different from the local specification");
            }
            if (!Objects.equals(spec.targetTable(), t.targetTable())
                    || !spec.restore().name().equals(t.restore())) {
                problems.add("Specification '" + t.specId() + "' target or restore classification does not match");
            }
            if (t.files() == null || t.rowCount() < 0 || (t.rowCount() > 0 && t.files().isEmpty())) {
                problems.add("Specification '" + t.specId() + "' has invalid row count or file list");
            } else {
                for (SnapshotManifest.FileEntry file : t.files()) {
                    if (!files.add(file.path())) {
                        problems.add("Duplicate manifest file '" + file.path() + "'");
                    }
                }
            }
        }
        for (SnapshotTableSpec spec : registry.all()) {
            if (!seen.contains(spec.id())) {
                problems.add("Missing manifest specification '" + spec.id() + "'; a complete snapshot is required");
            }
        }
        return problems;
    }
}
