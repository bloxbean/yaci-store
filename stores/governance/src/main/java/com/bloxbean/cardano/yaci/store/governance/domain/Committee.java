package com.bloxbean.cardano.yaci.store.governance.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Committee {
    private Integer epoch;
    private String govActionTxHash;
    private Integer govActionIndex;
    private BigInteger thresholdNumerator;
    private BigInteger thresholdDenominator;
    private BigDecimal threshold;
    private Long slot;
}
