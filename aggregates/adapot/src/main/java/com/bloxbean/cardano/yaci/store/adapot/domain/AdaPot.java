package com.bloxbean.cardano.yaci.store.adapot.domain;

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
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdaPot extends BlockAwareDomain {
    private Integer epoch;
    private Long slot;
    private BigInteger depositsStake;
    private BigInteger fees;
    private BigInteger utxo;
    private BigInteger treasury;
    private BigInteger reserves;
    private BigInteger rewards;
    private BigInteger depositsDrep;
    private BigInteger depositsProposal;
}
