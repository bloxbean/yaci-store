package com.bloxbean.cardano.yaci.store.adapot.storage.impl.model;

import com.bloxbean.cardano.yaci.store.events.domain.RewardRestType;
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
@Table(name = "reward_rest")
@IdClass(RewardRestId.class)
public class RewardRestEntity {
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

    @Column(name = "amount")
    private BigInteger amount;

    @Column(name = "spendable_epoch")
    private Integer spendableEpoch;

    @Column(name = "slot")
    private Long slot;

    @UpdateTimestamp
    @Column(name = "create_datetime")
    private LocalDateTime createDatetime;
}
