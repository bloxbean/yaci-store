package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import jakarta.persistence.Column;
import lombok.*;

import java.io.Serializable;

@EqualsAndHashCode
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CommitteeVoteId implements Serializable {
    @Column(name = "gov_action_tx_hash")
    private String govActionTxHash;
    @Column(name = "gov_action_index")
    private int govActionIndex;
    @Column(name = "voter_hash")
    private String voterHash;
    @Column(name = "slot")
    private long slot;
}
