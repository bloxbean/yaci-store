package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class LocalGovActionProposalStatusId {
    @Column(name = "gov_action_tx_hash")
    private String govActionTxHash;

    @Column(name = "gov_action_index")
    private long govActionIndex;

    @Column(name = "epoch")
    private Integer epoch;
}
