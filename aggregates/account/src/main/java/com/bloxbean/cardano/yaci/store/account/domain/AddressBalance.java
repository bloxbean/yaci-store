package com.bloxbean.cardano.yaci.store.account.domain;

import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
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
public class AddressBalance extends BlockAwareDomain {
    private String address;
    private String unit;
    private Long slot;
    private BigInteger quantity;
    private String policy;
    private String assetName;
    private String paymentCredential;
    private String stakeAddress;
    private String blockHash;
    private Integer epoch;
}
