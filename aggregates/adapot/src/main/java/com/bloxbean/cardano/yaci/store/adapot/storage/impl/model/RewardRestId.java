package com.bloxbean.cardano.yaci.store.adapot.storage.impl.model;

import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class RewardRestId implements Serializable {
    @Id
    @Column(name = "address")
    private String address;

    @Id
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private RewardRestType type;

    @Id
    @Column(name = "earned_epoch")
    private Integer earnedEpoch;
}
