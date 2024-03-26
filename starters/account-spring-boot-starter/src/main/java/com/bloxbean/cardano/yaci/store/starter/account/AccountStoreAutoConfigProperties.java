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
        private boolean stakeAddressBalanceEnabled = true;

        //parallel write & batch size settings
        private boolean parallelWrite = false;
        private int writeThreadDefaultBatchSize = 1000;
        private int jooqWriteBatchSize = 3000;
        private int writeThreadCount = 5;

        private int balanceHistoryCleanupInterval = 300;
        private long balanceCleanupSlotCount = 43200; //2160 blocks

        private int addressCacheSize = 2_000_000;
        private int addressCacheExpiryAfterAccess = 15;
    }

}
