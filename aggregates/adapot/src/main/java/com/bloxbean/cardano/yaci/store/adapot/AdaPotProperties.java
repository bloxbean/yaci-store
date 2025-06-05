package com.bloxbean.cardano.yaci.store.adapot;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdaPotProperties {

    private boolean enabled;

    @Builder.Default
    private int updateRewardDbBatchSize = 200;

    private boolean bulkUpdateReward = true;

    private boolean bulkUpdateRewardWithCopy = true;

    @Builder.Default
    private boolean verifyAdapotCalcValues = true;

    // Epoch Stake Pruning Configuration
    @Builder.Default
    private boolean epochStakePruningEnabled = false;

    @Builder.Default
    private int epochStakePruningInterval = 86400; // 24 hours in seconds

    @Builder.Default
    private int epochStakePruningSafeEpochs = 8; // Keep last 8 epochs

    @Builder.Default
    private int epochStakePruningBatchSize = 3000;
}
