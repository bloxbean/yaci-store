package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class GovActionProposalInfoId implements Serializable {
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "idx")
    private long index;
}
