package com.bloxbean.cardano.yaci.store.staking.storage.impl.model;

import jakarta.persistence.Column;

import java.io.Serializable;

public class PoolRegistrationId implements Serializable {
    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "cert_index")
    private int certIndex;
}
