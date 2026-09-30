package com.bloxbean.cardano.yaci.store.snapshot.spec;

import com.bloxbean.cardano.yaci.store.snapshot.util.Digests;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Binds {@code snapshot-table} YAML documents to {@link SnapshotTableSpec} and rejects anything that
 * is not fully specified.
 *
 * <p>Validation is intentionally strict rather than permissive: an unknown key, a missing
 * classification or a mapping that cannot be executed is an error at load time, not a surprise
 * during a multi-hour import.
 */
public class SnapshotSpecLoader {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private static final Set<String> ROOT_KEYS =
            Set.of("id", "spec-version", "module", "kind", "restore", "reason",
                    "source", "consistency", "import", "validation", "lossy", "lossy-note");
    private static final Set<String> SOURCE_KEYS = Set.of("exporter-id", "ducklake-relation", "partition");
    private static final Set<String> PARTITION_KEYS = Set.of("strategy", "column");
    private static final Set<String> CONSISTENCY_KEYS = Set.of("completed-epoch", "cutoff", "coverage");
    private static final Set<String> RULE_KEYS = Set.of("type", "column", "offset");
    private static final Set<String> IMPORT_KEYS =
            Set.of("target-table", "mode", "columns", "ignore-source-columns", "use-target-defaults",
                    "select-resource", "transform-version", "dependencies", "batch-boundary",
                    "batch-size", "target-partitioning", "handler");
    private static final Set<String> COLUMN_KEYS = Set.of("source", "converter", "constant", "use-default");
    private static final Set<String> TARGET_PARTITIONING_KEYS = Set.of("column", "partition-prefix");
    private static final Set<String> VALIDATION_KEYS =
            Set.of("key", "bounds", "required-columns", "source-key");

    public SnapshotTableSpec loadFile(Path file) {
        try {
            return load(Files.readAllBytes(file), file.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new SpecException("Unable to read snapshot spec: " + file, e);
        }
    }

    public SnapshotTableSpec loadStream(InputStream in, String origin) {
        try (in) {
            return load(in.readAllBytes(), origin);
        } catch (IOException e) {
            throw new SpecException("Unable to read snapshot spec: " + origin, e);
        }
    }

    /**
     * @param yaml   raw specification bytes; the SHA-256 of these exact bytes becomes the spec digest
     * @param origin diagnostic label (file path or classpath resource)
     */
    public SnapshotTableSpec load(byte[] yaml, String origin) {
        JsonNode root;
        try {
            root = YAML.readTree(yaml);
        } catch (IOException e) {
            throw new SpecException("Malformed YAML in " + origin, e);
        }
        if (root == null || !root.has("snapshot-table")) {
            throw new SpecException("Missing top-level 'snapshot-table' in " + origin);
        }
        JsonNode n = root.get("snapshot-table");
        rejectUnknown(n, ROOT_KEYS, origin, "snapshot-table");

        String id = requireText(n, "id", origin);
        Identifiers.requireSpecId(id);
        int specVersion = requireInt(n, "spec-version", origin);
        if (specVersion < 1) {
            throw new SpecException(origin + ": spec-version must be >= 1");
        }
        String module = requireText(n, "module", origin);
        TableKind kind = enumValue(TableKind.class, requireText(n, "kind", origin), origin, "kind");
        RestoreMode restore = enumValue(RestoreMode.class, requireText(n, "restore", origin), origin, "restore");
        String reason = optionalText(n, "reason");

        if (restore != RestoreMode.IMPORT && (reason == null || reason.isBlank())) {
            throw new SpecException(origin + ": restore=" + restore + " requires an explicit 'reason'");
        }

        SnapshotTableSpec.Source source = readSource(n.get("source"), origin);
        SnapshotTableSpec.Consistency consistency = readConsistency(n.get("consistency"), origin);
        SnapshotTableSpec.Import importSpec = readImport(n.get("import"), origin, restore);
        SnapshotTableSpec.Validation validation = readValidation(n.get("validation"), origin);
        Map<String, String> lossy = readLossy(n.get("lossy"), origin);
        String lossyNote = optionalText(n, "lossy-note");

        // The digest covers the transform resource too. Without that, editing a SQL transform would
        // change what an import produces while still matching the digest a manifest recorded.
        String digest = Digests.sha256Hex(yaml);
        if (importSpec.selectResource() != null) {
            digest = Digests.sha256Hex(digest + "\n"
                    + Digests.sha256Hex(readTransform(importSpec.selectResource(), origin)));
        }
        SnapshotTableSpec spec = new SnapshotTableSpec(id, specVersion, module, kind, restore, reason,
                source, consistency, importSpec, validation, lossy, lossyNote, digest, origin);
        validateSemantics(spec, origin);
        return spec;
    }

    // ---------------------------------------------------------------- sections

    private SnapshotTableSpec.Source readSource(JsonNode n, String origin) {
        if (n == null || n.isNull()) {
            return null;
        }
        rejectUnknown(n, SOURCE_KEYS, origin, "source");
        String exporterId = requireText(n, "exporter-id", origin);
        String relation = requireText(n, "ducklake-relation", origin);
        Identifiers.requireSqlIdentifier(relation, "ducklake-relation");

        JsonNode p = n.get("partition");
        if (p == null || p.isNull()) {
            throw new SpecException(origin + ": source.partition is required");
        }
        rejectUnknown(p, PARTITION_KEYS, origin, "source.partition");
        PartitionStrategy strategy =
                enumValue(PartitionStrategy.class, requireText(p, "strategy", origin), origin, "source.partition.strategy");
        String column = optionalText(p, "column");
        if (strategy != PartitionStrategy.NONE) {
            if (column == null) {
                throw new SpecException(origin + ": source.partition.column is required for strategy " + strategy);
            }
            Identifiers.requireSqlIdentifier(column, "source.partition.column");
        }
        return new SnapshotTableSpec.Source(exporterId, relation, new SnapshotTableSpec.Partition(strategy, column));
    }

    private SnapshotTableSpec.Consistency readConsistency(JsonNode n, String origin) {
        if (n == null || n.isNull()) {
            return new SnapshotTableSpec.Consistency(SnapshotTableSpec.CompletedEpochRule.none(),
                    SnapshotTableSpec.CutoffRule.none(), SnapshotTableSpec.CoverageRule.none());
        }
        rejectUnknown(n, CONSISTENCY_KEYS, origin, "consistency");

        JsonNode ceNode = n.get("completed-epoch");
        SnapshotTableSpec.CompletedEpochRule completedEpoch;
        if (ceNode == null || ceNode.isNull()) {
            completedEpoch = SnapshotTableSpec.CompletedEpochRule.none();
        } else {
            rejectUnknown(ceNode, RULE_KEYS, origin, "consistency.completed-epoch");
            CompletedEpochType type = enumValue(CompletedEpochType.class,
                    requireText(ceNode, "type", origin), origin, "consistency.completed-epoch.type");
            String column = optionalText(ceNode, "column");
            int offset = ceNode.has("offset") ? ceNode.get("offset").asInt() : 0;
            if (type != CompletedEpochType.NONE) {
                if (column == null) {
                    throw new SpecException(origin + ": consistency.completed-epoch.column is required for " + type);
                }
                Identifiers.requireSqlIdentifier(column, "consistency.completed-epoch.column");
            }
            if (type == CompletedEpochType.MAX_EPOCH && offset != 0) {
                throw new SpecException(origin + ": use MAX_EPOCH_OFFSET when an offset is needed");
            }
            completedEpoch = new SnapshotTableSpec.CompletedEpochRule(type, column, offset);
        }

        JsonNode cutNode = n.get("cutoff");
        SnapshotTableSpec.CutoffRule cutoff;
        if (cutNode == null || cutNode.isNull()) {
            cutoff = SnapshotTableSpec.CutoffRule.none();
        } else {
            rejectUnknown(cutNode, RULE_KEYS, origin, "consistency.cutoff");
            CutoffType type = enumValue(CutoffType.class, requireText(cutNode, "type", origin), origin, "consistency.cutoff.type");
            String column = optionalText(cutNode, "column");
            int offset = cutNode.has("offset") ? cutNode.get("offset").asInt() : 0;
            if (type != CutoffType.NONE) {
                if (column == null) {
                    throw new SpecException(origin + ": consistency.cutoff.column is required for " + type);
                }
                Identifiers.requireSqlIdentifier(column, "consistency.cutoff.column");
            }
            if (type == CutoffType.EPOCH_LTE_OFFSET && offset == 0) {
                throw new SpecException(origin + ": EPOCH_LTE_OFFSET requires a non-zero offset");
            }
            cutoff = new SnapshotTableSpec.CutoffRule(type, column, offset);
        }

        JsonNode covNode = n.get("coverage");
        SnapshotTableSpec.CoverageRule coverage;
        if (covNode == null || covNode.isNull()) {
            coverage = SnapshotTableSpec.CoverageRule.none();
        } else {
            rejectUnknown(covNode, RULE_KEYS, origin, "consistency.coverage");
            CoverageType type = enumValue(CoverageType.class, requireText(covNode, "type", origin), origin, "consistency.coverage.type");
            String column = optionalText(covNode, "column");
            if (type != CoverageType.NONE) {
                if (column == null) {
                    throw new SpecException(origin + ": consistency.coverage.column is required for " + type);
                }
                Identifiers.requireSqlIdentifier(column, "consistency.coverage.column");
            }
            coverage = new SnapshotTableSpec.CoverageRule(type, column);
        }
        return new SnapshotTableSpec.Consistency(completedEpoch, cutoff, coverage);
    }

    private SnapshotTableSpec.Import readImport(JsonNode n, String origin, RestoreMode restore) {
        if (n == null || n.isNull()) {
            throw new SpecException(origin + ": 'import' section is required (it names the target table)");
        }
        rejectUnknown(n, IMPORT_KEYS, origin, "import");
        String target = requireText(n, "target-table", origin);
        Identifiers.requireSqlIdentifier(target, "import.target-table");

        ImportMode mode = null;
        if (restore == RestoreMode.IMPORT) {
            mode = enumValue(ImportMode.class, requireText(n, "mode", origin), origin, "import.mode");
        } else if (n.has("mode")) {
            throw new SpecException(origin + ": import.mode is only valid when restore: IMPORT");
        }

        Map<String, SnapshotTableSpec.Column> columns = new LinkedHashMap<>();
        JsonNode colsNode = n.get("columns");
        if (colsNode != null && !colsNode.isNull()) {
            for (Iterator<String> it = colsNode.fieldNames(); it.hasNext(); ) {
                String targetCol = it.next();
                Identifiers.requireSqlIdentifier(targetCol, "import.columns key");
                JsonNode c = colsNode.get(targetCol);
                rejectUnknown(c, COLUMN_KEYS, origin, "import.columns." + targetCol);
                String src = optionalText(c, "source");
                String converter = optionalText(c, "converter");
                String constant = c.has("constant") && !c.get("constant").isNull()
                        ? c.get("constant").asText() : null;
                boolean useDefault = c.has("use-default") && c.get("use-default").asBoolean();
                boolean selfSourcing = converter != null && converter.startsWith("uuid-v5:");
                int provided = (src != null ? 1 : 0) + (constant != null ? 1 : 0) + (useDefault ? 1 : 0);
                if (provided != 1 && !(provided == 0 && selfSourcing)) {
                    throw new SpecException(origin + ": import.columns." + targetCol
                            + " must set exactly one of source, constant or use-default");
                }
                if (src != null) {
                    Identifiers.requireSqlIdentifier(src, "import.columns." + targetCol + ".source");
                }
                if (converter != null && src == null && !selfSourcing) {
                    throw new SpecException(origin + ": import.columns." + targetCol
                            + " declares a converter without a source column");
                }
                if (selfSourcing && src != null) {
                    throw new SpecException(origin + ": import.columns." + targetCol
                            + " uses a self-sourcing converter, which derives its value from the key "
                            + "columns named in the converter rather than a single source column");
                }
                columns.put(targetCol, new SnapshotTableSpec.Column(src, converter, constant, useDefault));
            }
        }

        List<String> ignore = stringList(n, "ignore-source-columns", origin);
        List<String> defaults = stringList(n, "use-target-defaults", origin);
        ignore.forEach(c -> Identifiers.requireSqlIdentifier(c, "import.ignore-source-columns"));
        defaults.forEach(c -> Identifiers.requireSqlIdentifier(c, "import.use-target-defaults"));

        String selectResource = optionalText(n, "select-resource");
        if (selectResource != null && !selectResource.startsWith("classpath:/snapshot/sql/")) {
            throw new SpecException(origin + ": import.select-resource must be a built-in "
                    + "classpath:/snapshot/sql/ resource, got " + selectResource);
        }
        int transformVersion = n.has("transform-version") ? n.get("transform-version").asInt() : 1;
        List<String> dependencies = stringList(n, "dependencies", origin);
        BatchBoundary boundary = n.has("batch-boundary")
                ? enumValue(BatchBoundary.class, n.get("batch-boundary").asText(), origin, "import.batch-boundary")
                : BatchBoundary.FILE_SET;
        int batchSize = n.has("batch-size") ? n.get("batch-size").asInt() : 0;
        if (batchSize < 0) {
            throw new SpecException(origin + ": import.batch-size must not be negative");
        }

        SnapshotTableSpec.TargetPartitioning partitioning = null;
        JsonNode tp = n.get("target-partitioning");
        if (tp != null && !tp.isNull()) {
            rejectUnknown(tp, TARGET_PARTITIONING_KEYS, origin, "import.target-partitioning");
            String col = requireText(tp, "column", origin);
            String prefix = requireText(tp, "partition-prefix", origin);
            Identifiers.requireSqlIdentifier(col, "import.target-partitioning.column");
            Identifiers.requireSqlIdentifier(prefix + "0", "import.target-partitioning.partition-prefix");
            partitioning = new SnapshotTableSpec.TargetPartitioning(col, prefix);
        }

        String handler = optionalText(n, "handler");

        return new SnapshotTableSpec.Import(target, mode, Map.copyOf(columns), List.copyOf(ignore),
                List.copyOf(defaults), selectResource, transformVersion, List.copyOf(dependencies),
                boundary, batchSize, partitioning, handler);
    }

    private Map<String, String> readLossy(JsonNode n, String origin) {
        if (n == null || n.isNull()) {
            return Map.of();
        }
        if (!n.isObject()) {
            throw new SpecException(origin + ": 'lossy' must be a mapping of column to reason");
        }
        Map<String, String> out = new LinkedHashMap<>();
        for (Iterator<String> it = n.fieldNames(); it.hasNext(); ) {
            String col = it.next();
            Identifiers.requireSqlIdentifier(col, "lossy key");
            String reason = n.get(col).asText().trim();
            if (reason.isEmpty()) {
                throw new SpecException(origin + ": lossy." + col + " needs a reason");
            }
            out.put(col, reason);
        }
        return Map.copyOf(out);
    }

    private SnapshotTableSpec.Validation readValidation(JsonNode n, String origin) {
        if (n == null || n.isNull()) {
            return new SnapshotTableSpec.Validation(List.of(), List.of(), List.of(), List.of());
        }
        rejectUnknown(n, VALIDATION_KEYS, origin, "validation");
        List<String> key = stringList(n, "key", origin);
        List<String> bounds = stringList(n, "bounds", origin);
        List<String> required = stringList(n, "required-columns", origin);
        List<String> sourceKey = stringList(n, "source-key", origin);
        key.forEach(c -> Identifiers.requireSqlIdentifier(c, "validation.key"));
        bounds.forEach(c -> Identifiers.requireSqlIdentifier(c, "validation.bounds"));
        required.forEach(c -> Identifiers.requireSqlIdentifier(c, "validation.required-columns"));
        sourceKey.forEach(c -> Identifiers.requireSqlIdentifier(c, "validation.source-key"));
        return new SnapshotTableSpec.Validation(List.copyOf(key), List.copyOf(bounds),
                List.copyOf(required), List.copyOf(sourceKey));
    }

    private static byte[] readTransform(String resourceRef, String origin) {
        String path = resourceRef.substring("classpath:".length());
        try (InputStream in = SnapshotSpecLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new SpecException(origin + ": SQL transform resource not found: " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new SpecException(origin + ": unable to read SQL transform resource " + path, e);
        }
    }

    // ---------------------------------------------------------------- cross-section rules

    private void validateSemantics(SnapshotTableSpec spec, String origin) {
        SnapshotTableSpec.Import imp = spec.importSpec();
        switch (spec.restore()) {
            case IMPORT -> {
                if (spec.source() == null) {
                    throw new SpecException(origin + ": restore: IMPORT requires a 'source' section");
                }
                if (imp.mode() == ImportMode.SQL) {
    
                } else if (imp.selectResource() != null) {
                    throw new SpecException(origin + ": import.select-resource is only valid in SQL mode");
                }
                if (imp.mode() == ImportMode.DIRECT && !imp.columns().isEmpty()) {
                    throw new SpecException(origin + ": DIRECT mode must not declare import.columns; use MAPPED");
                }
                if (imp.handler() != null) {
                    throw new SpecException(origin + ": import.handler is only valid when restore: HANDLER");
                }
                if (spec.validation().key().isEmpty()) {
                    throw new SpecException(origin + ": validation.key is required for imported tables");
                }
            }
            case HANDLER -> {
                if (imp.handler() == null || imp.handler().isBlank()) {
                    throw new SpecException(origin + ": restore: HANDLER requires import.handler");
                }
                if (spec.source() != null) {
                    throw new SpecException(origin + ": restore: HANDLER must not declare a 'source' section");
                }
            }
            case EMPTY_EXPECTED, RUNTIME_REBUILT, NOT_RESTORED -> {
                if (imp.handler() != null) {
                    throw new SpecException(origin + ": import.handler is only valid when restore: HANDLER");
                }
                if (!imp.columns().isEmpty()) {
                    throw new SpecException(origin + ": restore: " + spec.restore() + " must not declare import.columns");
                }
            }
        }

        if (spec.source() == null
                && spec.consistency().completedEpoch().type() != CompletedEpochType.NONE) {
            throw new SpecException(origin + ": a table without a source cannot gate the consistency point");
        }
        if (spec.restore() == RestoreMode.IMPORT
                && spec.importSpec().batchBoundary() == BatchBoundary.WHOLE_PARTITION
                && spec.source().partition().strategy() == PartitionStrategy.NONE) {
            throw new SpecException(origin + ": WHOLE_PARTITION batching requires a partitioned source");
        }

        for (Map.Entry<String, String> e : spec.lossy().entrySet()) {
            if (!imp.useTargetDefaults().contains(e.getKey())) {
                throw new SpecException(origin + ": lossy column '" + e.getKey()
                        + "' must also appear in import.use-target-defaults so the mapping is explicit");
            }
        }

        Set<String> dup = new TreeSet<>(imp.ignoreSourceColumns());
        if (dup.size() != imp.ignoreSourceColumns().size()) {
            throw new SpecException(origin + ": duplicate entries in import.ignore-source-columns");
        }
        for (String d : imp.useTargetDefaults()) {
            if (imp.columns().containsKey(d)) {
                throw new SpecException(origin + ": column '" + d
                        + "' is both explicitly mapped and listed in use-target-defaults");
            }
        }
    }

    // ---------------------------------------------------------------- primitives

    private static void rejectUnknown(JsonNode node, Set<String> allowed, String origin, String path) {
        if (node == null || !node.isObject()) {
            throw new SpecException(origin + ": '" + path + "' must be a mapping");
        }
        List<String> unknown = new ArrayList<>();
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            String f = it.next();
            if (!allowed.contains(f)) {
                unknown.add(f);
            }
        }
        if (!unknown.isEmpty()) {
            throw new SpecException(origin + ": unknown key(s) under '" + path + "': " + unknown
                    + ". Allowed: " + new TreeSet<>(allowed));
        }
    }

    private static String requireText(JsonNode n, String field, String origin) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull() || v.asText().isBlank()) {
            throw new SpecException(origin + ": '" + field + "' is required");
        }
        return v.asText().trim();
    }

    private static String optionalText(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return (v == null || v.isNull()) ? null : v.asText().trim();
    }

    private static int requireInt(JsonNode n, String field, String origin) {
        JsonNode v = n.get(field);
        if (v == null || !v.canConvertToInt()) {
            throw new SpecException(origin + ": '" + field + "' must be an integer");
        }
        return v.asInt();
    }

    private static List<String> stringList(JsonNode n, String field, String origin) {
        JsonNode v = n.get(field);
        if (v == null || v.isNull()) {
            return new ArrayList<>();
        }
        if (!v.isArray()) {
            throw new SpecException(origin + ": '" + field + "' must be a list");
        }
        List<String> out = new ArrayList<>();
        v.forEach(e -> out.add(e.asText().trim()));
        return out;
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, String origin, String path) {
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new SpecException(origin + ": invalid " + path + " '" + raw + "'. Allowed: "
                    + java.util.Arrays.toString(type.getEnumConstants()));
        }
    }

    static String utf8(byte[] b) {
        return new String(b, StandardCharsets.UTF_8);
    }
}
