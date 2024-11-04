package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DRepDist {
    private String drepHash;

    private String drepId;

    private Long amount;

    private Integer epoch;

    private Long slot;
}
