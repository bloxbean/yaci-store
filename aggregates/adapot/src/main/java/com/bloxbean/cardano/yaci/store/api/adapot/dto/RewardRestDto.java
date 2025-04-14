package com.bloxbean.cardano.yaci.store.api.adapot.dto;

import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigInteger;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RewardRestDto(
         String address,
         RewardRestType type,
         BigInteger amount,
         Integer earnedEpoch,
         Integer spendableEpoch
) {
}
