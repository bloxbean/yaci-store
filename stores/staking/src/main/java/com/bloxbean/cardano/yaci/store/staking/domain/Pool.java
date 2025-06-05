package com.bloxbean.cardano.yaci.store.staking.domain;

import com.bloxbean.cardano.yaci.store.common.domain.BlockAwareDomain;
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
@SuperBuilder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Pool extends BlockAwareDomain {
    private String poolId;
    private String txHash;
    private Integer certIndex;
    private Integer txIndex;
    private PoolStatusType status;
    private BigInteger amount;
    private Integer epoch;
    private Integer activeEpoch;
    private Integer retireEpoch;
    private Long registrationSlot;
    private Long slot;
    private String blockHash;
}
