package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class LocalDRepDistrId {
    @Column(name = "drep_hash")
    private String drepHash;

    @Column(name = "epoch")
    private Integer epoch;
}
