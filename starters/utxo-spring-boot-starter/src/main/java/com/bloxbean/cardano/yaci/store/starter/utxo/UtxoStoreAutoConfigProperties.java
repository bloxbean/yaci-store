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
         * Utxo Pruning interval in seconds
         */
        private int pruningInterval = 86400;
        /**
         * No of safe blocks to keep before pruning the UTXO
         */
        private int pruningSafeBlocks = 2160;

        /**
         * Enable content aware rollback
         */
        private boolean contentAwareRollback = false;

        /**
         * Maintain address_utxo_unspent, a copy of the outputs that are not spent yet. Outputs are
         * written together with address_utxo, and spent outputs are deleted at each commit. Readers that need current outputs then read it instead of anti-joining
         * address_utxo with tx_input over the whole history. On an already synced store, or one where
         * the table fell behind, the table is built when the sync reaches the tip, or beforehand with
         * admin-cli backfill-unspent-utxos.
         */
        private boolean unspentTableEnabled = false;

        /**
         * With unspent-table-enabled: write nothing to address_utxo_unspent during the initial sync and
         * build it once when the sync reaches the tip, then keep it current. Without it the table is
         * written from the first block of a new store.
         */
        private boolean unspentTableBackfillAtTip = false;

        /**
         * Serve the unspent-output queries (by address, payment credential, stake address, asset)
         * from address_utxo_unspent while it is current. Until then they use address_utxo and tx_input.
         */
        private boolean unspentTableReadEnabled = false;
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
