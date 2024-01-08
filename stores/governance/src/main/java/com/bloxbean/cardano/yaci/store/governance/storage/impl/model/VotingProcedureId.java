package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class VotingProcedureId implements Serializable {

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "index")
    private long index;
}
