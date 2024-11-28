package com.bloxbean.cardano.yaci.store.governancerules.domain;

import com.bloxbean.cardano.yaci.store.common.domain.ProtocolParams;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpochParam {
    private Integer epoch;
    private ProtocolParams params;
    private Long slot;
}
