package com.bloxbean.cardano.yaci.store.starter.assets;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store")
public class AssetsStoreProperties {
    private Assets assets = new Assets();

    @Getter
    @Setter
    public static final class Assets  {
       private boolean enabled = true;
       private boolean apiEnabled = true;
       private Endpoints endpoints = new Endpoints();
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint asset = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }

}
