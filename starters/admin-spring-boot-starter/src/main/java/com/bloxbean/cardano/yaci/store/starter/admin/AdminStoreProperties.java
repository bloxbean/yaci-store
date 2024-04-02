package com.bloxbean.cardano.yaci.store.starter.admin;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class AdminStoreProperties {
    private Admin admin;

    @Getter
    @Setter
    public static final class Admin  {
       private boolean apiEnabled = false;
    }
}
