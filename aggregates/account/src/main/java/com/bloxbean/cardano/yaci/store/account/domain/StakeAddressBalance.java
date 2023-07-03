package com.bloxbean.cardano.yaci.store.account.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class StakeAddressBalance {
    private String address;
    private String unit;
    private Long slot;
    private BigInteger quantity;
    private String policy;
    private String assetName;
    private String stakeCredential;
    private String blockHash;
    private Long block;
    private Integer epoch;
}

