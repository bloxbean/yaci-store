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

        //Verify adapot calculation values with known values (db sync)
        private boolean verifyAdapotCalcValues = true;

        // Epoch Stake Pruning Configuration
        private boolean epochStakePruningEnabled = false;
        private int epochStakePruningInterval = 86400;
        private int epochStakeSafeEpochs = 8;
        private int epochStakePruningBatchSize = 3000;
    }

}
