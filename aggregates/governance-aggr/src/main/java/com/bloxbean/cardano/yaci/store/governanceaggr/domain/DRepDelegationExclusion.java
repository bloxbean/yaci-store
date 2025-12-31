package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DRepDelegationExclusion {
    private String address;
    private String drepHash;
    private DrepType drepType;
    private Long slot;
    private Integer txIndex;
    private Integer certIndex;
}

