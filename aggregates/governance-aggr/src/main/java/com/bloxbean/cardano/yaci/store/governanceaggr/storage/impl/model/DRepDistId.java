package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import com.bloxbean.cardano.yaci.core.model.governance.DrepType;
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
    private DrepType drepType;

    @Column(name = "epoch")
    private Integer epoch;
}
