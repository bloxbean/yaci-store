package com.bloxbean.cardano.yaci.store.api.adapot.dto;

import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PoolAddressRewardDto(String address, BigInteger reward, RewardType type, Integer earnedEpoch) {
}
