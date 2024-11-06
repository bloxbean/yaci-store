package com.bloxbean.cardano.yaci.store.adapot.domain;

import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
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
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Reward {
    private String address;
    private Integer earnedEpoch;
    private RewardType type;
    private String poolId;
    private BigInteger amount;
    private Integer spendableEpoch;
    private Long slot;
}
