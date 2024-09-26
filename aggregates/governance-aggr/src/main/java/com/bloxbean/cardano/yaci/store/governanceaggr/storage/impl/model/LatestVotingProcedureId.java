package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestVotingProcedureId implements Serializable {

    @Column(name = "voter_hash", insertable = false, updatable = false)
    private String voterHash;

    @Column(name = "gov_action_tx_hash", insertable = false, updatable = false)
    private String govActionTxHash;

    @Column(name = "gov_action_index", insertable = false, updatable = false)
    private Integer govActionIndex;
}
