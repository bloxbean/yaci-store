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
 */
@Slf4j
@RequiredArgsConstructor
public class PostgresPartitionManager implements PartitionManager {
    private static final String REWARD_TABLE = "reward";
    private static final String REWARD_DEFAULT_PARTITION = "reward_default";
    private static final String REWARD_PARTITION_PREFIX = "reward_p";

    private static final String EPOCH_STAKE_TABLE = "epoch_stake";
    private static final String EPOCH_STAKE_DEFAULT_PARTITION = "epoch_stake_default";
    private static final String EPOCH_STAKE_PARTITION_PREFIX = "epoch_stake_p";

    private static final String DREP_DIST_TABLE = "drep_dist";
    private static final String DREP_DIST_DEFAULT_PARTITION = "drep_dist_default";
    private static final String DREP_DIST_PARTITION_PREFIX = "drep_dist_p";

    private final DSLContext dsl;

    // Track which partitions have already been created to avoid redundant checks
    // Map size is bounded by number of epochs (grows ~73 per year)
    private final ConcurrentMap<Integer, Boolean> ensuredRewardEpochs = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Boolean> ensuredEpochStakeEpochs = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Boolean> ensuredDRepDistEpochs = new ConcurrentHashMap<>();

    @Override
    public void ensureRewardPartition(int spendableEpoch) {
        if (spendableEpoch < 0) {
            log.debug("Skipping reward partition creation for negative epoch: {}", spendableEpoch);
            return;
        }

        ensuredRewardEpochs.computeIfAbsent(spendableEpoch, epoch -> {
            createRewardPartition(epoch);
            return Boolean.TRUE;
        });
    }

    @Override
    public void ensureEpochStakePartition(int epoch) {
        if (epoch < 0) {
            log.debug("Skipping epoch_stake partition creation for negative epoch: {}", epoch);
            return;
        }

        ensuredEpochStakeEpochs.computeIfAbsent(epoch, e -> {
            createEpochStakePartition(e);
            return Boolean.TRUE;
        });
    }

    @Override
    public void ensureDRepDistPartition(int epoch) {
        if (epoch < 0) {
            log.debug("Skipping drep_dist partition creation for negative epoch: {}", epoch);
            return;
        }

        ensuredDRepDistEpochs.computeIfAbsent(epoch, e -> {
            createDRepDistPartition(e);
            return Boolean.TRUE;
        });
    }

    private void createRewardPartition(int spendableEpoch) {
        dsl.transaction(configuration -> {
            DSLContext txDsl = DSL.using(configuration);
            try {
                String schema = getSchemaName(txDsl);
                String partitionName = REWARD_PARTITION_PREFIX + spendableEpoch;

                // Check if partition already exists
                if (partitionExists(txDsl, schema, partitionName)) {
                    log.debug("Reward partition {} already exists, skipping creation", partitionName);
                    return;
                }

                // Build fully qualified table names
                String parentTable = schema + "." + REWARD_TABLE;
                String defaultPartition = schema + "." + REWARD_DEFAULT_PARTITION;
                String partitionTable = schema + "." + partitionName;

                // Check if default partition has rows for this epoch
                boolean needsMigration = hasRowsInRewardDefault(txDsl, defaultPartition, spendableEpoch);

                if (needsMigration) {
                    log.info("Migrating reward rows from default partition to {}", partitionName);
                    // Create standalone table, move data, then attach
                    createStandalonePartitionTable(txDsl, parentTable, partitionTable);
                    moveRewardRowsFromDefault(txDsl, defaultPartition, partitionTable, spendableEpoch);
                    attachRewardPartition(txDsl, parentTable, partitionTable, spendableEpoch);
                } else {
                    // No existing data, create partition directly
                    createRewardPartitionDirectly(txDsl, parentTable, partitionTable, spendableEpoch);
                }

                log.info("Successfully created reward partition {} for spendable_epoch {}", partitionName, spendableEpoch);
            } catch (Exception e) {
                log.error("Failed to create reward partition for spendable_epoch {}: {}", spendableEpoch, e.getMessage(), e);
                throw new RuntimeException("Reward partition creation failed for epoch " + spendableEpoch, e);
            }
        });
    }

    private void createEpochStakePartition(int epoch) {
        try {
            String schema = getSchemaName(dsl);
            String partitionName = EPOCH_STAKE_PARTITION_PREFIX + epoch;

            // Check if partition already exists
            if (partitionExists(dsl, schema, partitionName)) {
                log.debug("Epoch stake partition {} already exists, skipping creation", partitionName);
                return;
            }

            // Build fully qualified table names
            String parentTable = schema + "." + EPOCH_STAKE_TABLE;
            String defaultPartition = schema + "." + EPOCH_STAKE_DEFAULT_PARTITION;
            String partitionTable = schema + "." + partitionName;

            // Check if default partition has rows for this epoch
            boolean needsMigration = hasRowsInEpochStakeDefault(dsl, defaultPartition, epoch);

            if (needsMigration) {
                log.info("Migrating epoch_stake rows from default partition to {}", partitionName);
                // Create standalone table, move data, then attach
                createStandalonePartitionTable(dsl, parentTable, partitionTable);
                moveEpochStakeRowsFromDefault(dsl, defaultPartition, partitionTable, epoch);
                attachEpochStakePartition(dsl, parentTable, partitionTable, epoch);
            } else {
                // No existing data, create partition directly
                createEpochStakePartitionDirectly(dsl, parentTable, partitionTable, epoch);
            }

            log.info("Successfully created epoch_stake partition {} for epoch {}", partitionName, epoch);
        } catch (Exception e) {
            log.error("Failed to create epoch_stake partition for epoch {}: {}", epoch, e.getMessage(), e);
            throw new RuntimeException("Epoch stake partition creation failed for epoch " + epoch, e);
        }
    }

    private void createDRepDistPartition(int epoch) {
        try {
            String schema = getSchemaName(dsl);
            String partitionName = DREP_DIST_PARTITION_PREFIX + epoch;

            // Check if partition already exists
            if (partitionExists(dsl, schema, partitionName)) {
                log.debug("DRep dist partition {} already exists, skipping creation", partitionName);
                return;
            }

            // Build fully qualified table names
            String parentTable = schema + "." + DREP_DIST_TABLE;
            String defaultPartition = schema + "." + DREP_DIST_DEFAULT_PARTITION;
            String partitionTable = schema + "." + partitionName;

            // Check if default partition has rows for this epoch
            boolean needsMigration = hasRowsInDRepDistDefault(dsl, defaultPartition, epoch);

            if (needsMigration) {
                log.info("Migrating drep_dist rows from default partition to {}", partitionName);
                // Create standalone table, move data, then attach
                createStandalonePartitionTable(dsl, parentTable, partitionTable);
                moveDRepDistRowsFromDefault(dsl, defaultPartition, partitionTable, epoch);
                attachDRepDistPartition(dsl, parentTable, partitionTable, epoch);
            } else {
                // No existing data, create partition directly
                createDRepDistPartitionDirectly(dsl, parentTable, partitionTable, epoch);
            }

            log.info("Successfully created drep_dist partition {} for epoch {}", partitionName, epoch);
        } catch (Exception e) {
            log.error("Failed to create drep_dist partition for epoch {}: {}", epoch, e.getMessage(), e);
            throw new RuntimeException("DRep dist partition creation failed for epoch " + epoch, e);
        }
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

    // ========== Reward Partition Helper Methods ==========

    /**
     * Check if the reward default partition contains rows for the given spendable_epoch.
     */
    private boolean hasRowsInRewardDefault(DSLContext ctx, String defaultPartition, int spendableEpoch) {
        var result = ctx.fetchOne(
                "SELECT 1 FROM " + defaultPartition + " " +
                "WHERE spendable_epoch = ? LIMIT 1",
                spendableEpoch
        );
        return result != null;
    }

    /**
     * Create reward partition directly (when no migration needed).
     */
    private void createRewardPartitionDirectly(DSLContext ctx, String parentTable, String partitionTable, int spendableEpoch) {
        ctx.execute(String.format(
                "CREATE TABLE %s PARTITION OF %s FOR VALUES FROM (%d) TO (%d)",
                partitionTable, parentTable, spendableEpoch, spendableEpoch + 1
        ));
        log.debug("Created reward partition {} for range [{}, {})", partitionTable, spendableEpoch, spendableEpoch + 1);
    }

    /**
     * Move reward rows from default partition to the new partition table.
     */
    private void moveRewardRowsFromDefault(DSLContext ctx, String defaultPartition, String partitionTable, int spendableEpoch) {
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

        log.debug("Migrated {} reward rows from default partition (deleted {} rows)", moved, deleted);
    }

    /**
     * Attach standalone reward partition table to parent.
     */
    private void attachRewardPartition(DSLContext ctx, String parentTable, String partitionTable, int spendableEpoch) {
        ctx.execute(String.format(
                "ALTER TABLE %s ATTACH PARTITION %s FOR VALUES FROM (%d) TO (%d)",
                parentTable, partitionTable, spendableEpoch, spendableEpoch + 1
        ));
        log.debug("Attached reward partition {} to {}", partitionTable, parentTable);
    }

    // ========== Epoch Stake Partition Helper Methods ==========

    /**
     * Check if the epoch_stake default partition contains rows for the given epoch.
     */
    private boolean hasRowsInEpochStakeDefault(DSLContext ctx, String defaultPartition, int epoch) {
        var result = ctx.fetchOne(
                "SELECT 1 FROM " + defaultPartition + " " +
                "WHERE epoch = ? LIMIT 1",
                epoch
        );
        return result != null;
    }

    /**
     * Create epoch_stake partition directly (when no migration needed).
     */
    private void createEpochStakePartitionDirectly(DSLContext ctx, String parentTable, String partitionTable, int epoch) {
        ctx.execute(String.format(
                "CREATE TABLE %s PARTITION OF %s FOR VALUES FROM (%d) TO (%d)",
                partitionTable, parentTable, epoch, epoch + 1
        ));
        log.debug("Created epoch_stake partition {} for range [{}, {})", partitionTable, epoch, epoch + 1);
    }

    /**
     * Move epoch_stake rows from default partition to the new partition table.
     */
    private void moveEpochStakeRowsFromDefault(DSLContext ctx, String defaultPartition, String partitionTable, int epoch) {
        int moved = ctx.execute(
                "INSERT INTO " + partitionTable + " " +
                "SELECT * FROM " + defaultPartition + " " +
                "WHERE epoch = ?",
                epoch
        );

        int deleted = ctx.execute(
                "DELETE FROM " + defaultPartition + " WHERE epoch = ?",
                epoch
        );

        log.debug("Migrated {} epoch_stake rows from default partition (deleted {} rows)", moved, deleted);
    }

    /**
     * Attach standalone epoch_stake partition table to parent.
     */
    private void attachEpochStakePartition(DSLContext ctx, String parentTable, String partitionTable, int epoch) {
        ctx.execute(String.format(
                "ALTER TABLE %s ATTACH PARTITION %s FOR VALUES FROM (%d) TO (%d)",
                parentTable, partitionTable, epoch, epoch + 1
        ));
        log.debug("Attached epoch_stake partition {} to {}", partitionTable, parentTable);
    }

    // ========== DRep Dist Partition Helper Methods ==========

    /**
     * Check if the drep_dist default partition contains rows for the given epoch.
     */
    private boolean hasRowsInDRepDistDefault(DSLContext ctx, String defaultPartition, int epoch) {
        var result = ctx.fetchOne(
                "SELECT 1 FROM " + defaultPartition + " " +
                "WHERE epoch = ? LIMIT 1",
                epoch
        );
        return result != null;
    }

    /**
     * Create drep_dist partition directly (when no migration needed).
     */
    private void createDRepDistPartitionDirectly(DSLContext ctx, String parentTable, String partitionTable, int epoch) {
        ctx.execute(String.format(
                "CREATE TABLE %s PARTITION OF %s FOR VALUES FROM (%d) TO (%d)",
                partitionTable, parentTable, epoch, epoch + 1
        ));
        log.debug("Created drep_dist partition {} for range [{}, {})", partitionTable, epoch, epoch + 1);
    }

    /**
     * Move drep_dist rows from default partition to the new partition table.
     */
    private void moveDRepDistRowsFromDefault(DSLContext ctx, String defaultPartition, String partitionTable, int epoch) {
        int moved = ctx.execute(
                "INSERT INTO " + partitionTable + " " +
                "SELECT * FROM " + defaultPartition + " " +
                "WHERE epoch = ?",
                epoch
        );

        int deleted = ctx.execute(
                "DELETE FROM " + defaultPartition + " WHERE epoch = ?",
                epoch
        );

        log.debug("Migrated {} drep_dist rows from default partition (deleted {} rows)", moved, deleted);
    }

    /**
     * Attach standalone drep_dist partition table to parent.
     */
    private void attachDRepDistPartition(DSLContext ctx, String parentTable, String partitionTable, int epoch) {
        ctx.execute(String.format(
                "ALTER TABLE %s ATTACH PARTITION %s FOR VALUES FROM (%d) TO (%d)",
                parentTable, partitionTable, epoch, epoch + 1
        ));
        log.debug("Attached drep_dist partition {} to {}", partitionTable, parentTable);
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
}
