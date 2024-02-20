package com.bloxbean.cardano.yaci.store.adapot.storage.impl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "epoch_stake")
@IdClass(EpochStakeId.class)
public class EpochStakeEntity {
    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Id
    @Column(name = "address")
    private String address;

    @Column(name = "amount")
    private BigInteger amount;

    @Column(name = "pool_id")
    private String poolId;

    @Column(name = "delegation_epoch")
    private Integer delegationEpoch;

    @Column(name = "active_epoch")
    private Integer activeEpoch;

    @Column(name = "create_datetime")
    private LocalDateTime createDateTime;
}
