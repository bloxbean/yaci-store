package com.bloxbean.cardano.yaci.store.api.adapot.dto;

import com.bloxbean.cardano.yaci.store.adapot.domain.InstantReward;
import com.bloxbean.cardano.yaci.store.adapot.domain.Reward;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardInfo;
import com.bloxbean.cardano.yaci.store.adapot.domain.RewardRest;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.bloxbean.cardano.yaci.store.events.domain.InstantRewardType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RewardInfoDto(
    String address,
    Integer earnedEpoch,
    Integer spendableEpoch,
    BigInteger amount,
    String poolId,
    RewardInfoType rewardType
) {

    public static RewardInfoDto toDto(RewardInfo rewardInfo) {
        return new RewardInfoDto(
            rewardInfo.getAddress(),
            rewardInfo.getEarnedEpoch(),
            rewardInfo.getSpendableEpoch(),
            rewardInfo.getAmount(),
            rewardInfo.getPoolId(),
            rewardInfo.getRewardType()
        );
    }

    public static RewardInfoDto fromReward(Reward reward) {
        RewardInfoType type = null;
        if (reward.getType() == RewardType.member) type = RewardInfoType.pool_member;
        else if (reward.getType() == RewardType.leader) type = RewardInfoType.pool_leader;

        return new RewardInfoDto(
            reward.getAddress(),
            reward.getEarnedEpoch(),
            reward.getSpendableEpoch(),
            reward.getAmount(),
            reward.getPoolId(),
            type
        );
    }
    public static RewardInfoDto fromRewardRest(RewardRest rewardRest) {
        RewardInfoType type = null;
        if (rewardRest.getType() == RewardRestType.treasury) type = RewardInfoType.treasury;
        else if (rewardRest.getType() == RewardRestType.proposal_refund) type = RewardInfoType.pool_deposit_refund;

        return new RewardInfoDto(
            rewardRest.getAddress(),
            rewardRest.getEarnedEpoch(),
            rewardRest.getSpendableEpoch(),
            rewardRest.getAmount(),
            null,
            type
        );
    }

    public static RewardInfoDto fromInstantReward(InstantReward instantReward) {
        RewardInfoType type = null;
        if (instantReward.getType() == InstantRewardType.treasury) type = RewardInfoType.treasury;
        else if (instantReward.getType() == InstantRewardType.reserves) type = RewardInfoType.reserves;

        return new RewardInfoDto(
            instantReward.getAddress(),
            instantReward.getEarnedEpoch(),
            instantReward.getSpendableEpoch(),
            instantReward.getAmount(),
            null,
            type
        );
    }
} 