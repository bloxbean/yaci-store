package com.bloxbean.cardano.yaci.store.utxo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UtxoStoreProperties {

    private boolean saveAddress;

    @Builder.Default
    private boolean addressCacheEnabled = false;

    @Builder.Default
    private int addressCacheSize = 50000;

    @Builder.Default
    private int addressCacheExpiryAfterAccess = 15;

    @Builder.Default
    private boolean pruningEnabled = false;

    @Builder.Default
    private int pruningInterval = 86400;

    @Builder.Default
    private int pruningSafeBlocks = 2160;

    @Builder.Default
    private boolean contentAwareRollback = false;

    /**
     * Maintain address_utxo_unspent (the outputs with no tx_input row): outputs are written with address_utxo,
     * and spent outputs are deleted at each commit (see UtxoStorageImpl).
     */
    @Builder.Default
    private boolean unspentTableEnabled = false;

    /**
     * With {@link #unspentTableEnabled}: write nothing to address_utxo_unspent during the initial sync, and
     * build it once when the store reaches the tip (see UnspentUtxoTable). On by default, so the initial sync
     * does no extra writes; the build at the tip took 2 hours on mainnet. Off, the table is written from the
     * first block.
     */
    @Builder.Default
    private boolean unspentTableBackfillAtTip = true;

    /**
     * Serve the unspent-output queries from address_utxo_unspent (see UnspentTableUtxoStorageReader), once the
     * table is active.
     */
    @Builder.Default
    private boolean unspentTableReadEnabled = false;
}
