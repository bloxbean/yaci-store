package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.governance.VoterType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class VotingProcedureId implements Serializable {
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "voter_hash")
    private String voterHash;

    @Column(name = "voter_type")
    @Enumerated(EnumType.STRING)
    private VoterType voterType;

    @Column(name = "gov_action_tx_hash")
    private String govActionTxHash;

    @Column(name = "gov_action_index")
    private Integer govActionIndex;
}
