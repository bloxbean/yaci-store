package com.bloxbean.cardano.yaci.store.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigInteger;
import java.util.List;

/**
 * This class is used to represent UTXO for controller API
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Utxo {

    private String txHash;
    private int outputIndex;
    private String address;
    private List<Amount> amount;
    private String dataHash;
    private String inlineDatum;
    private String referenceScriptHash;
    private Integer epoch;
    private Long blockNumber;
    private Long blockTime;

    @Getter
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Amount {
        private String unit;
        private BigInteger quantity;
    }
}
