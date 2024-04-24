package com.bloxbean.cardano.yaci.store.starter.transaction;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class TransactionAutoConfigProperties {
    private Transaction transaction;

    @Getter
    @Setter
    public static final class Transaction {
        private boolean enabled = true;
        private boolean apiEnabled = true;
        private Endpoints endpoints = new Endpoints();
        /**
         * Enable pruning of Transaction
         */
        private boolean pruningEnabled = false;
        /**
         * Transaction Pruning interval in seconds
         */
        private int pruningInterval = 86400;
        /**
         * safe slot count to keep before pruning the Transaction
         */
        private int pruningSafeSlot = 43200; // 2160 blocks
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint block = new Endpoint();
        private Endpoint transaction = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }

}
