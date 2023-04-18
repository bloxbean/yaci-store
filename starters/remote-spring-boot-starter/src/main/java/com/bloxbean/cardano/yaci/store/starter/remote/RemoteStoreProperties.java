package com.bloxbean.cardano.yaci.store.starter.remote;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class RemoteStoreProperties {
    private Remote remote;

    @Getter
    @Setter
    public static final class Remote {
       private boolean publisherEnabled = false;
       private boolean consumerEnabled = false;
    }

}
