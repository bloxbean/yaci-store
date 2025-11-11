package com.bloxbean.cardano.yaci.store.starter.transaction;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "store", ignoreUnknownFields = true)
public class TransactionAutoConfigProperties {
    private Transaction transaction = new Transaction();

    @Getter
    @Setter
    public static final class Transaction {
        private boolean enabled = true;
        private boolean apiEnabled = true;
        private Endpoints endpoints = new Endpoints();
        /**
         * Enable pruning of Transaction
         */
        private boolean pruningEnabled = false;
        /**
         * Transaction Pruning interval in seconds
         */
        private int pruningInterval = 86400;
        /**
         * safe slot count to keep before pruning the Transaction
         */
        private int pruningSafeSlot = 43200; // 2160 blocks
        /**
         * Enable/disable saving of transaction witnesses.
         * When disabled, transaction witnesses will not be stored in the database.
         * This can help reduce storage requirements if witness data is not needed.
         */
        private boolean saveWitness = false;


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
       * Default: 43,200 slots (based on 2160 safe blocks).
       * CBOR data older than this will be pruned if cborPruningEnabled is true.
       */
      private int cborRetentionSlots = 43200; // 20 * 2160 slots
    }

    @Getter
    @Setter
    public static final class Endpoints {
        private Endpoint block = new Endpoint();
        private Endpoint transaction = new Endpoint();
    }

    @Getter
    @Setter
    public static final class Endpoint {
        private boolean enabled = true;
    }

}
