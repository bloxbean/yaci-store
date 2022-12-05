package com.bloxbean.carano.yaci.indexer.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

//Not used
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TxResolvedInput implements Serializable {
    private String txHash;
    private Integer outputIndex;
    private String address;
    //private String ownerStakeAddr;

    private List<Amt> amounts;
    private String dataHash;
    private String inlineDatum;
    private String referenceScriptHash;
}
