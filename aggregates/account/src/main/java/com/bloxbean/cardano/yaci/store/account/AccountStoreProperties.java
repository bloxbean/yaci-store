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
    private boolean parallelWrite = false;
    @Builder.Default
    private int writeThreadDefaultBatchSize = 1000;
    @Builder.Default
    private int jooqWriteBatchSize = 3000;
    @Builder.Default
    private int writeThreadCount = 5;

    @Builder.Default
    private int balanceHistoryCleanupInterval = 300;
    @Builder.Default
    private long balanceCleanupSlotCount = 43200; //2160 blocks

    @Builder.Default
    private int addressCacheSize = 2_000_000;

    @Builder.Default
    private int addressCacheExpiryAfterAccess = 15;
}
