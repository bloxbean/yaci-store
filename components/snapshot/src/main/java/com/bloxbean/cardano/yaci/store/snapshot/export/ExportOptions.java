package com.bloxbean.cardano.yaci.store.snapshot.export;

import com.bloxbean.cardano.yaci.store.snapshot.ducklake.DuckLakeCatalog;
import com.bloxbean.cardano.yaci.store.snapshot.load.TargetTable;

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
 * @param completedPartitions source export journal: exporter id → partition value → completed row count.
 *                            Must include zero-row partitions; missing entries block publication.
 * @param memoryLimit      DuckDB memory limit for the catalog connection, e.g. {@code 1GB}
 * @param targetTables     the imported tables' definitions in this release's PostgreSQL schema, by
 *                         name, so every mapping and SQL transform output is planned against them
 *                         before anything is packaged. Tables absent here are not planned.
 * @param historySplit     package the history of {@code address_utxo}, {@code tx_input} and
 *                         {@code epoch_stake} in parts of its own, so an import into a target that
 *                         prunes them skips it (ADR 0005, D11)
 * @param utxoSafeBlocks   history split: outputs spent, and inputs, before this many blocks before the
 *                         point are history
 * @param epochStakeEpochs history split: epochs older than the newest this many are history
 * @param yaciStoreCommit  git commit of this release, when known
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
                            String flywayFingerprint,
                            Map<String, Map<String, Long>> completedPartitions,
                            Map<String, String> tableSchemaFingerprints,
                            String memoryLimit,
                            Map<String, TargetTable> targetTables,
                            boolean historySplit,
                            int utxoSafeBlocks,
                            int epochStakeEpochs,
                            String yaciStoreCommit) {

    public static final int DEFAULT_UTXO_SAFE_BLOCKS = 2160;
    public static final int DEFAULT_EPOCH_STAKE_EPOCHS = 8;

    public ExportOptions(Path dataDir, Path workDir, Path outputDir, String network, long protocolMagic,
                         String genesisHash, long partSizeBytes, int targetEpoch, long minConfirmations,
                         boolean allowIncomplete, boolean unsigned, String yaciStoreVersion,
                         List<String> modules, Map<String, String> pruningSettings, String schemaFingerprint,
                         String flywayFingerprint, Map<String, Map<String, Long>> completedPartitions,
                         Map<String, String> tableSchemaFingerprints, String memoryLimit,
                         Map<String, TargetTable> targetTables) {
        this(dataDir, workDir, outputDir, network, protocolMagic, genesisHash, partSizeBytes, targetEpoch,
                minConfirmations, allowIncomplete, unsigned, yaciStoreVersion, modules, pruningSettings,
                schemaFingerprint, flywayFingerprint, completedPartitions, tableSchemaFingerprints, memoryLimit,
                targetTables, false, DEFAULT_UTXO_SAFE_BLOCKS, DEFAULT_EPOCH_STAKE_EPOCHS, null);
    }

    /** A copy with the history split on or off, at the given windows. */
    public ExportOptions withHistorySplit(boolean split, int newUtxoSafeBlocks, int newEpochStakeEpochs) {
        return new ExportOptions(dataDir, workDir, outputDir, network, protocolMagic, genesisHash, partSizeBytes,
                targetEpoch, minConfirmations, allowIncomplete, unsigned, yaciStoreVersion, modules, pruningSettings,
                schemaFingerprint, flywayFingerprint, completedPartitions, tableSchemaFingerprints, memoryLimit,
                targetTables, split, newUtxoSafeBlocks, newEpochStakeEpochs, yaciStoreCommit);
    }

    /** A copy that records {@code commit} as the release's git commit. */
    public ExportOptions withCommit(String commit) {
        return new ExportOptions(dataDir, workDir, outputDir, network, protocolMagic, genesisHash, partSizeBytes,
                targetEpoch, minConfirmations, allowIncomplete, unsigned, yaciStoreVersion, modules, pruningSettings,
                schemaFingerprint, flywayFingerprint, completedPartitions, tableSchemaFingerprints, memoryLimit,
                targetTables, historySplit, utxoSafeBlocks, epochStakeEpochs, commit);
    }

    public ExportOptions(Path dataDir, Path workDir, Path outputDir, String network, long protocolMagic,
                         String genesisHash, long partSizeBytes, int targetEpoch, long minConfirmations,
                         boolean allowIncomplete, boolean unsigned, String yaciStoreVersion,
                         List<String> modules, Map<String, String> pruningSettings, String schemaFingerprint,
                         String flywayFingerprint, Map<String, Map<String, Long>> completedPartitions,
                         Map<String, String> tableSchemaFingerprints, String memoryLimit) {
        this(dataDir, workDir, outputDir, network, protocolMagic, genesisHash, partSizeBytes, targetEpoch,
                minConfirmations, allowIncomplete, unsigned, yaciStoreVersion, modules, pruningSettings,
                schemaFingerprint, flywayFingerprint, completedPartitions, tableSchemaFingerprints,
                memoryLimit, Map.of());
    }

    public ExportOptions(Path dataDir, Path workDir, Path outputDir, String network, long protocolMagic,
                         String genesisHash, long partSizeBytes, int targetEpoch, long minConfirmations,
                         boolean allowIncomplete, boolean unsigned, String yaciStoreVersion,
                         List<String> modules, Map<String, String> pruningSettings, String schemaFingerprint,
                         String flywayFingerprint, Map<String, Map<String, Long>> completedPartitions,
                         Map<String, String> tableSchemaFingerprints) {
        this(dataDir, workDir, outputDir, network, protocolMagic, genesisHash, partSizeBytes, targetEpoch,
                minConfirmations, allowIncomplete, unsigned, yaciStoreVersion, modules, pruningSettings,
                schemaFingerprint, flywayFingerprint, completedPartitions, tableSchemaFingerprints,
                DuckLakeCatalog.DEFAULT_MEMORY_LIMIT);
    }

    public ExportOptions(Path dataDir, Path workDir, Path outputDir, String network, long protocolMagic,
                         String genesisHash, long partSizeBytes, int targetEpoch, long minConfirmations,
                         boolean allowIncomplete, boolean unsigned, String yaciStoreVersion,
                         List<String> modules, Map<String, String> pruningSettings, String schemaFingerprint,
                         String flywayFingerprint, Map<String, Map<String, Long>> completedPartitions) {
        this(dataDir, workDir, outputDir, network, protocolMagic, genesisHash, partSizeBytes, targetEpoch,
                minConfirmations, allowIncomplete, unsigned, yaciStoreVersion, modules, pruningSettings,
                schemaFingerprint, flywayFingerprint, completedPartitions, Map.of());
    }

    public static final long DEFAULT_MIN_CONFIRMATIONS = 2160;
}
