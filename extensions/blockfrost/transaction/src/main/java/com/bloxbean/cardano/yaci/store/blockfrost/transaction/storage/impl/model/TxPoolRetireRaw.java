package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TxPoolRetireRaw {
    private Integer certIndex;
    /** Raw hex pool ID from DB (not yet bech32-encoded) */
    private String poolIdHex;
    private Integer retirementEpoch;
}
