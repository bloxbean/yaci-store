package com.bloxbean.cardano.yaci.store.starter.assets;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class AssetsStoreProperties {
    private Assets assets;

    @Getter
    @Setter
    public static final class Assets  {
       private boolean enabled = true;
    }

    @Getter
    @Setter
    public static final class AssetsApi {
        private boolean enabled = true;
    }

}
