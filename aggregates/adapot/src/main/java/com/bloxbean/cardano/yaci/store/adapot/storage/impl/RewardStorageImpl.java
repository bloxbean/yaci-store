package com.bloxbean.cardano.yaci.store.adapot.storage.impl;

import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.adapot.domain.UnclaimedRewardRest;
import com.bloxbean.cardano.yaci.store.adapot.storage.RewardStorage;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.mapper.Mapper;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.InstantRewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.RewardRestRepository;
import com.bloxbean.cardano.yaci.store.adapot.storage.impl.repository.UnclaimedRewardRestRepository;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jooq.DSLContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static com.bloxbean.cardano.yaci.store.adapot.jooq.Tables.REWARD;

@RequiredArgsConstructor
@Slf4j
public class RewardStorageImpl implements RewardStorage {
    private final InstantRewardRepository instantRewardRepository;
    private final RewardRestRepository rewardRestRepository;
    private final RewardRepository rewardRepository;
    private final UnclaimedRewardRestRepository unclaimedRewardRestRepository;
    private final Mapper mapper;
    private final DSLContext dsl;

    @Override
    public void saveInstantRewards(List<InstantReward> rewards) {
        instantRewardRepository.saveAll(rewards.stream().map(mapper::toInstantRewardEntity).toList());
    }

    @Override
    public void saveRewardRest(List<RewardRest> rewards) {
        rewardRestRepository.saveAll(rewards.stream().map(mapper::toRewardRestEntity).toList());
    }

    @Override
    public void saveRewards(List<Reward> rewards) {
        var inserts = rewards.stream()
                .map(reward -> dsl.insertInto(REWARD)
                        .set(REWARD.ADDRESS, reward.getAddress())
                        .set(REWARD.EARNED_EPOCH, reward.getEarnedEpoch())
                        .set(REWARD.TYPE, reward.getType().toString())
                        .set(REWARD.POOL_ID, reward.getPoolId())
                        .set(REWARD.AMOUNT, reward.getAmount())
                        .set(REWARD.SPENDABLE_EPOCH, reward.getSpendableEpoch())
                        .set(REWARD.SLOT, reward.getSlot())
                        .onDuplicateKeyUpdate()
                        .set(REWARD.ADDRESS, reward.getAddress())
                        .set(REWARD.EARNED_EPOCH, reward.getEarnedEpoch())
                        .set(REWARD.TYPE, reward.getType().toString())
                        .set(REWARD.POOL_ID, reward.getPoolId())
                        .set(REWARD.AMOUNT, reward.getAmount())
                        .set(REWARD.SPENDABLE_EPOCH, reward.getSpendableEpoch())
                        .set(REWARD.SLOT, reward.getSlot())).toList();

        dsl.batch(inserts).execute();
    }

    @Override
    public void bulkSaveRewards(List<Reward> rewards, int batchSize) {
        var currentTime = LocalDateTime.now();
        var rewardRecords = rewards.stream()
                .map(reward -> {
                    var rewardRecord = dsl.newRecord(REWARD);
                    rewardRecord.setAddress(reward.getAddress());
                    rewardRecord.setEarnedEpoch(reward.getEarnedEpoch());
                    rewardRecord.setType(reward.getType().toString());
                    rewardRecord.setPoolId(reward.getPoolId());
                    rewardRecord.setAmount(reward.getAmount());
                    rewardRecord.setSpendableEpoch(reward.getSpendableEpoch());
                    rewardRecord.setSlot(reward.getSlot());
                    rewardRecord.setUpdateDatetime(currentTime);
                    return rewardRecord;
                });

        try {
            dsl.loadInto(REWARD)
                    //.bulkAfter(batchSize)
                    //.batchAfter(batchSize)
                    .commitAfter(batchSize)
                    .loadRecords(rewardRecords)
                    .fields(REWARD.ADDRESS, REWARD.EARNED_EPOCH, REWARD.TYPE, REWARD.POOL_ID, REWARD.AMOUNT, REWARD.SPENDABLE_EPOCH, REWARD.SLOT)
                    .execute();
        } catch (IOException e) {
            throw new RuntimeException("Reward data could not be loaded", e);
        }
    }

    public void bulkSaveRewardsWithCopy(List<Reward> rewards, int spendableEpoch) {
        long startTotal = System.currentTimeMillis();

        // Increase work_mem for this bulk operation to prevent disk-based temp files
        // This setting only affects the current transaction and automatically resets after
        try {
            dsl.execute("SET LOCAL work_mem = '1GB'");
            dsl.execute("SET LOCAL maintenance_work_mem = '2GB'");
            log.debug("Increased work_mem to 1GB and maintenance_work_mem to 2GB for bulk COPY operation");
        } catch (Exception e) {
            log.warn("Failed to set work_mem: {}. Continuing with default settings.", e.getMessage());
        }

        String partitionName = "reward_p" + spendableEpoch;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        LocalDateTime now = LocalDateTime.now();

        // TIMING: Detach partition
        long startDetach = System.currentTimeMillis();
        boolean wasDetached = detachPartition(partitionName, spendableEpoch);
        long detachTime = System.currentTimeMillis() - startDetach;

        // TIMING: Drop indexes (only works on detached table)
        long startDrop = System.currentTimeMillis();
        if (wasDetached) {
            dropPartitionIndexes(partitionName);
        }
        long dropTime = System.currentTimeMillis() - startDrop;

        // TIMING: CSV Generation
        long startCsv = System.currentTimeMillis();
        try (CSVPrinter csv = new CSVPrinter(
                new OutputStreamWriter(baos, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT
                        .withHeader(
                                "address",
                                "earned_epoch",
                                "type",
                                "pool_id",
                                "amount",
                                "spendable_epoch",
                                "slot",
                                "update_datetime"
                        )
                        .withTrim()
        )) {
            for (Reward r : rewards) {
                csv.printRecord(
                        r.getAddress(),
                        r.getEarnedEpoch(),
                        r.getType().toString(),
                        r.getPoolId(),
                        r.getAmount(),
                        r.getSpendableEpoch(),
                        r.getSlot(),
                        now
                );
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write CSV for rewards", e);
        }
        long csvTime = System.currentTimeMillis() - startCsv;
        long csvSizeBytes = baos.size();

        log.info("Bulk COPY performance - Rows: {}, CSV generation: {}ms (size: {} MB)",
                rewards.size(),
                csvTime,
                String.format("%.2f", csvSizeBytes / 1024.0 / 1024.0));

        // TIMING: COPY Execution (to detached partition or parent table)
        long startCopy = System.currentTimeMillis();
        try (var in = new ByteArrayInputStream(baos.toByteArray())) {
            // Copy to detached partition table directly if detached, otherwise to parent
            if (wasDetached) {
                // JOOQ handles COPY protocol properly
                executeCopyToTable(in, partitionName);
            } else {
                // Fallback: COPY to parent table (will route to correct partition)
                dsl.loadInto(REWARD)
                        .loadCSV(in)
                        .fields(
                                REWARD.ADDRESS,
                                REWARD.EARNED_EPOCH,
                                REWARD.TYPE,
                                REWARD.POOL_ID,
                                REWARD.AMOUNT,
                                REWARD.SPENDABLE_EPOCH,
                                REWARD.SLOT,
                                REWARD.UPDATE_DATETIME
                        )
                        .execute();
            }
        } catch (IOException e) {
            throw new RuntimeException("Reward data could not be loaded via COPY", e);
        }
        long copyTime = System.currentTimeMillis() - startCopy;

        // TIMING: Recreate indexes (only on detached table)
        long startRecreate = System.currentTimeMillis();
        if (wasDetached) {
            recreatePartitionIndexes(partitionName);
        }
        long recreateTime = System.currentTimeMillis() - startRecreate;

        // TIMING: Reattach partition
        long startAttach = System.currentTimeMillis();
        if (wasDetached) {
            attachPartition(partitionName, spendableEpoch);
        }
        long attachTime = System.currentTimeMillis() - startAttach;

        long totalTime = System.currentTimeMillis() - startTotal;

        log.info("Bulk COPY performance summary - Rows: {}, Total: {}ms", rewards.size(), totalTime);
        log.info("  └─ Detach partition: {}ms", detachTime);
        log.info("  └─ Drop indexes (including PK): {}ms", dropTime);
        log.info("  └─ CSV generation: {}ms (size: {} MB)", csvTime, String.format("%.2f", csvSizeBytes / 1024.0 / 1024.0));
        log.info("  └─ COPY execution: {}ms ({} rows/sec)",
                 copyTime,
                 copyTime > 0 ? String.format("%.0f", rewards.size() * 1000.0 / copyTime) : "N/A");
        log.info("  └─ Recreate indexes (including PK): {}ms", recreateTime);
        log.info("  └─ Attach partition: {}ms", attachTime);
        log.info("Performance improvement: COPY without PK maintenance vs with PK (expected ~10-15s vs ~140s)");
    }

    /**
     * Execute COPY FROM STDIN to a specific table using JOOQ.
     */
    private void executeCopyToTable(ByteArrayInputStream in, String tableName) throws IOException {
        // Use JOOQ's loader but target specific table
        var tableRef = dsl.meta().getTables(tableName).get(0);
        dsl.loadInto(tableRef)
                .loadCSV(in)
                .fields(
                        tableRef.field("address"),
                        tableRef.field("earned_epoch"),
                        tableRef.field("type"),
                        tableRef.field("pool_id"),
                        tableRef.field("amount"),
                        tableRef.field("spendable_epoch"),
                        tableRef.field("slot"),
                        tableRef.field("update_datetime")
                )
                .execute();
    }

    /**
     * Drop all indexes (including primary key) on a partition before bulk insert.
     * This dramatically speeds up bulk COPY by eliminating index maintenance overhead.
     * Safe because partition is detached and isolated from queries.
     */
    private void dropPartitionIndexes(String partitionName) {
        try {
            // Drop secondary indexes
            dsl.execute("DROP INDEX IF EXISTS " + partitionName + "_earned_epoch_type_idx");
            dsl.execute("DROP INDEX IF EXISTS " + partitionName + "_slot_idx");
            dsl.execute("DROP INDEX IF EXISTS " + partitionName + "_spendable_epoch_idx");
            log.debug("Dropped secondary indexes on partition {}", partitionName);

            // Drop primary key constraint
            // Note: This is safe because the partition is detached and not accessible to queries
            // The PK will be recreated via bulk index build (much faster than maintaining it during insert)
            dsl.execute("ALTER TABLE " + partitionName + " DROP CONSTRAINT IF EXISTS " + partitionName + "_pkey");
            log.debug("Dropped primary key constraint on partition {}", partitionName);
        } catch (Exception e) {
            log.warn("Failed to drop indexes/constraints on partition {}: {}", partitionName, e.getMessage());
            // Continue anyway - indexes/constraints might not exist yet
        }
    }

    /**
     * Recreate all indexes (including primary key) on a partition after bulk insert.
     * Bulk index build is much faster than maintaining indexes during insert.
     */
    private void recreatePartitionIndexes(String partitionName) {
        try {
            // Recreate primary key constraint FIRST
            // Bulk PK creation on sorted data is ~10-20x faster than maintaining it during insert
            dsl.execute(String.format(
                    "ALTER TABLE %s ADD CONSTRAINT %s_pkey PRIMARY KEY (address, earned_epoch, type, pool_id, spendable_epoch)",
                    partitionName, partitionName));
            log.debug("Recreated primary key constraint on partition {}", partitionName);

            // Recreate secondary indexes
            dsl.execute(String.format(
                    "CREATE INDEX IF NOT EXISTS %s_earned_epoch_type_idx ON %s (earned_epoch, type)",
                    partitionName, partitionName));
            dsl.execute(String.format(
                    "CREATE INDEX IF NOT EXISTS %s_slot_idx ON %s (slot)",
                    partitionName, partitionName));
            dsl.execute(String.format(
                    "CREATE INDEX IF NOT EXISTS %s_spendable_epoch_idx ON %s (spendable_epoch)",
                    partitionName, partitionName));
            log.debug("Recreated secondary indexes on partition {}", partitionName);
        } catch (Exception e) {
            log.error("Failed to recreate indexes/constraints on partition {}: {}", partitionName, e.getMessage(), e);
            throw new RuntimeException("Index recreation failed for partition " + partitionName, e);
        }
    }

    /**
     * Detach a partition from the parent table.
     * Returns true if partition was detached, false if partition doesn't exist yet.
     */
    private boolean detachPartition(String partitionName, int spendableEpoch) {
        try {
            // Check if partition exists
            var result = dsl.fetchOne(
                    "SELECT 1 FROM pg_class c " +
                    "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                    "WHERE n.nspname = current_schema() AND c.relname = ?",
                    partitionName
            );

            if (result == null) {
                log.debug("Partition {} does not exist yet, will create fresh", partitionName);
                return false;
            }

            // Detach partition
            dsl.execute(String.format("ALTER TABLE reward DETACH PARTITION %s", partitionName));
            log.debug("Detached partition {} from reward table", partitionName);
            return true;
        } catch (Exception e) {
            log.warn("Failed to detach partition {}: {}. Will use fallback approach.",
                     partitionName, e.getMessage());
            return false;
        }
    }

    /**
     * Attach a partition back to the parent table.
     */
    private void attachPartition(String partitionName, int spendableEpoch) {
        try {
            dsl.execute(String.format(
                    "ALTER TABLE reward ATTACH PARTITION %s FOR VALUES FROM (%d) TO (%d)",
                    partitionName, spendableEpoch, spendableEpoch + 1
            ));
            log.debug("Attached partition {} to reward table for epoch {}", partitionName, spendableEpoch);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to attach partition {} back to reward table: {}",
                      partitionName, e.getMessage(), e);
            throw new RuntimeException("Failed to attach partition " + partitionName, e);
        }
    }

    @Override
    public void saveUnclaimedRewardRest(List<UnclaimedRewardRest> unclaimedRewards) {
        unclaimedRewardRestRepository.saveAll(unclaimedRewards.stream().map(mapper::toUnclaimedRewardRestEntity).toList());
    }

    @Override
    public List<UnclaimedRewardRest> findUnclaimedRewardRest(int spendableEpoch) {
        return unclaimedRewardRestRepository.findBySpendableEpoch(spendableEpoch)
                .stream().map(mapper::toUnclaimedRewardRest)
                .toList();
    }

    @Override
    public List<RewardRest> findTreasuryWithdrawals(int spendableEpoch) {
        return rewardRestRepository.findBySpendableEpochAndType(spendableEpoch, RewardRestType.treasury)
                .stream().map(mapper::toRewardRest)
                .toList();
    }

    @Override
    public int deleteLeaderMemberRewards(int earnedEpoch) {
        return rewardRepository.deleteLeaderMemberRewards(earnedEpoch);
    }

    @Override
    public int deleteRewardRest(int earnedEpoch, RewardRestType type) {
        return rewardRestRepository.deleteByEarnedEpochAndType(earnedEpoch, type);
    }

    @Override
    public int deleteUnclaimedRewardRest(int earnedEpoch, RewardRestType type) {
        return unclaimedRewardRestRepository.deleteByEarnedEpochAndType(earnedEpoch, type);
    }

    @Override
    public int deleteInstantRewardsBySlotGreaterThan(long slot) {
        return instantRewardRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteRewardsBySlotGreaterThan(long slot) {
        return rewardRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteRewardRestsBySlotGreaterThan(long slot) {
        return rewardRestRepository.deleteBySlotGreaterThan(slot);
    }

    @Override
    public int deleteUnclaimedRewardsBySlotGreaterThan(long slot) {
        return unclaimedRewardRestRepository.deleteBySlotGreaterThan(slot);
    }

}
