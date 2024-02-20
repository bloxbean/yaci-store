package com.bloxbean.cardano.yaci.store.adapot.storage.impl.model;

import com.bloxbean.cardano.yaci.store.common.model.BlockAwareEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "reward_account")
@IdClass(RewardAccountId.class)
public class RewardAccountEntity extends BlockAwareEntity {
    @Id
    @Column(name = "address")
    private String address;

    @Id
    @Column(name = "slot")
    private Long slot;

    @Column(name = "amount")
    private BigInteger amount;

    @Column(name = "withdrawable")
    private BigInteger withdrawable;

    @Column(name = "epoch")
    private Integer epoch;
}
