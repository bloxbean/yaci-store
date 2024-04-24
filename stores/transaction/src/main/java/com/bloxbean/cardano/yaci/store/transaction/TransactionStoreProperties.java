package com.bloxbean.cardano.yaci.store.transaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionStoreProperties {

    @Builder.Default
    private boolean pruningEnabled = false;

    @Builder.Default
    private int pruningInterval = 86400;

    @Builder.Default
    private int pruningSafeSlot = 43200; // 2160 blocks
}
