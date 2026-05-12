package com.bloxbean.cardano.yaci.store.api.adapot.dto;

import com.bloxbean.cardano.yaci.store.adapot.domain.WithdrawableReward;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WithdrawableRewardDto(
        String address,
        BigInteger withdrawableAmount
) {
    public static WithdrawableRewardDto toDto(WithdrawableReward withdrawableReward) {
        return new WithdrawableRewardDto(
                withdrawableReward.getAddress(),
                withdrawableReward.getWithdrawableAmount()
        );
    }
}
