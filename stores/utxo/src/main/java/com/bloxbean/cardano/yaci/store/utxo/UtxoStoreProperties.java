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
}
