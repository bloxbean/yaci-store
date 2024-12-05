package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DRepDist {
    private String drepHash;

    private String drepId;

    private BigInteger amount;

    private Integer epoch;
}
