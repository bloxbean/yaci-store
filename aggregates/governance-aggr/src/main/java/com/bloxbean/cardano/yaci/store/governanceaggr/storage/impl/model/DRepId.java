package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class DRepId implements Serializable {
    @Column(name = "drep_id")
    private String drepId;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "cert_index")
    private Integer certIndex;

    @Column(name = "slot")
    private Long slot;
}
