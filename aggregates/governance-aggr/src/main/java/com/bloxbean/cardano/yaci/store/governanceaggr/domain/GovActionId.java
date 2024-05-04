package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Builder
@AllArgsConstructor
public class GovActionId {
    private String govActionTxHash;
    private Integer govActionIndex;
}
