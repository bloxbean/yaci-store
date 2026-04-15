package com.bloxbean.cardano.yaci.store.blockfrost.transaction.storage.impl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TxOutputRaw {
    private String txHash;
    private Integer outputIndex;
    private String address;
    private Long lovelaceAmount;
    private String amountsJson;
    private String dataHash;
    private String inlineDatum;
    private String referenceScriptHash;
    private Boolean isCollateralReturn;
    private String consumedByTx;
}
