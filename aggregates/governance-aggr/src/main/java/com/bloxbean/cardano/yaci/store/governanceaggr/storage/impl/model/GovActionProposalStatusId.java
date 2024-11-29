package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class GovActionProposalStatusId {
    @Column(name = "gov_action_tx_hash")
    private String govActionTxHash;

    @Column(name = "gov_action_index")
    private int govActionIndex;

    @Column(name = "epoch")
    private Integer epoch;
}
