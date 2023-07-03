package com.bloxbean.cardano.yaci.store.starter.account;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class AccountStoreProperties {
    private Account account;

    @Getter
    @Setter
    public static final class Account {
       private boolean enabled = false;
       private boolean historyCleanupEnabled = false;
    }

}
