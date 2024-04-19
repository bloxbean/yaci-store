package com.bloxbean.cardano.yaci.store.starter.utxo;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class UtxoStoreAutoConfigProperties {
    private Utxo utxo = new Utxo();

    @Getter
    @Setter
    public static final class Utxo {
       private boolean enabled = true;
       private boolean apiEnabled = true;
       private Endpoints endpoints = new Endpoints();

       private boolean saveAddress = false;
       private boolean addressCacheEnabled = false;
       private int addressCacheSize = 50000;
       private int addressCacheExpiryAfterAccess = 15;

        /**
         * Enable pruning of UTXO
         */
        private boolean pruningEnabled = false;
        /**
         * Utxo Pruning interval in minutes
         */
        private int pruningInterval = 1440; //In minutes
        /**
         * No of safe blocks to keep before pruning the UTXO
         */
        private int pruningSafeBlocks = 2160;
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
