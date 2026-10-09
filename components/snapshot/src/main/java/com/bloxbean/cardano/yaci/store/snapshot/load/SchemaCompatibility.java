package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.manifest.SnapshotManifest;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.spec.RestoreMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Full restores compare global metadata; scoped restores compare each selected target definition. */
public final class SchemaCompatibility {
    private SchemaCompatibility() {}
    public static List<String> check(PgSchema schema, SnapshotManifest manifest, SnapshotSpecRegistry registry)
            throws SQLException {
        List<String> problems = new ArrayList<>();
        boolean sameSchema = manifest.schemaFingerprint() != null
                && manifest.schemaFingerprint().equals(schema.fingerprint());
        if (!registry.isScoped()) {
            if (manifest.schemaFingerprint() != null && !sameSchema) {
                problems.add("Target schema fingerprint does not match the snapshot. Initialize with the matching release.");
            }
            if (manifest.flywayFingerprint() != null && !manifest.flywayFingerprint().equals(schema.flywayFingerprint())) {
                problems.add("Applied Flyway migrations do not match the snapshot's release");
            }
            return problems;
        }
        for (var spec : registry.all()) {
            boolean required = spec.restore() == RestoreMode.IMPORT || spec.restore() == RestoreMode.HANDLER;
            if (!schema.tableExists(spec.targetTable())) {
                if (required) problems.add("Enabled specification '" + spec.id() + "' requires missing target table '"
                        + spec.targetTable() + "'");
                continue;
            }
            String expected = manifest.tableSchemaFingerprints() == null ? null
                    : manifest.tableSchemaFingerprints().get(spec.targetTable());
            if (expected == null) {
                if (!sameSchema && required) problems.add("Snapshot lacks target schema metadata for '"
                        + spec.targetTable() + "'. Regenerate it with per-table fingerprints for scoped import.");
            } else if (!expected.equals(schema.tableFingerprint(spec.targetTable()))) {
                problems.add("Target definition for selected table '" + spec.targetTable() + "' does not match the snapshot");
            }
        }
        return problems;
    }
}
