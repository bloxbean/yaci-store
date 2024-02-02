package com.bloxbean.cardano.yaci.store.epochaggr.domain;

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
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Epoch {
    private long number;
    private int blockCount;
    private long transactionCount;
    private BigInteger totalOutput;
    private BigInteger totalFees;
    private long startTime;
    private long endTime;
    private long maxSlot;
}
