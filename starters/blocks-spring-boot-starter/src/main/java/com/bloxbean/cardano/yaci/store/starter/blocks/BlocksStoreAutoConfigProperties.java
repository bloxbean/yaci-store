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
        * When enabled, CBOR data older than cborRetentionSlots will be automatically deleted.
        */
       private boolean cborPruningEnabled = false;

       /**
        * Retention period for CBOR data in slots.
        * Default: 2,592,000 slots = 30 days (1 slot = 1 second on Cardano mainnet).
        * CBOR data older than this will be pruned if cborPruningEnabled is true.
        */
       private int cborRetentionSlots = 2592000; // 30 days (30 * 24 * 60 * 60 slots)
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
