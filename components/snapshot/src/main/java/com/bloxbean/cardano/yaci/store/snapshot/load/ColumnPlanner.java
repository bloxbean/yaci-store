package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.convert.ConverterRegistry;
import com.bloxbean.cardano.yaci.store.snapshot.convert.ValueConverter;
import com.bloxbean.cardano.yaci.store.snapshot.spec.ImportMode;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotTableSpec;
import com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Reconciles a source relation's columns with its PostgreSQL target and produces an executable
 * {@link ColumnPlan}.
 *
 * <p>The accounting is deliberately total. After explicit mappings, ignored export columns and
 * declared target defaults are applied, every remaining source column must match a target column by
 * identical name with a compatible type, and every remaining target column must have been produced.
 * Anything left over is an error.
 *
 * <p>That strictness is the whole point: a permissive name-based mapper silently writes NULL when a
 * column is renamed or added, which produces a database that looks populated but is wrong.
 */
public class ColumnPlanner {

    private final ConverterRegistry converters;

    public ColumnPlanner(ConverterRegistry converters) {
        this.converters = converters;
    }

    /** Raised when the source schema, the target schema and the specification do not agree. */
    public static class MappingException extends RuntimeException {
        public MappingException(String message) {
            super(message);
        }
    }

    /**
     * @param spec          the table specification
     * @param sourceColumns source column name to DuckLake type (for SQL mode: the transform's output
     *                      columns)
     * @param target        the PostgreSQL target table
     */
    public ColumnPlan plan(SnapshotTableSpec spec, Map<String, String> sourceColumns, TargetTable target) {
        SnapshotTableSpec.Import imp = spec.importSpec();
        List<String> problems = new ArrayList<>();

        Set<String> remainingSource = new LinkedHashSet<>(sourceColumns.keySet());
        Set<String> remainingTarget = new LinkedHashSet<>(target.columns().keySet());

        for (String ignored : imp.ignoreSourceColumns()) {
            if (!remainingSource.remove(ignored)) {
                problems.add("ignore-source-columns lists '" + ignored + "' which the source does not have");
            }
        }

        List<String> defaulted = new ArrayList<>();
        for (String d : imp.useTargetDefaults()) {
            if (!remainingTarget.remove(d)) {
                problems.add("use-target-defaults lists '" + d + "' which the target does not have");
            } else {
                defaulted.add(d);
            }
        }

        List<String> targetColumns = new ArrayList<>();
        List<String> expressions = new ArrayList<>();

        // 1. Explicit mappings.
        for (Map.Entry<String, SnapshotTableSpec.Column> e : sortedByTargetOrder(imp.columns(), target)) {
            String targetCol = e.getKey();
            SnapshotTableSpec.Column mapping = e.getValue();
            if (!remainingTarget.remove(targetCol)) {
                problems.add("import.columns maps '" + targetCol
                        + "' which is not a target column of " + target.name());
                continue;
            }
            if (mapping.useDefault()) {
                defaulted.add(targetCol);
                continue;
            }
            if (mapping.constant() != null) {
                targetColumns.add(targetCol);
                expressions.add(Identifiers.literal(mapping.constant()));
                continue;
            }

            if (mapping.source() == null) {
                // Self-sourcing converter (uuid-v5): the value comes from the key columns named in
                // the converter, which are themselves mapped to their own target columns.
                ValueConverter conv = converters.get(mapping.converter());
                String reject = conv.reject("", lower(target.type(targetCol)));
                if (reject != null) {
                    problems.add("import.columns." + targetCol + ": " + reject);
                }
                if (conv instanceof com.bloxbean.cardano.yaci.store.snapshot.convert.UuidV5Converter u) {
                    for (String kc : u.keyColumns()) {
                        if (!sourceColumns.containsKey(kc)) {
                            problems.add("import.columns." + targetCol + " derives a UUID from '" + kc
                                    + "' which the export does not contain");
                        }
                    }
                }
                targetColumns.add(targetCol);
                expressions.add(conv.toSql(null));
                continue;
            }

            String sourceCol = mapping.source();
            if (!sourceColumns.containsKey(sourceCol)) {
                problems.add("import.columns." + targetCol + " reads source column '" + sourceCol
                        + "' which the export does not contain");
                continue;
            }
            remainingSource.remove(sourceCol);

            String expr = Identifiers.quote(sourceCol);
            if (mapping.converter() != null) {
                ValueConverter conv = converters.get(mapping.converter());
                String reject = conv.reject(lower(sourceColumns.get(sourceCol)), lower(target.type(targetCol)));
                if (reject != null) {
                    problems.add("import.columns." + targetCol + ": " + reject);
                }
                expr = conv.toSql(expr);
            } else {
                String incompat = TypeCompatibility.reject(sourceColumns.get(sourceCol), target.type(targetCol));
                if (incompat != null) {
                    problems.add("import.columns." + targetCol + " maps '" + sourceCol + "': " + incompat
                            + ". Declare a converter.");
                }
            }
            targetColumns.add(targetCol);
            expressions.add(expr);
        }

        // 2. Automatic mapping, by identical name only, and only when the types are compatible.
        for (String col : new ArrayList<>(remainingTarget)) {
            if (!remainingSource.contains(col)) {
                continue;
            }
            String incompat = TypeCompatibility.reject(sourceColumns.get(col), target.type(col));
            if (incompat != null) {
                problems.add("column '" + col + "' matches by name but " + incompat
                        + ". Map it explicitly with a converter.");
            }
            remainingTarget.remove(col);
            remainingSource.remove(col);
            targetColumns.add(col);
            expressions.add(Identifiers.quote(col));
        }

        // 3. Nothing may be left unaccounted for.
        if (!remainingSource.isEmpty()) {
            problems.add("source column(s) " + new TreeSet<>(remainingSource)
                    + " are neither mapped nor listed in ignore-source-columns");
        }
        if (!remainingTarget.isEmpty()) {
            problems.add("target column(s) " + new TreeSet<>(remainingTarget)
                    + " would be left unwritten. Map them, or list them in use-target-defaults.");
        }

        if (imp.mode() == ImportMode.DIRECT && !problems.isEmpty()) {
            problems.add("DIRECT mode requires identical names and compatible types; use MAPPED instead");
        }

        if (!problems.isEmpty()) {
            throw new MappingException("Snapshot spec '" + spec.id() + "' does not match the schemas:\n  - "
                    + String.join("\n  - ", problems));
        }
        return new ColumnPlan(List.copyOf(targetColumns), List.copyOf(expressions), List.copyOf(defaulted));
    }

    /** Deterministic mapping order: follow the target table's ordinal order. */
    private static List<Map.Entry<String, SnapshotTableSpec.Column>> sortedByTargetOrder(
            Map<String, SnapshotTableSpec.Column> columns, TargetTable target) {
        List<String> order = new ArrayList<>(target.columns().keySet());
        List<Map.Entry<String, SnapshotTableSpec.Column>> entries = new ArrayList<>(columns.entrySet());
        entries.sort((a, b) -> {
            int ia = order.indexOf(a.getKey());
            int ib = order.indexOf(b.getKey());
            if (ia < 0 && ib < 0) {
                return a.getKey().compareTo(b.getKey());
            }
            if (ia < 0) {
                return 1;
            }
            if (ib < 0) {
                return -1;
            }
            return Integer.compare(ia, ib);
        });
        return entries;
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
