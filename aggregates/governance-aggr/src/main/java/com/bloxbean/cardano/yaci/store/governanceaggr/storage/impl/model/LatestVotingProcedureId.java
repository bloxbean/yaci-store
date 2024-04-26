package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import jakarta.persistence.Column;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
@Builder
public class LatestVotingProcedureId implements Serializable {

    @Column(name = "voter_hash")
    private String voterHash;

    @Column(name = "gov_action_tx_hash")
    private String govActionTxHash;

    @Column(name = "gov_action_index")
    private Integer govActionIndex;
}
