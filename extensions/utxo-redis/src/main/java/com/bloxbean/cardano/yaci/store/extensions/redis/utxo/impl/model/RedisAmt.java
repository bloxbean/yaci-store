package com.bloxbean.cardano.yaci.store.extensions.redis.utxo.impl.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.redis.om.spring.annotations.Indexed;
import lombok.*;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RedisAmt {

    @Indexed
    private String unit;

    @Indexed
    private String policyId;

    @Indexed
    private String assetName;
    private BigInteger quantity;
}