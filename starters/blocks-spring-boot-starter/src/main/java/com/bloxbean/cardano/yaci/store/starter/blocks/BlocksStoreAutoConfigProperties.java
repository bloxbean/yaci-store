package com.bloxbean.cardano.yaci.store.starter.blocks;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class BlocksStoreAutoConfigProperties {
    private Blocks blocks = new Blocks();

    @Getter
    @Setter
    public static final class Blocks  {
       private boolean enabled = true;

       private boolean apiEnabled = true;

       private Endpoints endpoints = new Endpoints();
       private Metrics metrics = new Metrics();
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint block = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static final class Metrics {
        private boolean enabled = true;
        private long updateInterval = 60000; // 60 seconds
    }
}
