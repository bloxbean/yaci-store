package com.bloxbean.cardano.yaci.store.adapot.storage.impl.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class EpochStakeId implements Serializable {
    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Id
    @Column(name = "address")
    private String address;
}
