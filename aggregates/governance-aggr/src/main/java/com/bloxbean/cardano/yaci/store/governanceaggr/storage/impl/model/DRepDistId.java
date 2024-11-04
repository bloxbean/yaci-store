package com.bloxbean.cardano.yaci.store.governanceaggr.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class DRepDistId implements Serializable {
    @Column(name = "drep_id")
    private String drepId;

    @Column(name = "epoch")
    private Integer epoch;
}
