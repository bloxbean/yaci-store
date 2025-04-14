package com.bloxbean.cardano.yaci.store.api.adapot.dto;

import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.util.PoolUtil;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AddressPoolRewardDto(Integer epoch, BigInteger amount, String poolId, RewardType type) {

    public static AddressPoolRewardDto toDto(Reward reward) {
        String poolBech32Id = null;

        try {
            if (reward.getPoolId() != null) {
                poolBech32Id = PoolUtil.getBech32PoolId(reward.getPoolId());
            }
        } catch (Exception e) {}

        return new AddressPoolRewardDto(reward.getSpendableEpoch(),
                reward.getAmount(),
                poolBech32Id,
                reward.getType());
    }
}
