package com.bloxbean.cardano.yaci.store.api.adapot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigInteger;

/**
 * Data Transfer Object for AdaPot
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AdaPotDto(Integer epoch,
                        BigInteger depositsStake,
                        BigInteger fees,
                        BigInteger treasury,
                        BigInteger reserves,
                        BigInteger circulation,
                        BigInteger distributedRewards,
                        BigInteger undistributedRewards,
                        BigInteger rewardsPot,
                        BigInteger poolRewardsPot) {
}
