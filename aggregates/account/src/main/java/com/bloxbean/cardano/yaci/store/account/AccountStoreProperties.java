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
    
    private boolean batchBalanceAggregationEnabled;
    private boolean batchBalanceAggregationSchedulerEnabled;
    private int batchBalanceAggregationScheduleDelay;
    private int batchBalanceAggregationBatchSize;
    private int batchBalanceAggregationSafeBlockDiff;
}
