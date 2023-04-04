package com.bloxbean.cardano.yaci.store.starter.transaction;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class TransactionStoreProperties {
    private Transaction transaction;

    @Getter
    @Setter
    public static final class Transaction {
       private boolean enabled = true;
    }

}
