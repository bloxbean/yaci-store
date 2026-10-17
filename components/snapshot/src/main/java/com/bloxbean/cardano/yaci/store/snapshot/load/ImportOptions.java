package com.bloxbean.cardano.yaci.store.snapshot.load;

import com.bloxbean.cardano.yaci.store.snapshot.archive.PartSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Inputs to {@code snapshot import} and {@code snapshot validate}.
 *
 * <p>Credentials are never accepted as command arguments; the caller passes an already-resolved
 * JDBC URL, user and password from the same Spring configuration the admin CLI already uses, and
 * they are redacted from every message this component produces.
 *
 * @param workDir        where parts are extracted and where each worker gets its own spill directory
 * @param workers        bounded worker count
 * @param memoryLimit    per-worker DuckDB memory limit, e.g. {@code "4GB"}
 * @param minFreeDiskGb  the import aborts before crossing this floor
 * @param keepExtracted  leave extracted Parquet in place after a successful import
 * @param retention      what this import adds to the snapshot's own retention, e.g. keeping only the
 *                       newest epochs of {@code epoch_stake} ({@code Retention}); empty for none
 * @param targetSettings the target's store settings a retention depends on, e.g.
 *                       {@code store.utxo.pruning-enabled}; a pruned snapshot is refused unless the
 *                       target runs the matching pruning
 * @param partSource     where the parts are read from; null for the files in {@code archiveDir}. A
 *                       remote source is streamed straight into the extractor (ADR 0005, D5)
 * @param trustedKeys    public keys a snapshot's signature is checked against; without one that
 *                       verifies, import needs {@code allowUnsigned}
 */
public record ImportOptions(Path manifestPath,
                            Path archiveDir,
                            Path workDir,
                            String jdbcUrl,
                            String user,
                            String password,
                            String schema,
                            String network,
                            long protocolMagic,
                            long eventPublisherId,
                            int cursorBlocksToKeep,
                            int workers,
                            String memoryLimit,
                            long minFreeDiskGb,
                            boolean allowUnsigned,
                            boolean allowCustomSpecs,
                            List<Path> customSpecPaths,
                            boolean keepExtracted,
                            boolean skipExtraction,
                            boolean tunePostgres,
                            Map<String, String> retention,
                            Map<String, String> targetSettings,
                            PartSource partSource,
                            List<java.security.PublicKey> trustedKeys) {

    public ImportOptions(Path manifestPath, Path archiveDir, Path workDir, String jdbcUrl, String user,
                         String password, String schema, String network, long protocolMagic, long eventPublisherId,
                         int cursorBlocksToKeep, int workers, String memoryLimit, long minFreeDiskGb,
                         boolean allowUnsigned, boolean allowCustomSpecs, List<Path> customSpecPaths,
                         boolean keepExtracted, boolean skipExtraction, boolean tunePostgres,
                         Map<String, String> retention, Map<String, String> targetSettings, PartSource partSource) {
        this(manifestPath, archiveDir, workDir, jdbcUrl, user, password, schema, network, protocolMagic,
                eventPublisherId, cursorBlocksToKeep, workers, memoryLimit, minFreeDiskGb, allowUnsigned,
                allowCustomSpecs, customSpecPaths, keepExtracted, skipExtraction, tunePostgres, retention,
                targetSettings, partSource, List.of());
    }

    /** A copy that checks signatures against {@code keys}. */
    public ImportOptions withTrustedKeys(List<java.security.PublicKey> keys) {
        return new ImportOptions(manifestPath, archiveDir, workDir, jdbcUrl, user, password, schema, network,
                protocolMagic, eventPublisherId, cursorBlocksToKeep, workers, memoryLimit, minFreeDiskGb, allowUnsigned,
                allowCustomSpecs, customSpecPaths, keepExtracted, skipExtraction, tunePostgres, retention,
                targetSettings, partSource, keys);
    }

    public ImportOptions(Path manifestPath, Path archiveDir, Path workDir, String jdbcUrl, String user,
                         String password, String schema, String network, long protocolMagic, long eventPublisherId,
                         int cursorBlocksToKeep, int workers, String memoryLimit, long minFreeDiskGb,
                         boolean allowUnsigned, boolean allowCustomSpecs, List<Path> customSpecPaths,
                         boolean keepExtracted, boolean skipExtraction, boolean tunePostgres,
                         Map<String, String> retention, Map<String, String> targetSettings) {
        this(manifestPath, archiveDir, workDir, jdbcUrl, user, password, schema, network, protocolMagic,
                eventPublisherId, cursorBlocksToKeep, workers, memoryLimit, minFreeDiskGb, allowUnsigned,
                allowCustomSpecs, customSpecPaths, keepExtracted, skipExtraction, tunePostgres, retention,
                targetSettings, null);
    }

    /** A copy that reads its parts from {@code source}. */
    public ImportOptions withPartSource(PartSource source) {
        return new ImportOptions(manifestPath, archiveDir, workDir, jdbcUrl, user, password, schema, network,
                protocolMagic, eventPublisherId, cursorBlocksToKeep, workers, memoryLimit, minFreeDiskGb, allowUnsigned,
                allowCustomSpecs, customSpecPaths, keepExtracted, skipExtraction, tunePostgres, retention,
                targetSettings, source, trustedKeys);
    }

    /** The parts' source: {@link #partSource}, or the files in {@link #archiveDir}. */
    public PartSource parts() {
        return partSource != null ? partSource : PartSource.directory(archiveDir);
    }

    public ImportOptions {
        retention = retention == null ? Map.of() : Map.copyOf(retention);
        targetSettings = targetSettings == null ? Map.of() : Map.copyOf(targetSettings);
        trustedKeys = trustedKeys == null ? List.of() : List.copyOf(trustedKeys);
    }

    /** Without retention. */
    public ImportOptions(Path manifestPath, Path archiveDir, Path workDir, String jdbcUrl, String user,
                         String password, String schema, String network, long protocolMagic, long eventPublisherId,
                         int cursorBlocksToKeep, int workers, String memoryLimit, long minFreeDiskGb,
                         boolean allowUnsigned, boolean allowCustomSpecs, List<Path> customSpecPaths,
                         boolean keepExtracted, boolean skipExtraction, boolean tunePostgres) {
        this(manifestPath, archiveDir, workDir, jdbcUrl, user, password, schema, network, protocolMagic,
                eventPublisherId, cursorBlocksToKeep, workers, memoryLimit, minFreeDiskGb, allowUnsigned,
                allowCustomSpecs, customSpecPaths, keepExtracted, skipExtraction, tunePostgres, Map.of(), Map.of());
    }

    /** Without {@code --tune-postgres}. */
    public ImportOptions(Path manifestPath, Path archiveDir, Path workDir, String jdbcUrl, String user,
                         String password, String schema, String network, long protocolMagic, long eventPublisherId,
                         int cursorBlocksToKeep, int workers, String memoryLimit, long minFreeDiskGb,
                         boolean allowUnsigned, boolean allowCustomSpecs, List<Path> customSpecPaths,
                         boolean keepExtracted, boolean skipExtraction) {
        this(manifestPath, archiveDir, workDir, jdbcUrl, user, password, schema, network, protocolMagic,
                eventPublisherId, cursorBlocksToKeep, workers, memoryLimit, minFreeDiskGb, allowUnsigned,
                allowCustomSpecs, customSpecPaths, keepExtracted, skipExtraction, false);
    }

    public static final int DEFAULT_WORKERS = 1;
    public static final String DEFAULT_MEMORY_LIMIT = "1GB";
    public static final long DEFAULT_MIN_FREE_DISK_GB = 20;
}
