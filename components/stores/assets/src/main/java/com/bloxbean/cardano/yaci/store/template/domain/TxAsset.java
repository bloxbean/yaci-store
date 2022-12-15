package com.bloxbean.cardano.yaci.store.template.domain;

import com.bloxbean.cardano.yaci.store.template.domain.MintType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigInteger;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TxAsset {
    private String txHash;
    private String policy;
    private String assetName;
    private String unit;
    private BigInteger quantity;
    private String mintType;
}
