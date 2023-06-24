package com.bloxbean.cardano.yaci.store.blocks.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class BlockSummary {
    private long number;
    private long slot;
    private int epochNumber;
    private BigInteger totalOutput;
    private String era;
    private String issuerVkey;
    private String slotLeader;
    private long blockBodySize;
    private int noOfTxs;
    private long blockTime;
}
