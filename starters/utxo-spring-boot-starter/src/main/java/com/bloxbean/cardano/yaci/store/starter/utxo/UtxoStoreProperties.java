package com.bloxbean.cardano.yaci.store.starter.utxo;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class UtxoStoreProperties {
    private Utxo utxo;

    @Getter
    @Setter
    public static final class Utxo {
       private boolean enabled = true;
       private boolean apiEnabled = true;
       private Endpoints endpoints = new Endpoints();
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint address = new Endpoint();
        private Endpoint asset = new Endpoint();
        private Endpoint transaction = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }

}
