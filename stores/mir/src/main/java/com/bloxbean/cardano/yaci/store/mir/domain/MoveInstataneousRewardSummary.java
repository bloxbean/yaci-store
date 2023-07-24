package com.bloxbean.cardano.yaci.store.mir.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MoveInstataneousRewardSummary {
    private String txHash;
    private Long slot;
    private Long blockNumber;
    private Long blockTime;
    private MirPot pot;
    private int certIndex;
    private int totalStakeKeys;
    private BigInteger totalRewards;
}
