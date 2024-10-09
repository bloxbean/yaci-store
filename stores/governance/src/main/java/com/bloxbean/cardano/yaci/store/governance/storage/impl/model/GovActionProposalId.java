package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@EqualsAndHashCode
@Getter
@Setter
public class GovActionProposalId implements Serializable {
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "idx")
    private long index;
}
