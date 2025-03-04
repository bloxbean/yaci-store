package com.bloxbean.cardano.yaci.store.adapot.storage.impl.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "adapot")
@IdClass(AdaPotId.class)
public class AdaPotEntity {
    @Id
    @Column(name = "epoch")
    private Integer epoch;

    @Id
    @Column(name = "slot")
    private Long slot;

    @Column(name = "deposits_stake")
    private BigInteger depositsStake;

    @Column(name = "fees")
    private BigInteger fees;

    @Column(name = "utxo")
    private BigInteger utxo;

    @Column(name = "treasury")
    private BigInteger treasury;

    @Column(name = "reserves")
    private BigInteger reserves;

    @Column(name = "circulation")
    private BigInteger circulation;

    @Column(name = "distributed_rewards")
    private BigInteger distributedRewards;

    @Column(name = "undistributed_rewards")
    private BigInteger undistributedRewards;

    @Column(name = "rewards_pot")
    private BigInteger rewardsPot;

    @Column(name = "pool_rewards_pot")
    private BigInteger poolRewardsPot;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDateTime;
}
