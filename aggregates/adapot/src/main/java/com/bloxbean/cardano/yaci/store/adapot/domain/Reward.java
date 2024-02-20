package com.bloxbean.cardano.yaci.store.adapot.domain;

import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Reward extends BlockAwareDomain {
    private UUID id;
    private String address;
    private BigInteger amount;
    private RewardType type;
    private String txHash;
    private Long slot;
    private Integer earnedEpoch;
    private Integer spendableEpoch;
}
