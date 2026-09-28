package com.bloxbean.cardano.yaci.store.blockfrost.governance.storage.impl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated model for a DRep delegator: stake address + total unspent lovelace.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BFDRepDelegator {
    private String address;
    private Long amount;
}
