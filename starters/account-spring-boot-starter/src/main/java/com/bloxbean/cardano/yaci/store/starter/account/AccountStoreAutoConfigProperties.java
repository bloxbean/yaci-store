package com.bloxbean.cardano.yaci.store.starter.account;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class AccountStoreAutoConfigProperties {
    private Account account = new Account();

    @Getter
    @Setter
    public static final class Account {
        private boolean enabled = false;
        private boolean apiEnabled = false;

        private boolean balanceAggregationEnabled = false;
        private boolean historyCleanupEnabled = false;

        private int maxBalanceRecordsPerAddressPerBatch = 3;
        private boolean addressBalanceEnabled = true;
        private boolean stakeAddressBalanceEnabled = true;

        private int balanceHistoryCleanupInterval = 300;
        private long balanceCleanupSlotCount = 43200; //2160 blocks
        private long balanceCleanupBatchThreshold = 20000;

        private boolean saveAddressTxAmount = false;
        private boolean addressTxAmountIncludeZeroAmount = false;
        private boolean addressTxAmountExcludeZeroTokenAmount = true;

        private long initialBalanceSnapshotBlock;

        private int balanceCalcJobBatchSize = 1000;
        private int balanceCalcJobPartitionSize = 10;
        private String balanceCalcBatchMode = "tx-amount";

        /**
         * Enable Address Balance and Stake Address Balance Pruning (History records)
         */
        private boolean pruningEnabled = false;
        /**
         * Pruning batch size
         */
        private int pruningBatchSize = 3000;
        /**
         * Pruning interval in seconds
         */
        private int pruningInterval = 86400;
    }

}
