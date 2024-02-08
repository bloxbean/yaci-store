package com.bloxbean.cardano.yaci.store.staking.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class PoolId implements Serializable {
    @Column(name = "pool_id")
    private String poolId;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "cert_index")
    private Integer certIndex;

    @Column(name = "slot")
    private Long slot;
}
