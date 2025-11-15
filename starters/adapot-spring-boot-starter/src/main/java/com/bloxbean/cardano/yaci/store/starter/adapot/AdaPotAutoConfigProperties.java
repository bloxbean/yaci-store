package com.bloxbean.cardano.yaci.store.starter.adapot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class AdaPotAutoConfigProperties {

    private Adapot adaPot = new Adapot();

    @Getter
    @Setter
    public static final class Adapot {
        private boolean enabled = false;
        private boolean apiEnabled = true;

        //Batch size for updating rewards in the db
        private int updateRewardDbBatchSize = 200;
        private boolean bulkUpdateReward = true;
        private boolean bulkUpdateRewardWithCopy = true;

        // PostgreSQL Bulk Load Memory Configuration for Reward Operations
        // Default is null to use PostgreSQL defaults
        private String rewardBulkLoadWorkMem;
        private String rewardBulkLoadMaintenanceWorkMem;

        // PostgreSQL Memory Configuration for Stake Snapshot Operations
        // Default is null to use PostgreSQL defaults
        private String stakeSnapshotWorkMem;

        //Verify adapot calculation values with known values (db sync)
        private boolean verifyAdapotCalcValues = true;

        // Epoch Stake Pruning Configuration
        private boolean epochStakePruningEnabled = false;
        private int epochStakePruningInterval = 86400;
        private int epochStakeSafeEpochs = 8;
        private int epochStakePruningBatchSize = 3000;

        // Reward Pruning Configuration
        private boolean rewardPruningEnabled = false;
        private int rewardPruningInterval = 86400;
        private int rewardPruningSafeSlots = 43200; // 2160 blocks
        private int rewardPruningBatchSize = 3000;

        private Metrics metrics = new Metrics();
    }

    @Getter
    @Setter
    public static final class Metrics {
        private boolean enabled = true;
        private long updateInterval = 120000; // 120 seconds
    }

}
