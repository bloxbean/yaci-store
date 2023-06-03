package com.bloxbean.cardano.yaci.store.staking.storage.impl.jpa.model;

import jakarta.persistence.Column;

import java.io.Serializable;

public class PoolRetirementId implements Serializable {
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "cert_index")
    private int certIndex;
}
