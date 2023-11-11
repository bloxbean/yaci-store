package com.bloxbean.cardano.yaci.store.starter.remote;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class RemoteStoreAutoConfProperties {
    private Remote remote;

    @Getter
    @Setter
    public static final class Remote {
       private boolean publisherEnabled = false;
       private boolean consumerEnabled = false;
       private List<String> publisherEvents = List.of("blockEvent", "rollbackEvent");
    }

}
