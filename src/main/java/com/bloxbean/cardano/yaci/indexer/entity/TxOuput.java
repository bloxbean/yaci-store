package com.bloxbean.cardano.yaci.indexer.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//Not used
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TxOuput {
    private String address;

    private List<Amt> amounts;
    private String dataHash;
    private String inlineDatum;
    private String referenceScriptHash;
}
