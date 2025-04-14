package com.bloxbean.cardano.yaci.store.api.adapot.dto;

import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AddressRewardRestDto(Integer epoch, Integer earnedEpoch, BigInteger amount, RewardRestType type) {

    public static AddressRewardRestDto toDto(RewardRest rewardRest) {
        return new AddressRewardRestDto(rewardRest.getSpendableEpoch(),
                rewardRest.getEarnedEpoch(),
                rewardRest.getAmount(),
                rewardRest.getType());
    }
}
