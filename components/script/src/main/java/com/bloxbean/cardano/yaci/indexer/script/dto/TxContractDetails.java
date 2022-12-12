package com.bloxbean.cardano.yaci.indexer.script.dto;

import com.bloxbean.cardano.yaci.indexer.script.model.ScriptType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TxContractDetails {
    private String txHash;
    private String scriptHash;
    private String scriptContent;
    private ScriptType type;
    private RedeemerDto redeemer;
    private String datum;
    private String datumHash;
}
