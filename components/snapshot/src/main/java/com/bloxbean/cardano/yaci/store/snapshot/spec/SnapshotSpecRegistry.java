package com.bloxbean.cardano.yaci.store.snapshot.spec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Deterministic registry of table specifications.
 *
 * <p>Built-in specs are collected at build time into {@code /snapshot/specs.index} so no classpath
 * scanning, Spring context or store-module class is required to read them. Operator specs are only
 * ever loaded from an explicit local path and can never shadow a built-in definition: mapping rules
 * are executable, so a downloaded archive must not be able to introduce or replace one.
 */
public class SnapshotSpecRegistry {

    private static final Logger log = LoggerFactory.getLogger(SnapshotSpecRegistry.class);
    public static final String INDEX_RESOURCE = "/snapshot/specs.index";

    private final Map<String, SnapshotTableSpec> byKey = new TreeMap<>();
    private final Map<String, SnapshotTableSpec> byId = new TreeMap<>();
    private final Map<String, SnapshotTableSpec> byTargetTable = new TreeMap<>();
    private final List<String> builtInKeys = new ArrayList<>();

    private SnapshotSpecRegistry() {
    }

    /** Registry containing only the specifications shipped with this Yaci Store release. */
    public static SnapshotSpecRegistry builtIn() {
        SnapshotSpecRegistry registry = new SnapshotSpecRegistry();
        registry.loadBuiltIn();
        return registry;
    }

    /**
     * @param customPaths YAML files or directories explicitly named by the operator
     * @param allowCustom must be true when {@code customPaths} is non-empty; mirrors
     *                    {@code --allow-custom-specs} so custom mapping rules are never silently used
     */
    public static SnapshotSpecRegistry load(List<Path> customPaths, boolean allowCustom) {
        SnapshotSpecRegistry registry = builtIn();
        if (customPaths == null || customPaths.isEmpty()) {
            return registry;
        }
        if (!allowCustom) {
            throw new SpecException("Custom specification files were supplied without --allow-custom-specs");
        }
        registry.loadCustom(customPaths);
        return registry;
    }

    private void loadBuiltIn() {
        SnapshotSpecLoader loader = new SnapshotSpecLoader();
        for (String resource : readIndex()) {
            String path = "/snapshot/" + resource;
            try (InputStream in = SnapshotSpecRegistry.class.getResourceAsStream(path)) {
                if (in == null) {
                    throw new SpecException("Specification listed in index but missing from classpath: " + path);
                }
                register(loader.loadStream(in, "classpath:" + path), true);
            } catch (IOException e) {
                throw new SpecException("Unable to read " + path, e);
            }
        }
        log.debug("Loaded {} built-in snapshot specifications", byKey.size());
    }

    private void loadCustom(List<Path> paths) {
        SnapshotSpecLoader loader = new SnapshotSpecLoader();
        for (Path p : expand(paths)) {
            register(loader.loadFile(p), false);
        }
    }

    private static List<Path> expand(List<Path> paths) {
        List<Path> files = new ArrayList<>();
        for (Path p : paths) {
            if (Files.isDirectory(p)) {
                try (Stream<Path> s = Files.list(p)) {
                    s.filter(f -> f.getFileName().toString().endsWith(".yml"))
                            .sorted()
                            .forEach(files::add);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } else if (Files.isRegularFile(p)) {
                files.add(p);
            } else {
                throw new SpecException("Specification path does not exist: " + p);
            }
        }
        return files;
    }

    private static List<String> readIndex() {
        try (InputStream in = SnapshotSpecRegistry.class.getResourceAsStream(INDEX_RESOURCE)) {
            if (in == null) {
                return List.of();
            }
            List<String> out = new ArrayList<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    String t = line.trim();
                    if (!t.isEmpty() && !t.startsWith("#")) {
                        out.add(t);
                    }
                }
            }
            out.sort(Comparator.naturalOrder());
            return out;
        } catch (IOException e) {
            throw new SpecException("Unable to read " + INDEX_RESOURCE, e);
        }
    }

    private void register(SnapshotTableSpec spec, boolean builtIn) {
        String key = spec.key();
        SnapshotTableSpec existing = byKey.get(key);
        if (existing != null) {
            if (builtIn) {
                throw new SpecException("Duplicate built-in specification " + key
                        + " from " + existing.origin() + " and " + spec.origin());
            }
            throw new SpecException("Custom specification " + key + " (" + spec.origin()
                    + ") cannot override the built-in definition from " + existing.origin());
        }
        SnapshotTableSpec sameId = byId.get(spec.id());
        if (sameId != null && sameId.specVersion() != spec.specVersion()) {
            throw new SpecException("Specification id '" + spec.id() + "' is declared at versions "
                    + sameId.specVersion() + " and " + spec.specVersion()
                    + ". A release must select exactly one version per table.");
        }
        SnapshotTableSpec sameTarget = byTargetTable.get(spec.targetTable());
        if (sameTarget != null) {
            throw new SpecException("Target table '" + spec.targetTable() + "' is claimed by both '"
                    + sameTarget.id() + "' and '" + spec.id() + "'");
        }
        byKey.put(key, spec);
        byId.put(spec.id(), spec);
        byTargetTable.put(spec.targetTable(), spec);
        if (builtIn) {
            builtInKeys.add(key);
        }
    }

    /** Specs in deterministic id order. */
    public List<SnapshotTableSpec> all() {
        return new ArrayList<>(byId.values());
    }

    /** Specs whose data is packaged into a snapshot, in dependency-safe load order. */
    public List<SnapshotTableSpec> importedTables() {
        List<SnapshotTableSpec> imported = byId.values().stream()
                .filter(s -> s.restore() == RestoreMode.IMPORT)
                .toList();
        return orderByDependencies(imported);
    }

    public List<SnapshotTableSpec> handlerTables() {
        return byId.values().stream().filter(s -> s.restore() == RestoreMode.HANDLER).toList();
    }

    public Optional<SnapshotTableSpec> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Optional<SnapshotTableSpec> byTarget(String targetTable) {
        return Optional.ofNullable(byTargetTable.get(targetTable));
    }

    public boolean isBuiltIn(SnapshotTableSpec spec) {
        return builtInKeys.contains(spec.key());
    }

    /** Spec id to SHA-256 digest, as recorded in and checked against the manifest. */
    public Map<String, String> digests() {
        Map<String, String> out = new TreeMap<>();
        byId.forEach((id, spec) -> out.put(id, spec.digest()));
        return out;
    }

    /**
     * Topological order with a deterministic tie-break on spec id, so a manifest produced twice from
     * the same registry lists tables identically.
     */
    public static List<SnapshotTableSpec> orderByDependencies(Collection<SnapshotTableSpec> specs) {
        Map<String, SnapshotTableSpec> index = new TreeMap<>();
        specs.forEach(s -> index.put(s.id(), s));

        Map<String, Integer> state = new LinkedHashMap<>();
        List<SnapshotTableSpec> ordered = new ArrayList<>();
        for (String id : index.keySet()) {
            visit(id, index, state, ordered, new ArrayList<>());
        }
        return ordered;
    }

    private static void visit(String id, Map<String, SnapshotTableSpec> index, Map<String, Integer> state,
                              List<SnapshotTableSpec> ordered, List<String> path) {
        Integer s = state.get(id);
        if (s != null && s == 2) {
            return;
        }
        if (s != null && s == 1) {
            throw new SpecException("Circular snapshot dependency: " + String.join(" -> ", path) + " -> " + id);
        }
        SnapshotTableSpec spec = index.get(id);
        if (spec == null) {
            throw new SpecException("Unknown dependency '" + id + "' referenced from " + path);
        }
        state.put(id, 1);
        path.add(id);
        List<String> deps = new ArrayList<>(spec.importSpec().dependencies());
        deps.sort(Comparator.naturalOrder());
        for (String d : deps) {
            visit(d, index, state, ordered, path);
        }
        path.remove(path.size() - 1);
        state.put(id, 2);
        ordered.add(spec);
    }
}
