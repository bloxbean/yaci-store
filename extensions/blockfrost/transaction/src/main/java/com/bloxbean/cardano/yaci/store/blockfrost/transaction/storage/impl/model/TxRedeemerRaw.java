package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TxRedeemerRaw {
    private Integer txIndex;
    /** Raw purpose string from DB: "Spend", "Mint", "Cert", "Reward", "Voting", "Proposing" */
    private String purpose;
    private String scriptHash;
    private String redeemerDatahash;
    private Long unitMem;
    private Long unitSteps;
}
