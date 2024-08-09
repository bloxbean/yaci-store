package com.bloxbean.cardano.yaci.store.adapot.storage.impl.model;

import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class RewardId implements Serializable {
    @Column(name = "address")
    private String address;

    @Column(name = "earned_epoch")
    private Integer earnedEpoch;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private RewardType type;

    @Column(name = "pool_id")
    private String poolId;
}
