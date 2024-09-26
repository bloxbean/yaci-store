package com.bloxbean.cardano.yaci.store.governanceaggr.domain;

import lombok.*;

@EqualsAndHashCode
@Builder
@Getter
@Setter
@AllArgsConstructor
public class CommitteeVoteId {
    private String govActionTxHash;
    private int govActionIndex;
    private long slot;
}
