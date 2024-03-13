package com.bloxbean.cardano.yaci.store.starter.metadata;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class MetadataStoreProperties {
    private Metadata metadata;

    @Getter
    @Setter
    public static final class Metadata  {
       private boolean enabled = true;
       private boolean apiEnabled = true;
       private Endpoints endpoints = new Endpoints();
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint transaction = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }
}
