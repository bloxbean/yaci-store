package com.bloxbean.cardano.yaci.store.starter.admin;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class AdminStoreProperties {
    private Admin admin = new Admin();

    @Getter
    @Setter
    public static final class Admin {
        /**
         * Enable admin api
         */
        private boolean apiEnabled = false;
        /**
         * Enable auto recovery
         */
        private boolean autoRecoveryEnabled = false;
        /**
         * Health check interval in seconds for auto recovery
         */
        private long healthCheckInterval = 120;
    }
}
