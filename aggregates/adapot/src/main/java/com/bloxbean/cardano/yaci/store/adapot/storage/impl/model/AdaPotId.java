package com.bloxbean.cardano.yaci.store.adapot.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class AdaPotId implements Serializable {
    @Column(name = "epoch")
    private Integer epoch;

    @Column(name = "slot")
    private Long slot;

    @Column(name = "epoch_boundary")
    private Boolean epochBoundary;
}
