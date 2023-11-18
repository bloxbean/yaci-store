package com.bloxbean.cardano.yaci.store.assets.domain;

import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TxAsset extends BlockAwareDomain {
    private Long slot;
    private String txHash;
    private String policy;
    private String assetName;
    private String unit;
    private String fingerprint;
    private BigInteger quantity;
    private MintType mintType;
}
