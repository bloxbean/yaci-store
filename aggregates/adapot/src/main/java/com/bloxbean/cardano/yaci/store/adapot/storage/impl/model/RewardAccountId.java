package com.bloxbean.cardano.yaci.store.adapot.storage.impl.model;

import jakarta.persistence.Column;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class RewardAccountId implements Serializable {
    @Column(name = "address")
    private String address;

    @Column(name = "slot")
    private Long slot;
}
