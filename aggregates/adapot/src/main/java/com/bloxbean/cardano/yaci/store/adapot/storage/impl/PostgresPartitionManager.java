package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.storage.PartitionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * PostgreSQL partition manager for the reward table.
 * Creates range partitions by spendable_epoch to improve write performance during reward calculations.
 *
 * <p><b>Index Inheritance:</b> Indexes are automatically inherited from the parent table.
 * When an index is created on the parent 'reward' table, PostgreSQL automatically creates
 * corresponding indexes on all existing partitions and any future partitions. No manual
 * index creation is needed on individual partitions.</p>
 *
 * <p><b>Memory:</b> The ensuredEpochs map is intentionally unbounded as epochs grow slowly
 * (~73 per year). Memory impact is negligible: ~4 years = ~300 epochs = ~2.4KB.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class PostgresPartitionManager implements PartitionManager {
    private static final String REWARD_TABLE = "reward";
    private static final String DEFAULT_PARTITION = "reward_default";
    private static final String PARTITION_PREFIX = "reward_p";

    private final DSLContext dsl;

    // Track which partitions have already been created to avoid redundant checks
    // Map size is bounded by number of epochs (grows ~73 per year)
    private final ConcurrentMap<Integer, Boolean> ensuredEpochs = new ConcurrentHashMap<>();

    @Override
    public void ensureRewardPartition(int spendableEpoch) {
        if (spendableEpoch < 0) {
            log.debug("Skipping partition creation for negative epoch: {}", spendableEpoch);
            return;
        }

        if (dsl.dialect().family() != SQLDialect.POSTGRES) {
            log.debug("Partitioning is only supported for PostgreSQL, current dialect: {}", dsl.dialect().family());
            return;
        }

        ensuredEpochs.computeIfAbsent(spendableEpoch, epoch -> {
            createPartition(epoch);
            return Boolean.TRUE;
        });
    }

    private void createPartition(int spendableEpoch) {
        dsl.transaction(configuration -> {
            DSLContext txDsl = DSL.using(configuration);
            try {
                String schema = getSchemaName(txDsl);
                String partitionName = PARTITION_PREFIX + spendableEpoch;

                // Check if partition already exists
                if (partitionExists(txDsl, schema, partitionName)) {
                    log.debug("Partition {} already exists, skipping creation", partitionName);
                    return;
                }

                // Build fully qualified table names
                String parentTable = schema + "." + REWARD_TABLE;
                String defaultPartition = schema + "." + DEFAULT_PARTITION;
                String partitionTable = schema + "." + partitionName;

                // Check if default partition has rows for this epoch
                boolean needsMigration = hasRowsInDefault(txDsl, defaultPartition, spendableEpoch);

                if (needsMigration) {
                    log.info("Migrating {} rows from default partition to {}", spendableEpoch, partitionName);
                    // Create standalone table, move data, then attach
                    createStandalonePartitionTable(txDsl, parentTable, partitionTable);
                    moveRowsFromDefault(txDsl, defaultPartition, partitionTable, spendableEpoch);
                    attachPartition(txDsl, parentTable, partitionTable, spendableEpoch);
                } else {
                    // No existing data, create partition directly
                    createPartitionDirectly(txDsl, parentTable, partitionTable, spendableEpoch);
                }

                log.info("Successfully created partition {} for spendable_epoch {}", partitionName, spendableEpoch);
            } catch (Exception e) {
                log.error("Failed to create partition for spendable_epoch {}: {}", spendableEpoch, e.getMessage(), e);
                throw new RuntimeException("Partition creation failed for epoch " + spendableEpoch, e);
            }
        });
    }

    /**
     * Get the schema name for table creation.
     * Tries to detect from current schema, falls back to 'public'.
     */
    private String getSchemaName(DSLContext ctx) {
        try {
            var result = ctx.fetchOne("SELECT current_schema()");
            if (result != null) {
                String schema = result.get(0, String.class);
                if (schema != null && !schema.isBlank()) {
                    return schema;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to detect current schema, using 'public': {}", e.getMessage());
        }
        return "public";
    }

    /**
     * Check if a partition already exists in the database.
     */
    private boolean partitionExists(DSLContext ctx, String schema, String partitionName) {
        var result = ctx.fetchOne(
                "SELECT 1 FROM pg_class c " +
                "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                "WHERE n.nspname = ? AND c.relname = ?",
                schema, partitionName
        );
        return result != null;
    }

    /**
     * Check if the default partition contains rows for the given spendable_epoch.
     */
    private boolean hasRowsInDefault(DSLContext ctx, String defaultPartition, int spendableEpoch) {
        var result = ctx.fetchOne(
                "SELECT 1 FROM " + defaultPartition + " " +
                "WHERE spendable_epoch = ? LIMIT 1",
                spendableEpoch
        );
        return result != null;
    }

    /**
     * Create partition directly (when no migration needed).
     */
    private void createPartitionDirectly(DSLContext ctx, String parentTable, String partitionTable, int spendableEpoch) {
        ctx.execute(String.format(
                "CREATE TABLE %s PARTITION OF %s FOR VALUES FROM (%d) TO (%d)",
                partitionTable, parentTable, spendableEpoch, spendableEpoch + 1
        ));
        log.debug("Created partition {} for range [{}, {})", partitionTable, spendableEpoch, spendableEpoch + 1);
    }

    /**
     * Create standalone partition table (for migration scenario).
     */
    private void createStandalonePartitionTable(DSLContext ctx, String parentTable, String partitionTable) {
        ctx.execute(String.format(
                "CREATE TABLE IF NOT EXISTS %s (LIKE %s INCLUDING DEFAULTS INCLUDING CONSTRAINTS)",
                partitionTable, parentTable
        ));
        // Ensure the table is empty before migration
        ctx.execute("TRUNCATE " + partitionTable);
    }

    /**
     * Move rows from default partition to the new partition table.
     */
    private void moveRowsFromDefault(DSLContext ctx, String defaultPartition, String partitionTable, int spendableEpoch) {
        int moved = ctx.execute(
                "INSERT INTO " + partitionTable + " " +
                "SELECT * FROM " + defaultPartition + " " +
                "WHERE spendable_epoch = ?",
                spendableEpoch
        );

        int deleted = ctx.execute(
                "DELETE FROM " + defaultPartition + " WHERE spendable_epoch = ?",
                spendableEpoch
        );

        log.debug("Migrated {} rows from default partition (deleted {} rows)", moved, deleted);
    }

    /**
     * Attach standalone partition table to parent.
     */
    private void attachPartition(DSLContext ctx, String parentTable, String partitionTable, int spendableEpoch) {
        ctx.execute(String.format(
                "ALTER TABLE %s ATTACH PARTITION %s FOR VALUES FROM (%d) TO (%d)",
                parentTable, partitionTable, spendableEpoch, spendableEpoch + 1
        ));
        log.debug("Attached partition {} to {}", partitionTable, parentTable);
    }
}
