package com.bloxbean.cardano.yaci.store.api.adapot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NetworkInfoDto(Supply supply, Stake stake) {

    public record Supply(BigInteger max, BigInteger circulating, BigInteger treasury, BigInteger reserves) {
    }

    public record Stake(BigInteger active) {
    }
}


