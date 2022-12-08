package com.bloxbean.carano.yaci.indexer.common.domain;

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
public class TxOuput implements Serializable {
    private String address;

    private List<Amt> amounts;
    private String dataHash;
    private String inlineDatum;
    private String referenceScriptHash;
}
