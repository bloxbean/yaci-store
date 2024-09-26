package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import lombok.*;

@EqualsAndHashCode
@Builder
@AllArgsConstructor
@Setter
@Getter
public class GovActionId {
    private String govActionTxHash;
    private Integer govActionIndex;
}
