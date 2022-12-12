package com.bloxbean.cardano.yaci.store.blocks.dto;

import com.bloxbean.cardano.yaci.store.blocks.model.Vrf;
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
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BlockDetails {
    private String blockHash;
    private long block;
    private long slot;
   // private long epoch;
    private String era;
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
