package com.bloxbean.cardano.yaci.store.blocks.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Block {
    private String blockHash;
    private long block;
    private long slot;
   // private long epoch;
    private int era;
    private String prevHash;
    private String issuerVkey;
    private String vrfVkey;
    private Vrf nonceVrf;
    private Vrf leaderVrf;
    private Vrf vrfResult;
    private long blockBodySize;
    private String blockBodyHash;
    private String protocolVersion;
    private int noOfTxs;
}
