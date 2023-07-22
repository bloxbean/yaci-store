package com.bloxbean.cardano.yaci.store.script.domain;

import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TxScript extends BlockAwareDomain {
    private String txHash;
    private String scriptHash;
    private Long slot;
    private String blockHash;
    private ScriptType type;
    private String redeemer;
    private String datum;
    private String datumHash;
}
