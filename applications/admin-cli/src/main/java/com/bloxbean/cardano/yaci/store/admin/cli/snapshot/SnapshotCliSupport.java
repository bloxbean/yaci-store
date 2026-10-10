package com.bloxbean.cardano.yaci.store.admin.cli.snapshot;

import com.bloxbean.cardano.yaci.store.snapshot.export.ExportOptions;
import com.bloxbean.cardano.yaci.store.snapshot.load.ImportOptions;
import com.bloxbean.cardano.yaci.store.snapshot.load.PgSchema;
import com.bloxbean.cardano.yaci.store.snapshot.spec.SnapshotSpecRegistry;
import lombok.RequiredArgsConstructor;
import com.bloxbean.cardano.yaci.store.common.config.StoreModuleConfig;
import com.bloxbean.cardano.yaci.store.common.config.SchemaProfile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the admin CLI's existing Spring configuration into the plain option records the snapshot
 * component expects.
 *
 * <p>Datasource credentials are read from the same configuration the CLI already uses; they are
 * never accepted as command arguments and never written to a manifest, report or log.
 */
@Component
@RequiredArgsConstructor
public class SnapshotCliSupport {

    private final Environment environment;

    public String jdbcUrl() {
        return required("spring.datasource.url");
    }

    public String dbUser() {
        return environment.getProperty("spring.datasource.username", "");
    }

    public String dbPassword() {
        return environment.getProperty("spring.datasource.password", "");
    }

    /** Target schema, taken from {@code currentSchema} in the JDBC URL as the rest of the CLI does. */
    public String schema() {
        String url = jdbcUrl();
        if (url.contains("currentSchema=")) {
            return url.split("currentSchema=")[1].split("&")[0];
        }
        return "public";
    }

    public long protocolMagic() {
        return Long.parseLong(environment.getProperty("store.cardano.protocol-magic", "764824073"));
    }

    public String network() {
        return networkOf(protocolMagic());
    }

    public static String networkOf(long protocolMagic) {
        return switch ((int) protocolMagic) {
            case 764824073 -> "mainnet";
            case 1 -> "preprod";
            case 2 -> "preview";
            case 4 -> "sanchonet";
            default -> "custom-" + protocolMagic;
        };
    }

    public long eventPublisherId() {
        return Long.parseLong(environment.getProperty("store.event-publisher-id", "1"));
    }

    public int cursorBlocksToKeep() {
        return Integer.parseInt(environment.getProperty("store.cardano.cursor-no-of-blocks-to-keep", "2160"));
    }

    public String analyticsDataDir() {
        return environment.getProperty("yaci.store.analytics.export-path", "./data/analytics");
    }

    public String yaciStoreVersion() {
        return environment.getProperty("yaci.store.version",
                getClass().getPackage().getImplementationVersion() != null
                        ? getClass().getPackage().getImplementationVersion() : "unknown");
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl(), dbUser(), dbPassword());
    }

    /** Modules considered enabled, recorded in the manifest as the snapshot's profile. */
    public List<String> enabledModules() {
        return StoreModuleConfig.configured(environment).entrySet().stream()
                .filter(Map.Entry::getValue).map(Map.Entry::getKey).sorted().toList();
    }

    /** Use the consuming application's recorded classpath and effective flags, not CLI dependencies. */
    public SnapshotSpecRegistry importRegistry(String specFile, boolean allowCustomSpecs) throws SQLException {
        SnapshotSpecRegistry registry = registry(specFile, allowCustomSpecs);
        try (Connection conn = connect()) {
            Map<String, Boolean> profile = SchemaProfile.read(conn, schema());
            if (profile.isEmpty()) {
                throw new IllegalStateException("Target schema has no store profile. Run the consuming application once "
                        + "with store.schema-only=true and the intended store flags, then retry import.");
            }
            for (var module : StoreModuleConfig.MODULES.entrySet()) {
                String property = module.getValue().property();
                if (property != null && environment.containsProperty(property)
                        && !environment.getProperty(property, Boolean.class).equals(profile.get(module.getKey()))) {
                    throw new IllegalStateException("Configured " + property + " differs from the target schema-only profile. "
                            + "Use the same application configuration for initialization and import.");
                }
            }
            // Custom snapshot modules need an explicit enabled flag and existing target tables.
            for (var spec : registry.all()) {
                if (!StoreModuleConfig.MODULES.containsKey(spec.module())) {
                    profile = new LinkedHashMap<>(profile);
                    profile.put(spec.module(), environment.getProperty("store." + spec.module() + ".enabled", Boolean.class, false));
                }
            }
            return registry.selectModules(profile.entrySet().stream().filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet()));
        }
    }

    /**
     * Specs for producing a snapshot from this source: the imported tables of store modules the
     * source has disabled are left out, since it never writes their history.
     */
    public SnapshotSpecRegistry exportRegistry(String specFile, boolean allowCustomSpecs) {
        java.util.Set<String> disabled = StoreModuleConfig.configured(environment).entrySet().stream()
                .filter(e -> !e.getValue()).map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
        return registry(specFile, allowCustomSpecs).withoutImportedTablesOf(disabled);
    }

    public Map<String, String> pruningSettings() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("store.utxo.pruning-enabled", environment.getProperty("store.utxo.pruning-enabled", "false"));
        out.put("store.transaction.pruning-enabled",
                environment.getProperty("store.transaction.pruning-enabled", "false"));
        out.put("store.adapot.reward-pruning-enabled",
                environment.getProperty("store.adapot.reward-pruning-enabled", "false"));
        out.put("store.adapot.epoch-stake-pruning-enabled",
                environment.getProperty("store.adapot.epoch-stake-pruning-enabled", "false"));
        out.put("store.account.pruning-enabled",
                environment.getProperty("store.account.pruning-enabled", "false"));
        return out;
    }

    public SnapshotSpecRegistry registry(String specFile, boolean allowCustomSpecs) {
        List<Path> custom = new ArrayList<>();
        if (specFile != null && !specFile.isBlank()) {
            for (String s : specFile.split(",")) {
                custom.add(Path.of(s.trim()));
            }
        }
        return SnapshotSpecRegistry.load(custom, allowCustomSpecs);
    }

    public ExportOptions exportOptions(String dataDir, String outputDir, String workDir, String partSize,
                                       int targetEpoch, long minConfirmations, boolean allowIncomplete,
                                       boolean unsigned) throws SQLException {
        String schemaFingerprint;
        String flywayFingerprint;
        Map<String, String> tableSchemaFingerprints;
        Map<String, Map<String, Long>> completedPartitions = new LinkedHashMap<>();
        try (Connection conn = connect()) {
            PgSchema pgs = new PgSchema(conn, schema());
            schemaFingerprint = pgs.fingerprint();
            flywayFingerprint = pgs.flywayFingerprint();
            tableSchemaFingerprints = pgs.tableFingerprints();
            String stateTable = com.bloxbean.cardano.yaci.store.snapshot.util.Identifiers.quote(schema())
                    + ".analytics_export_state";
            try (var st = conn.createStatement();
                 var rs = st.executeQuery("SELECT table_name, partition_value, row_count FROM " + stateTable
                         + " WHERE export_status = 'COMPLETED' AND row_count IS NOT NULL")) {
                while (rs.next()) {
                    completedPartitions.computeIfAbsent(rs.getString(1), k -> new LinkedHashMap<>())
                            .put(rs.getString(2), rs.getLong(3));
                }
            }
        }
        return new ExportOptions(
                Path.of(dataDir != null ? dataDir : analyticsDataDir()).toAbsolutePath().normalize(),
                Path.of(workDir).toAbsolutePath().normalize(),
                Path.of(outputDir).toAbsolutePath().normalize(),
                network(), protocolMagic(), null,
                parseSize(partSize), targetEpoch, minConfirmations, allowIncomplete, unsigned,
                yaciStoreVersion(), enabledModules(), pruningSettings(),
                schemaFingerprint, flywayFingerprint, completedPartitions, tableSchemaFingerprints);
    }

    public ImportOptions importOptions(String manifest, String workDir, int workers, String memoryLimit,
                                       long minFreeDiskGb, boolean allowUnsigned, String specFile,
                                       boolean allowCustomSpecs, boolean keepExtracted) {
        Path manifestPath = Path.of(manifest).toAbsolutePath().normalize();
        List<Path> custom = new ArrayList<>();
        if (specFile != null && !specFile.isBlank()) {
            for (String s : specFile.split(",")) {
                custom.add(Path.of(s.trim()));
            }
        }
        return new ImportOptions(manifestPath, manifestPath.getParent(),
                Path.of(workDir).toAbsolutePath().normalize(),
                jdbcUrl(), dbUser(), dbPassword(), schema(), network(), protocolMagic(),
                eventPublisherId(), cursorBlocksToKeep(), workers, memoryLimit, minFreeDiskGb,
                allowUnsigned, allowCustomSpecs, custom, keepExtracted, false);
    }

    public static void ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot create directory " + path + ": " + e.getMessage());
        }
    }

    /** Accepts {@code 8GiB}, {@code 512MiB}, {@code 2GB} or a plain byte count. */
    public static long parseSize(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String v = value.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        long multiplier = 1;
        if (v.endsWith("GIB") || v.endsWith("GB")) {
            multiplier = 1024L * 1024 * 1024;
            v = v.substring(0, v.length() - (v.endsWith("GIB") ? 3 : 2));
        } else if (v.endsWith("MIB") || v.endsWith("MB")) {
            multiplier = 1024L * 1024;
            v = v.substring(0, v.length() - (v.endsWith("MIB") ? 3 : 2));
        } else if (v.endsWith("KIB") || v.endsWith("KB")) {
            multiplier = 1024L;
            v = v.substring(0, v.length() - (v.endsWith("KIB") ? 3 : 2));
        }
        return (long) (Double.parseDouble(v) * multiplier);
    }

    public static String humanBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KiB", "MiB", "GiB", "TiB"};
        double v = bytes / 1024.0;
        int i = 0;
        while (v >= 1024 && i < units.length - 1) {
            v /= 1024;
            i++;
        }
        return String.format("%.1f %s", v, units[i]);
    }

    public static String humanMillis(long millis) {
        long s = millis / 1000;
        if (s < 60) {
            return s + "s";
        }
        return String.format("%dm %02ds", s / 60, s % 60);
    }

    private String required(String key) {
        String v = environment.getProperty(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Required property '" + key + "' is not configured");
        }
        return v;
    }
}
