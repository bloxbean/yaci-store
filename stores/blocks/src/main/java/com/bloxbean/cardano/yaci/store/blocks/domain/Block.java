package com.bloxbean.cardano.yaci.store.blocks.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Block {
    private String hash;
    private Long number;
    private Long slot;
    private BigInteger totalOutput;
    private BigInteger totalFees;
    private Long blockTime;
    private Integer epochNumber;
    private Integer epochSlot;
    private Integer era;
    private String prevHash;
    private String issuerVkey;
    private String vrfVkey;
    private Vrf nonceVrf;
    private Vrf leaderVrf;
    private Vrf vrfResult;
    private String opCertHotVKey;
    private Integer opCertSeqNumber;
    private Integer opcertKesPeriod;
    private String opCertSigma;
    private long blockBodySize;
    private String blockBodyHash;
    private String protocolVersion;
    private int noOfTxs;
    private String slotLeader;
}
