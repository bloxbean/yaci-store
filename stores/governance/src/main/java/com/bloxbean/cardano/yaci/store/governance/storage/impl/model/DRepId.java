package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class DRepId implements Serializable {
    @Column(name = "drep_hash")
    private String drepHash;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "cert_index")
    private Integer certIndex;

    @Column(name = "slot")
    private Long slot;
}
