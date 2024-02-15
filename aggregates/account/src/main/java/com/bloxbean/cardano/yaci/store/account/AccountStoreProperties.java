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
    private boolean addressBalanceEnabled = true;
}
