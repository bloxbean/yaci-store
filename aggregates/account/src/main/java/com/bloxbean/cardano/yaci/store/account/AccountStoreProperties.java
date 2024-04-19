package com.bloxbean.cardano.yaci.store.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountStoreProperties {
    private boolean historyCleanupEnabled;
    private boolean balanceAggregationEnabled;

    @Builder.Default
    private int maxBalanceRecordsPerAddressPerBatch = 3;
    @Builder.Default
    private boolean stakeAddressBalanceEnabled = true;

    @Builder.Default
    private int balanceHistoryCleanupInterval = 300;
    @Builder.Default
    private long balanceCleanupSlotCount = 43200; //2160 blocks
    @Builder.Default
    private long balanceCleanupBatchThreshold = 20000;

    @Builder.Default
    private boolean saveAddressTxAmount = false;
    @Builder.Default
    private boolean addressTxAmountIncludeZeroAmount = false;
    @Builder.Default
    private boolean addressTxAmountExcludeTokenZeroAmount = true;
    private long initialBalanceSnapshotBlock;

    @Builder.Default
    private int balanceCalcJobBatchSize = 1000;

    @Builder.Default
    private int balanceCalcJobPartitionSize = 10;
}
