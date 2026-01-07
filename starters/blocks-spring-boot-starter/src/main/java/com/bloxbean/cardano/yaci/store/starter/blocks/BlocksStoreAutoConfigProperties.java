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

       /**
        * Enable/disable saving of block CBOR data.
        * When enabled, raw CBOR bytes of blocks will be stored in a separate table.
        * This is useful for block verification and debugging.
        * Note: This significantly increases storage requirements.
        */
       private boolean saveCbor = false;

       /**
        * Enable/disable pruning of block CBOR data.
        * When enabled, CBOR data older than cborPruningSafeSlots will be automatically deleted.
        */
       private boolean cborPruningEnabled = false;

       /**
        * Safe slot count to keep before pruning the block CBOR data.
        * Default: 43,200 slots (based on 2160 safe blocks).
        * CBOR data older than this will be pruned if cborPruningEnabled is true.
        */
       private int cborPruningSafeSlots = 43200; // 20 * 2160 slots
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
