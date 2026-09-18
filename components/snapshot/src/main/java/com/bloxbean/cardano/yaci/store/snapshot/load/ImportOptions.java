package com.bloxbean.cardano.yaci.store.snapshot.load;

import java.nio.file.Path;
import java.util.List;

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
                            boolean skipExtraction) {

    public static final int DEFAULT_WORKERS = 4;
    public static final String DEFAULT_MEMORY_LIMIT = "4GB";
    public static final long DEFAULT_MIN_FREE_DISK_GB = 20;
}
