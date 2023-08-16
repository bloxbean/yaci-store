package com.bloxbean.cardano.yaci.store.blocks.dto;

import com.bloxbean.cardano.yaci.store.blocks.domain.Vrf;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BlockDto {
    private long time;
    private long height;
    private long number;
    private String hash;
    private long slot;
    private int epoch;
    private int era;
   // private int epochSlot; //TODO
    private String slotLeader;
    private long size;
    private int txCount;
    private BigInteger output;
    private BigInteger fees;
    private String blockVrf;
  //  private String opCert;
  //  private int opCertCount; //TODO
    private String previousBlock;
  //  private String nextBlock; //TODO
  //  private long confirmations; //TODO

    private String issuerVkey;
    private Vrf nonceVrf;
    private Vrf leaderVrf;
    private Vrf vrfResult;
    private String blockBodyHash;
    private String protocolVersion;
}
