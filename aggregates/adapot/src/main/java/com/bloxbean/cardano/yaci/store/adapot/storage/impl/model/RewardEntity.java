package com.bloxbean.cardano.yaci.store.adapot.storage.impl.model;

import com.bloxbean.cardano.yaci.store.events.domain.RewardType;
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
@Table(name = "reward")
@IdClass(RewardId.class)
public class RewardEntity {
    @Id
    @Column(name = "address")
    private String address;

    @Id
    @Column(name = "earned_epoch")
    private Integer earnedEpoch;

    @Id
    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private RewardType type;

    @Id
    @Column(name = "pool_id")
    private String poolId;

    @Column(name = "amount")
    private BigInteger amount;

    @Column(name = "spendable_epoch")
    private Integer spendableEpoch;

    @Column(name = "slot")
    private Long slot;

    @UpdateTimestamp
    @Column(name = "update_datetime")
    private LocalDateTime updateDateTime;
}
