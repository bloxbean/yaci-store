package com.bloxbean.cardano.yaci.store.snapshot.export;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Inputs to {@code snapshot inspect} and {@code snapshot export}.
 *
 * <p>No credential ever reaches here: the schema and Flyway fingerprints are computed by the caller
 * from its own configured datasource and passed in already reduced to digests.
 *
 * @param targetEpoch      restore to this completed epoch instead of the newest supported one.
 *                         Must not exceed what the gating tables support. 0 means "newest".
 * @param minConfirmations blocks the point must sit behind the newest exported block
 * @param allowIncomplete  publish even though some tables are declared lossy; development only
 * @param unsigned         acknowledge that no signature will be produced
 */
public record ExportOptions(Path dataDir,
                            Path workDir,
                            Path outputDir,
                            String network,
                            long protocolMagic,
                            String genesisHash,
                            long partSizeBytes,
                            int targetEpoch,
                            long minConfirmations,
                            boolean allowIncomplete,
                            boolean unsigned,
                            String yaciStoreVersion,
                            List<String> modules,
                            Map<String, String> pruningSettings,
                            String schemaFingerprint,
                            String flywayFingerprint) {

    public static final long DEFAULT_MIN_CONFIRMATIONS = 2160;
}
