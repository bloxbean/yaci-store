package com.bloxbean.cardano.yaci.store.starter.account;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class AccountStoreAutoConfigProperties {
    private Account account;

    @Getter
    @Setter
    public static final class Account {
        private boolean enabled = false;
        private boolean balanceAggregationEnabled = false;
        private boolean historyCleanupEnabled = false;
        private boolean batchBalanceAggregationEnabled = false;
        private boolean batchBalanceAggregationSchedulerEnabled=false;
        private int batchBalanceAggregationScheduleDelay = 5; //5 sec
        private int batchBalanceAggregationBatchSize = 4320; //~ 1 day (4320 blocks)
        private int batchBalanceAggregationSafeBlockDiff = 500;
    }

}
