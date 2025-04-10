package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import com.bloxbean.cardano.client.transaction.spec.governance.DRepType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class DRepDistId implements Serializable {
    @Column(name = "drep_hash")
    private String drepHash;

    @Column(name = "drep_type")
    @Enumerated(EnumType.STRING)
    private DRepType drepType;

    @Column(name = "epoch")
    private Integer epoch;
}
