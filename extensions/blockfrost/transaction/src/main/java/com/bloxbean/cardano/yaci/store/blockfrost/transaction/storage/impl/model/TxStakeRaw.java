package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TxStakeRaw {
    private Integer certIndex;
    private String address;
    /** Raw type from DB: "STAKE_REGISTRATION" or "STAKE_DEREGISTRATION" */
    private String type;
}
