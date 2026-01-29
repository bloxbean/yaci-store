package com.bloxbean.cardano.yaci.store.starter.live;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class LiveStoreProperties {
    private Live live = new Live();

    @Getter
    @Setter
    public static final class Live  {
       private boolean enabled = false;
       private boolean apiEnabled = true;
    }

}
