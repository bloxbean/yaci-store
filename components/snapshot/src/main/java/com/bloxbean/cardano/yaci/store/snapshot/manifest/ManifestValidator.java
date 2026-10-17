package com.bloxbean.cardano.yaci.store.snapshot.manifest;

import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validate all archive structure and require compatible specifications for every selected table.
 *
 * <p>A specification must match at the same {@code spec-version}. A manifest that records the export
 * contract digest needs only the contract to match, so a release that fixes an import transform
 * still loads snapshots exported before the fix; an older manifest needs the whole specification.
 */
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
            // A table with no rows and no files carries nothing to load: a specification removed since
            // the export (e.g. cip113-registry-node) must not make the snapshot unimportable.
            if (spec == null && t.rowCount() == 0 && (t.files() == null || t.files().isEmpty())) {
                continue;
            }
            if (spec == null && !registry.isScoped()) {
                problems.add("Snapshot references specification '" + t.specId() + "' not installed locally");
                continue;
            }
            if (registry.isSelected(t.specId()) && spec != null) {
                if (spec.specVersion() != t.specVersion()) {
                    problems.add("Specification '" + t.specId() + "' version does not match the local specification");
                } else if (t.contractDigest() != null) {
                    // Only what the data is must match; a newer importer may load it differently.
                    if (!Objects.equals(spec.contractDigest(), t.contractDigest())) {
                        problems.add("Specification '" + t.specId() + "' has an export contract different from "
                                + "the local specification at the same spec-version. Re-export the snapshot, "
                                + "or import it with the release that produced it.");
                    }
                } else if (!Objects.equals(spec.digest(), t.specDigest())) {
                    problems.add("Specification '" + t.specId() + "' has digest different from the local specification");
                }
                if (!Objects.equals(spec.targetTable(), t.targetTable())
                        || !spec.restore().name().equals(t.restore())) {
                    problems.add("Specification '" + t.specId() + "' target or restore classification does not match");
                }
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
            if (!seen.contains(spec.id()) && spec.optional()) {
                problems.add("The snapshot does not carry " + spec.targetTable() + ", which this target turns on with "
                        + String.join(" or ", spec.enabledBy()) + ". Turn it off, or import a snapshot exported"
                        + " from a source that saves it.");
            } else if (!seen.contains(spec.id())) {
                problems.add("Missing manifest specification '" + spec.id() + "'; a complete snapshot is required");
            }
        }
        return problems;
    }

    /**
     * Selected tables whose export contract matches but whose import side differs from the
     * specification the snapshot was exported with, so they load with this release's transform.
     * Informational: {@link #validate} has already accepted them.
     */
    public List<String> importChanges(SnapshotManifest manifest) {
        List<String> notes = new ArrayList<>();
        for (SnapshotManifest.TableManifest t : manifest.tables() == null ? List.<SnapshotManifest.TableManifest>of()
                : manifest.tables()) {
            SnapshotTableSpec spec = registry.byId(t.specId()).orElse(null);
            if (spec != null && registry.isSelected(t.specId()) && t.contractDigest() != null
                    && t.contractDigest().equals(spec.contractDigest()) && !spec.digest().equals(t.specDigest())) {
                notes.add("Specification '" + t.specId() + "' is loaded with this release's import "
                        + (spec.importSpec().transformVersion() != t.transformVersion()
                        ? "transform (version " + spec.importSpec().transformVersion() + "; the snapshot was "
                        + "exported with version " + t.transformVersion() + ")"
                        : "mapping, which differs from the exporting release's")
                        + "; the export contract matches");
            }
        }
        return notes;
    }
}
