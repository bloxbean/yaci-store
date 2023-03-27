package com.bloxbean.cardano.yaci.store.script.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TxScript {
    private String txHash;
    private String scriptHash;
    private Long slot;
    private Long block;
    private String blockHash;
    private ScriptType type;
    private String redeemer;
    private String datum;
    private String datumHash;
}
