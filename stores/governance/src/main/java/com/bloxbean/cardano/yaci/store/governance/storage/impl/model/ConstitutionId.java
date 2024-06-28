package com.bloxbean.cardano.yaci.store.governance.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class ConstitutionId implements Serializable  {
    @Column(name = "anchor_hash")
    private String anchorHash;

    @Column(name = "slot")
    private Long slot;
}
