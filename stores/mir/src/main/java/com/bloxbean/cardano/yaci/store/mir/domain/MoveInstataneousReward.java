package com.bloxbean.cardano.yaci.store.mir.domain;

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
public class MoveInstataneousReward extends BlockAwareDomain {
    private MirPot pot;
    private String txHash;
    private long certIndex;

    private String credential;
    private String address;
    private BigInteger amount;

    private Integer epoch;
    private Long slot;
    private String blockHash;
}
