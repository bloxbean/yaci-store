package com.bloxbean.cardano.yaci.store.utxo.storage.impl.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TxInputEntity {

    private String txHash;

    private Integer outputIndex;

    private Long spentAtSlot;

    private Long spentAtBlock;

    private String spentAtBlockHash;

    private Long spentBlockTime;

    private Integer spentEpoch;

    private String spentTxHash;
}
